package com.shoppos.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
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

import java.io.OutputStream;
import java.util.UUID;

/** Classic Bluetooth (SPP / RFCOMM) ESC-POS thermal printers that are already paired in Android settings. */
@CapacitorPlugin(
    name = "BtPrinter",
    permissions = { @Permission(strings = { Manifest.permission.BLUETOOTH_CONNECT }, alias = "bt") }
)
public class BtPrinterPlugin extends Plugin {
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;
    private OutputStream out;

    private boolean granted() {
        return Build.VERSION.SDK_INT < 31 || getPermissionState("bt") == PermissionState.GRANTED;
    }

    private BluetoothAdapter adapter() {
        BluetoothManager m = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        return m == null ? null : m.getAdapter();
    }

    @PluginMethod
    public void list(PluginCall call) {
        if (!granted()) { requestPermissionForAlias("bt", call, "permDone"); return; }
        doList(call);
    }

    @PluginMethod
    public void connect(PluginCall call) {
        if (!granted()) { requestPermissionForAlias("bt", call, "permDone"); return; }
        doConnect(call);
    }

    @PermissionCallback
    private void permDone(PluginCall call) {
        if (!granted()) { call.reject("Bluetooth ခွင့်ပြုချက် မရပါ"); return; }
        if ("connect".equals(call.getMethodName())) doConnect(call); else doList(call);
    }

    @SuppressLint("MissingPermission")
    private void doList(PluginCall call) {
        BluetoothAdapter ad = adapter();
        if (ad == null) { call.reject("ဤဖုန်းတွင် Bluetooth မရှိပါ"); return; }
        if (!ad.isEnabled()) { call.reject("Bluetooth ကို အရင်ဖွင့်ပါ"); return; }
        JSArray arr = new JSArray();
        for (BluetoothDevice d : ad.getBondedDevices()) {
            JSObject o = new JSObject();
            o.put("name", d.getName() == null ? d.getAddress() : d.getName());
            o.put("address", d.getAddress());
            arr.put(o);
        }
        JSObject res = new JSObject();
        res.put("devices", arr);
        call.resolve(res);
    }

    @SuppressLint("MissingPermission")
    private void doConnect(final PluginCall call) {
        final String address = call.getString("address", "");
        final BluetoothAdapter ad = adapter();
        if (ad == null || !ad.isEnabled()) { call.reject("Bluetooth ကို အရင်ဖွင့်ပါ"); return; }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    closeQuiet();
                    BluetoothDevice dev = ad.getRemoteDevice(address);
                    // cancelDiscovery() needs BLUETOOTH_SCAN on Android 12+ - never let it block connecting
                    try { ad.cancelDiscovery(); } catch (Exception ignored) { }
                    BluetoothSocket s = null;
                    Exception last = null;
                    for (int mode = 0; mode < 3 && s == null; mode++) {
                        BluetoothSocket t = null;
                        try {
                            if (mode == 0) t = dev.createRfcommSocketToServiceRecord(SPP);
                            else if (mode == 1) t = dev.createInsecureRfcommSocketToServiceRecord(SPP);
                            else t = (BluetoothSocket) dev.getClass().getMethod("createRfcommSocket", int.class).invoke(dev, 1);
                            t.connect();
                            s = t;
                        } catch (Exception e) {
                            last = e;
                            try { if (t != null) t.close(); } catch (Exception ignored) { }
                        }
                    }
                    if (s == null) throw (last != null ? last : new Exception("connect failed"));
                    synchronized (BtPrinterPlugin.this) { socket = s; out = s.getOutputStream(); }
                    call.resolve();
                } catch (Exception e) {
                    closeQuiet();
                    call.reject("Printer ချိတ်၍ မရပါ — ဖွင့်ထားခြင်း/Pair ထားခြင်း စစ်ပါ (" + e.getMessage() + ")");
                }
            }
        }).start();
    }

    @PluginMethod
    public void write(final PluginCall call) {
        final String data = call.getString("data", "");
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                    synchronized (BtPrinterPlugin.this) {
                        if (out == null) { call.reject("Printer မချိတ်ရသေးပါ"); return; }
                        out.write(bytes);
                        out.flush();
                    }
                    call.resolve();
                } catch (Exception e) {
                    closeQuiet();
                    call.reject("Printer သို့ ပို့၍ မရပါ (" + e.getMessage() + ")");
                }
            }
        }).start();
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        closeQuiet();
        call.resolve();
    }

    private synchronized void closeQuiet() {
        try { if (out != null) out.close(); } catch (Exception ignored) { }
        try { if (socket != null) socket.close(); } catch (Exception ignored) { }
        out = null; socket = null;
    }

    @Override
    protected void handleOnDestroy() { closeQuiet(); }
}
