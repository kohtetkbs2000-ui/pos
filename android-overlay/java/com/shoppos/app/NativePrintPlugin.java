package com.shoppos.app;

import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Opens the Android print dialog (printer / Save as PDF) for a voucher HTML page. */
@CapacitorPlugin(name = "NativePrint")
public class NativePrintPlugin extends Plugin {
    private WebView printView;

    @PluginMethod
    public void print(final PluginCall call) {
        final String html = call.getString("html", "");
        final String name = call.getString("name", "voucher");
        final String size = call.getString("size", "a4");
        final Object wo = call.getData().opt("wmm"), ho = call.getData().opt("hmm");
        final double wmm = wo instanceof Number ? ((Number) wo).doubleValue() : 0;
        final double hmm = ho instanceof Number ? ((Number) ho).doubleValue() : 0;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    printView = new WebView(getActivity());
                    printView.setWebViewClient(new WebViewClient() {
                        private boolean started = false;

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            if (started) return;
                            started = true;
                            PrintManager pm = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);
                            PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(name);
                            PrintAttributes.Builder b = new PrintAttributes.Builder();
                            if (wmm > 0 && hmm > 0) {
                                int wm = (int) Math.round(wmm / 25.4 * 1000), hm = (int) Math.round(hmm / 25.4 * 1000);
                                b.setMediaSize(new PrintAttributes.MediaSize("lbl" + wm + "x" + hm, "Label", wm, hm));
                                b.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
                            } else if ("th".equals(size)) {
                                b.setMediaSize(new PrintAttributes.MediaSize("th80", "80mm", 3150, 11000));
                                b.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
                            } else {
                                b.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
                            }
                            pm.print(name, adapter, b.build());
                            call.resolve();
                        }
                    });
                    printView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
                } catch (Exception e) {
                    call.reject(String.valueOf(e.getMessage()));
                }
            }
        });
    }
}
