package com.mirurotv.app

import android.graphics.Bitmap
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class TVWebViewClient(
    private val adBlocker: AdBlocker,
    private val onRecreateNeeded: () -> Unit
) : WebViewClient() {

    companion object {
        private const val TAG = "TVWebViewClient"
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val host = request.url.host ?: return super.shouldInterceptRequest(view, request)

        // Block ad domains
        if (adBlocker.isAdDomain(host)) {
            Log.d(TAG, "Blocked: $host")
            return adBlocker.createBlockedResponse()
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)

        // Inject ad-blocking CSS
        val css = adBlocker.getAdBlockCSS()
        val cssInjection = """
            (function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.innerHTML = '${css.replace("'", "\\'")}' ;
                document.head.appendChild(style);
            })();
        """.trimIndent()
        view.evaluateJavascript(cssInjection, null)

        // Inject ad-blocking JavaScript
        view.evaluateJavascript(adBlocker.getAdBlockJS(), null)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val host = request.url.host ?: return true

        // Block navigation to ad domains
        if (adBlocker.isAdDomain(host)) {
            return true // Block the navigation
        }

        // Allow all other navigations (streaming site + CDNs)
        return false
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: RenderProcessGoneDetail
    ): Boolean {
        if (!detail.didCrash()) {
            // Renderer killed by system for memory — recreate
            Log.w(TAG, "Renderer killed by system, requesting recreate...")
            onRecreateNeeded()
            return true
        }
        // Actual crash
        Log.e(TAG, "Renderer crashed!")
        onRecreateNeeded()
        return true
    }
}
