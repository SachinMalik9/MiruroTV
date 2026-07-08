package com.mirurotv.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TARGET_URL = "https://www.miruro.tv/"
    }

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var chromeClient: TVWebChromeClient
    private lateinit var cursorOverlay: CursorOverlayView

    // Track key repeat for smooth movement
    private var isMoving = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen immersive mode
        setupFullscreen()

        setContentView(R.layout.activity_main)

        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        cursorOverlay = findViewById(R.id.cursorOverlay)

        setupWebView()
        webView.loadUrl(TARGET_URL)
    }

    private fun setupFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.webView)

        // Enable remote debugging (remove in production if desired)
        WebView.setWebContentsDebuggingEnabled(true)

        // Get ad blocker from Application
        val app = application as MiruroTVApp

        // Set WebView clients
        val webViewClient = TVWebViewClient(app.adBlocker) { recreateWebView() }
        webView.webViewClient = webViewClient

        chromeClient = TVWebChromeClient(webView, fullscreenContainer)
        webView.webChromeClient = chromeClient

        // Configure WebView settings
        webView.settings.apply {
            // JavaScript & Storage
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true

            // Media
            mediaPlaybackRequiresUserGesture = false // Allow autoplay on TV

            // Content loading
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            loadWithOverviewMode = true
            useWideViewPort = true

            // Cache
            cacheMode = WebSettings.LOAD_DEFAULT

            // Popups - block them
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false

            // Security
            allowFileAccess = false
            allowContentAccess = false

            // User Agent - remove WebView identifier for full site experience
            userAgentString = userAgentString.replace("; wv)", ")")
        }

        // Cookies - enable for login/preferences
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Focus for D-pad navigation
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        // Low-RAM optimization
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.isLowRamDevice) {
            webView.settings.apply {
                setSupportZoom(false)
                builtInZoomControls = false
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
        }

        // Set renderer priority - waive when not visible to save RAM
        webView.setRendererPriorityPolicy(
            WebView.RENDERER_PRIORITY_IMPORTANT,
            true // waived when not visible
        )
    }

    // ==========================================
    // VIRTUAL MOUSE CURSOR - D-PAD NAVIGATION
    // ==========================================

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Don't intercept during fullscreen video
        if (chromeClient.isFullscreen()) {
            return super.dispatchKeyEvent(event)
        }

        val keyCode = event.keyCode

        when (keyCode) {
            // D-pad movement → move cursor
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val dx = when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> -1f
                        KeyEvent.KEYCODE_DPAD_RIGHT -> 1f
                        else -> 0f
                    }
                    val dy = when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> -1f
                        KeyEvent.KEYCODE_DPAD_DOWN -> 1f
                        else -> 0f
                    }
                    cursorOverlay.moveCursor(dx, dy)

                    // Simulate mouse hover at cursor position
                    simulateMouseMove(cursorOverlay.cursorX, cursorOverlay.cursorY)

                    // Auto-scroll when cursor is near edges
                    autoScrollAtEdges()

                    isMoving = true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    cursorOverlay.resetAcceleration()
                    isMoving = false
                }
                return true
            }

            // OK/Select/Enter → simulate mouse click
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    simulateClick(cursorOverlay.cursorX, cursorOverlay.cursorY)
                }
                return true
            }

            // Back button
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    when {
                        chromeClient.isFullscreen() -> chromeClient.onHideCustomView()
                        webView.canGoBack() -> webView.goBack()
                        else -> @Suppress("DEPRECATION") super.onBackPressed()
                    }
                }
                return true
            }

            // Menu button → toggle cursor visibility
            KeyEvent.KEYCODE_MENU -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    cursorOverlay.isVisible = !cursorOverlay.isVisible
                }
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun simulateMouseMove(x: Float, y: Float) {
        // Inject JavaScript to dispatch mousemove event at cursor position
        val js = """
            (function() {
                var el = document.elementFromPoint($x, $y);
                if (el) {
                    var evt = new MouseEvent('mouseover', {
                        bubbles: true, cancelable: true,
                        clientX: $x, clientY: $y
                    });
                    el.dispatchEvent(evt);
                    var moveEvt = new MouseEvent('mousemove', {
                        bubbles: true, cancelable: true,
                        clientX: $x, clientY: $y
                    });
                    el.dispatchEvent(moveEvt);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun simulateClick(x: Float, y: Float) {
        // Inject JavaScript to simulate a full click sequence at cursor position
        val js = """
            (function() {
                var el = document.elementFromPoint($x, $y);
                if (el) {
                    // Try to find clickable parent (a, button, etc.)
                    var clickable = el;
                    var maxDepth = 10;
                    while (clickable && maxDepth > 0) {
                        var tag = clickable.tagName.toLowerCase();
                        if (tag === 'a' || tag === 'button' || tag === 'input' ||
                            tag === 'select' || tag === 'video' ||
                            clickable.onclick || clickable.getAttribute('role') === 'button' ||
                            clickable.getAttribute('tabindex') !== null ||
                            window.getComputedStyle(clickable).cursor === 'pointer') {
                            break;
                        }
                        clickable = clickable.parentElement;
                        maxDepth--;
                    }
                    if (!clickable) clickable = el;

                    // Dispatch full mouse event sequence
                    ['mousedown', 'mouseup', 'click'].forEach(function(type) {
                        var evt = new MouseEvent(type, {
                            bubbles: true, cancelable: true,
                            clientX: $x, clientY: $y,
                            button: 0, buttons: 1,
                            view: window
                        });
                        clickable.dispatchEvent(evt);
                    });

                    // Also try touch events for touch-only listeners
                    try {
                        var touchStart = new TouchEvent('touchstart', {
                            bubbles: true, cancelable: true,
                            touches: [new Touch({
                                identifier: 1, target: clickable,
                                clientX: $x, clientY: $y
                            })]
                        });
                        clickable.dispatchEvent(touchStart);
                        var touchEnd = new TouchEvent('touchend', {
                            bubbles: true, cancelable: true,
                            changedTouches: [new Touch({
                                identifier: 1, target: clickable,
                                clientX: $x, clientY: $y
                            })]
                        });
                        clickable.dispatchEvent(touchEnd);
                    } catch(e) {}

                    // Focus if it's an input/focusable element
                    if (clickable.focus) clickable.focus();
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun autoScrollAtEdges() {
        val edgeThreshold = 60f // pixels from edge to start scrolling
        val scrollAmount = 40

        val x = cursorOverlay.cursorX
        val y = cursorOverlay.cursorY
        val w = cursorOverlay.width.toFloat()
        val h = cursorOverlay.height.toFloat()

        val scrollX = when {
            x < edgeThreshold -> -scrollAmount
            x > w - edgeThreshold -> scrollAmount
            else -> 0
        }
        val scrollY = when {
            y < edgeThreshold -> -scrollAmount
            y > h - edgeThreshold -> scrollAmount
            else -> 0
        }

        if (scrollX != 0 || scrollY != 0) {
            val js = "window.scrollBy($scrollX, $scrollY);"
            webView.evaluateJavascript(js, null)
        }
    }

    // ==========================================
    // LIFECYCLE
    // ==========================================

    private fun recreateWebView() {
        runOnUiThread {
            val parent = webView.parent as? ViewGroup
            parent?.removeView(webView)
            webView.removeAllViews()
            webView.destroy()

            setupWebView()
            // Add webview back at index 0 (behind cursor overlay)
            parent?.addView(webView, 0)
            webView.loadUrl(TARGET_URL)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            chromeClient.isFullscreen() -> chromeClient.onHideCustomView()
            webView.canGoBack() -> webView.goBack()
            else -> @Suppress("DEPRECATION") super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.requestFocus()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        @Suppress("DEPRECATION")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            webView.clearCache(true)
        }
    }

    override fun onDestroy() {
        val parent = webView.parent as? ViewGroup
        parent?.removeView(webView)
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}
