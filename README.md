# 📺 MiruroTV — Ad-Free Android TV Browser

A lightweight, ad-blocking Android TV app that wraps [miruro.tv](https://www.miruro.tv/) in an optimized WebView with a built-in virtual mouse cursor for easy navigation.

![Platform](https://img.shields.io/badge/Platform-Android%20TV-green)
![Min SDK](https://img.shields.io/badge/Min%20SDK-23%20(Android%206.0)-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
![APK Size](https://img.shields.io/badge/APK%20Size-~2MB-orange)

## ✨ Features

- 🛡️ **3-Layer Ad Blocker** — Blocks 160+ ad/tracker domains at the network level + CSS/JS injection
- 🖱️ **Virtual Mouse Cursor** — Navigate any website with your TV remote D-pad
- 🎬 **Fullscreen Video** — Native fullscreen support for video playback
- ⚡ **Ultra Lightweight** — Only 3 dependencies, < 80MB RAM usage
- 🔄 **Crash Recovery** — Auto-recovers if system kills WebView for memory
- 🍪 **Persistent Sessions** — Login and preferences saved between sessions
- 📺 **TV Optimized** — Landscape mode, hardware accelerated, 60fps smooth

## 🎮 Remote Control

| Button | Action |
|--------|--------|
| ↑ ↓ ← → (D-pad) | Move cursor around the screen |
| OK / Select | Click where cursor is pointing |
| Back | Exit fullscreen → Go back → Exit app |
| Menu | Show/hide cursor |

## 🚀 Quick Start

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (any recent version)
- Android SDK with API 35

### Build
1. Clone this repo or download the ZIP
2. Open in Android Studio
3. Wait for Gradle sync
4. **Build → Build APK(s)**

### Install on Android TV
```bash
# Connect to your TV via ADB
adb connect YOUR_TV_IP:5555

# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to a USB drive and install via a file manager app.

## 🔧 Change the Target Website

Edit `MainActivity.kt` line 14:
```kotlin
private const val TARGET_URL = "https://www.miruro.tv/"
// Change to any website you want:
// private const val TARGET_URL = "https://www.youtube.com/"
```

## 🛡️ Ad Blocker

The built-in ad blocker uses 3 layers of protection:

| Layer | Method | What it does |
|-------|--------|-------------|
| **1. Network** | `shouldInterceptRequest()` | Blocks requests to 160+ ad domains before they load |
| **2. CSS** | Style injection | Hides ad containers, overlays, and popups |
| **3. JavaScript** | Script injection | Removes ad elements + MutationObserver for dynamic ads |

### Add Custom Blocked Domains
Edit `app/src/main/assets/ad_domains.txt` — add one domain per line:
```
# My custom blocks
annoying-ads.com
tracker-site.net
```

## 📁 Project Structure

```
app/src/main/
├── java/com/mirurotv/app/
│   ├── MainActivity.kt          # WebView + virtual cursor navigation
│   ├── AdBlocker.kt             # 3-layer ad blocking engine
│   ├── TVWebViewClient.kt       # Request interception + ad injection
│   ├── TVWebChromeClient.kt     # Fullscreen video + popup blocking
│   ├── CursorOverlayView.kt     # Virtual mouse cursor for D-pad
│   └── MiruroTVApp.kt           # App startup + WebView pre-warming
├── res/
│   ├── layout/activity_main.xml  # WebView + cursor overlay layout
│   ├── drawable/banner.xml       # TV launcher banner
│   └── values/                   # Theme, colors, strings
├── assets/
│   └── ad_domains.txt            # Blocked ad/tracker domains
└── AndroidManifest.xml           # TV app configuration
```

## 📊 Specs

| Metric | Value |
|--------|-------|
| APK Size | ~2-3 MB |
| RAM Usage | ~50-80 MB |
| Dependencies | 3 (core-ktx, appcompat, webkit) |
| Min Android | 6.0 (API 23) |
| Target Android | 14 (API 34) |
| Ad Domains Blocked | 160+ |

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Add more ad domains to `ad_domains.txt`
- Improve cursor navigation
- Add new features
- Report bugs

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

This app is for personal use. It wraps third-party websites and is not affiliated with any streaming service. The ad blocker is provided for a better user experience. Please support content creators when possible.
