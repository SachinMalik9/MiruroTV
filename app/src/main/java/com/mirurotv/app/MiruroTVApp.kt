package com.mirurotv.app

import android.app.Application
import android.webkit.WebView

class MiruroTVApp : Application() {

    lateinit var adBlocker: AdBlocker
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize ad blocker (loads domain list on background thread)
        adBlocker = AdBlocker(this)

        // Pre-warm WebView to eliminate cold-start jank (~300-500ms saved)
        try {
            WebView(this).destroy()
        } catch (_: Exception) {
            // Ignore errors during pre-warming
        }
    }
}
