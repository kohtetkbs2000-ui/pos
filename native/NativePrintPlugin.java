package com.shoppos.app;

import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * NativePrint.print({ html, name, size: 'a4' | 'th' | 'custom', wmm, hmm })
 * Renders the HTML in a WebView and opens Android's print dialog
 * (any installed printer / print service, or "Save as PDF").
 */
@CapacitorPlugin(name = "NativePrint")
public class NativePrintPlugin extends Plugin {

    private WebView keep; // keep a reference until the print job is handed over

    private static int mils(double mm) {
        return (int) Math.round(mm / 25.4 * 1000.0);
    }

    private PrintAttributes attrs(String size, double wmm, double hmm) {
        PrintAttributes.MediaSize ms;
        if ("a4".equals(size)) {
            ms = PrintAttributes.MediaSize.ISO_A4;
        } else if ("custom".equals(size) && wmm > 0 && hmm > 0) {
            if (wmm > hmm) {
                ms = new PrintAttributes.MediaSize("label", "Label", mils(hmm), mils(wmm)).asLandscape();
            } else {
                ms = new PrintAttributes.MediaSize("label", "Label", mils(wmm), mils(hmm));
            }
        } else {
            ms = new PrintAttributes.MediaSize("roll80", "80mm Roll", mils(80), mils(297));
        }
        return new PrintAttributes.Builder()
                .setMediaSize(ms)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
    }

    @PluginMethod
    public void print(final PluginCall call) {
        final String html = call.getString("html");
        if (html == null || html.isEmpty()) {
            call.reject("html is empty");
            return;
        }
        final String name = call.getString("name", "document");
        final String size = call.getString("size", "a4");
        final double wmm = call.getDouble("wmm", 80.0);
        final double hmm = call.getDouble("hmm", 100.0);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final WebView wv = new WebView(getActivity());
                    keep = wv;
                    wv.getSettings().setJavaScriptEnabled(false);
                    wv.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            try {
                                PrintManager pm = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);
                                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(name);
                                pm.print(name, adapter, attrs(size, wmm, hmm));
                                JSObject res = new JSObject();
                                res.put("ok", true);
                                call.resolve(res);
                            } catch (Exception e) {
                                call.reject("Print failed: " + e.getMessage());
                            }
                        }
                    });
                    wv.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
                } catch (Exception e) {
                    call.reject("Print failed: " + e.getMessage());
                }
            }
        });
    }
}
