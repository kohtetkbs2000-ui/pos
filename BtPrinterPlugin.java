package com.shoppos.app;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BtPrinter (classic Bluetooth / SPP thermal printers, e.g. 58mm / 80mm ESC/POS)
 *   list()                       -> { devices: [{ address, name }] }  (paired devices)
 *   connect({ address })
 *   write({ address, data })     data = base64 bytes
 *   disconnect({ address })
 * Pair the printer first in Android Settings > Bluetooth.
 */
@CapacitorPlugin(
        name = "BtPrinter",
        permissions = {
                @Permission(alias = "bt", strings = { "android.permission.BLUETOOTH_CONNECT" })
        }
)
public class BtPrinterPlugin extends Plugin {

    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Map<String, BluetoothSocket> sockets = new ConcurrentHashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private boolean needPermission() {
        return Build.VERSION.SDK_INT >= 31 && getPermissionState("bt") != PermissionState.GRANTED;
    }

    @PermissionCallback
    private void permCallback(PluginCall call) {
        if (getPermissionState("bt") == PermissionState.GRANTED) {
            if ("connect".equals(call.getMethodName())) {
                doConnect(call);
            } else {
                doList(call);
            }
        } else {
            call.reject("Bluetooth permission denied");
        }
    }

    // ---- list ----
    @PluginMethod
    public void list(PluginCall call) {
        if (needPermission()) {
            requestPermissionForAlias("bt", call, "permCallback");
            return;
        }
        doList(call);
    }

    @SuppressLint("MissingPermission")
    private void doList(PluginCall call) {
        BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
        if (ad == null) {
            call.reject("This device has no Bluetooth");
            return;
        }
        if (!ad.isEnabled()) {
            call.reject("Please turn on Bluetooth first");
            return;
        }
        try {
            JSArray arr = new JSArray();
            Set<BluetoothDevice> bonded = ad.getBondedDevices();
            for (BluetoothDevice d : bonded) {
                JSObject o = new JSObject();
                o.put("address", d.getAddress());
                String n = d.getName();
                o.put("name", n == null ? d.getAddress() : n);
                arr.put(o);
            }
            JSObject res = new JSObject();
            res.put("devices", arr);
            call.resolve(res);
        } catch (SecurityException e) {
            call.reject("Bluetooth permission denied");
        }
    }

    // ---- connect ----
    @PluginMethod
    public void connect(PluginCall call) {
        if (needPermission()) {
            requestPermissionForAlias("bt", call, "permCallback");
            return;
        }
        doConnect(call);
    }

    @SuppressLint("MissingPermission")
    private void doConnect(final PluginCall call) {
        final String addr = call.getString("address");
        if (addr == null || addr.isEmpty()) {
            call.reject("address is required");
            return;
        }
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
                    if (ad == null || !ad.isEnabled()) {
                        call.reject("Please turn on Bluetooth first");
                        return;
                    }
                    closeQuiet(addr);
                    BluetoothDevice dev = ad.getRemoteDevice(addr);
                    try { ad.cancelDiscovery(); } catch (SecurityException ignore) { }
                    BluetoothSocket s = null;
                    try {
                        s = dev.createRfcommSocketToServiceRecord(SPP);
                        s.connect();
                    } catch (IOException first) {
                        if (s != null) {
                            try { s.close(); } catch (IOException ignore) { }
                        }
                        // fallback used by many cheap printers
                        s = (BluetoothSocket) dev.getClass()
                                .getMethod("createRfcommSocket", int.class)
                                .invoke(dev, 1);
                        s.connect();
                    }
                    sockets.put(addr, s);
                    call.resolve();
                } catch (Exception e) {
                    call.reject("Bluetooth connect failed: " + e.getMessage());
                }
            }
        });
    }

    // ---- write ----
    @PluginMethod
    public void write(final PluginCall call) {
        final String addr = call.getString("address");
        final String data = call.getString("data");
        if (addr == null || data == null) {
            call.reject("address and data are required");
            return;
        }
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    BluetoothSocket s = sockets.get(addr);
                    if (s == null || !s.isConnected()) {
                        call.reject("Printer is not connected");
                        return;
                    }
                    byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                    OutputStream os = s.getOutputStream();
                    os.write(bytes);
                    os.flush();
                    call.resolve();
                } catch (Exception e) {
                    closeQuiet(addr);
                    call.reject("Write failed: " + e.getMessage());
                }
            }
        });
    }

    // ---- disconnect ----
    @PluginMethod
    public void disconnect(PluginCall call) {
        String addr = call.getString("address");
        if (addr != null) {
            closeQuiet(addr);
        }
        call.resolve();
    }

    private void closeQuiet(String addr) {
        BluetoothSocket s = sockets.remove(addr);
        if (s != null) {
            try { s.close(); } catch (IOException ignore) { }
        }
    }

    @Override
    protected void handleOnDestroy() {
        for (String a : sockets.keySet()) {
            closeQuiet(a);
        }
        io.shutdown();
    }
}
