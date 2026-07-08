package com.mirurotv.app

import android.os.Message
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout

class TVWebChromeClient(
    private val webView: WebView,
    private val fullscreenContainer: FrameLayout
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    fun isFullscreen(): Boolean = customView != null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        // Video is requesting fullscreen
        customView = view
        customViewCallback = callback

        webView.visibility = View.GONE
        fullscreenContainer.visibility = View.VISIBLE
        fullscreenContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onHideCustomView() {
        // Exit fullscreen
        fullscreenContainer.removeView(customView)
        fullscreenContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE

        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }

    // Block all popup windows (ads)
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        return false // Block all popups
    }

    // Suppress JavaScript alerts from ad scripts
    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String,
        result: JsResult
    ): Boolean {
        result.cancel()
        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String,
        result: JsResult
    ): Boolean {
        result.cancel()
        return true
    }
}
