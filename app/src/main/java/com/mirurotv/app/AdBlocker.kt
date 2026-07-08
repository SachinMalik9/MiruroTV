package com.mirurotv.app

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

class AdBlocker(context: Context) {

    private val blockedDomains: HashSet<String> = HashSet(5000)

    init {
        loadBlockList(context)
    }

    private fun loadBlockList(context: Context) {
        try {
            context.assets.open("ad_domains.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        // Handle hosts file format: "0.0.0.0 domain.com" or just "domain.com"
                        val domain = if (trimmed.contains(" ")) {
                            trimmed.substringAfterLast(" ").trim()
                        } else {
                            trimmed
                        }
                        if (domain.isNotEmpty() && domain.contains(".")) {
                            blockedDomains.add(domain.lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If file fails to load, use hardcoded fallback list
            loadFallbackList()
        }
    }

    private fun loadFallbackList() {
        val fallback = listOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "adservice.google.com",
            "pagead2.googlesyndication.com", "ads.yahoo.com", "ad.doubleclick.net",
            "static.ads-twitter.com", "ads.pubmatic.com", "cdn.taboola.com",
            "cdn.outbrain.com", "securepubads.g.doubleclick.net",
            "tpc.googlesyndication.com", "s0.2mdn.net", "z-na.amazon-adsystem.com",
            "advertising.com", "adnxs.com", "adsrvr.org", "rubiconproject.com",
            "casalemedia.com", "openx.net", "pubmatic.com", "sharethrough.com",
            "smaato.net", "spotxchange.com", "contextweb.com", "smartadserver.com"
        )
        blockedDomains.addAll(fallback)
    }

    fun isAdDomain(host: String): Boolean {
        val lowerHost = host.lowercase()

        // Check exact match
        if (blockedDomains.contains(lowerHost)) return true

        // Check parent domains (e.g., "ads.example.com" → check "example.com")
        val parts = lowerHost.split(".")
        for (i in 1 until parts.size - 1) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parentDomain)) return true
        }
        return false
    }

    fun createBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun getAdBlockCSS(): String {
        return """
            [class*="ad-"], [class*="ads-"], [class*="advert"],
            [id*="ad-"], [id*="ads-"], [id*="advert"],
            [class*="banner" i], [class*="sponsor" i],
            [class*="popup" i]:not([class*="player"]),
            iframe[src*="doubleclick"], iframe[src*="googlesyndication"],
            iframe[src*="facebook.com/plugins"],
            div[data-ad], div[data-ads], div[data-ad-slot],
            ins.adsbygoogle, .ad-wrapper, .ad-container, .ad-placeholder,
            .ad-overlay, .ad-banner, .ads-banner,
            #ad-header, #ad-footer, #ad-sidebar {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                min-height: 0 !important;
                max-height: 0 !important;
                overflow: hidden !important;
                opacity: 0 !important;
                pointer-events: none !important;
            }
        """.trimIndent().replace("\n", " ")
    }

    fun getAdBlockJS(): String {
        return """
            (function() {
                // Remove ad iframes
                document.querySelectorAll('iframe').forEach(function(iframe) {
                    var src = (iframe.src || '').toLowerCase();
                    if (src.includes('doubleclick') || src.includes('googlesyndication') ||
                        src.includes('adserver') || src.includes('ads.') ||
                        src.includes('taboola') || src.includes('outbrain') ||
                        src.includes('amazon-adsystem')) {
                        iframe.remove();
                    }
                });

                // Remove elements with ad-related classes/ids
                var adSelectors = [
                    '[class*="ad-wrapper"]', '[class*="ad-container"]',
                    '[class*="ad-banner"]', '[class*="ad-overlay"]',
                    '[id*="ad-wrapper"]', '[id*="ad-container"]',
                    'ins.adsbygoogle', '[data-ad]', '[data-ads]'
                ];
                document.querySelectorAll(adSelectors.join(',')).forEach(function(el) {
                    el.remove();
                });

                // MutationObserver for dynamically loaded ads
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) {
                                var cl = (node.className || '').toString().toLowerCase();
                                var id = (node.id || '').toLowerCase();
                                var tag = node.tagName.toLowerCase();
                                if (cl.includes('ad-') || cl.includes('ads-') ||
                                    cl.includes('advert') || cl.includes('sponsor') ||
                                    id.includes('ad-') || id.includes('ads-') ||
                                    (tag === 'ins' && cl.includes('adsbygoogle'))) {
                                    node.style.display = 'none';
                                    node.style.visibility = 'hidden';
                                    node.style.height = '0';
                                }
                                // Also check for ad iframes in new nodes
                                if (tag === 'iframe') {
                                    var src = (node.src || '').toLowerCase();
                                    if (src.includes('doubleclick') || src.includes('googlesyndication') ||
                                        src.includes('adserver') || src.includes('ads.')) {
                                        node.remove();
                                    }
                                }
                            }
                        });
                    });
                });
                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                }

                // Block window.open
                window.open = function() { return null; };
            })();
        """.trimIndent()
    }
}
