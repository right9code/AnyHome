# AnyHome

A minimalist, high-contrast, distraction-free Android launcher tailored specifically for E-Ink devices (such as Bigme, Onyx Boox, Meebook, etc.) with dedicated **Home App Mode** support (ideal for KOReader, Kindle, or your favorite e-reading software).

---

## ✨ Features

- **Home App Mode (Dedicated Reader / Home App):**
  - Designate any app (e.g. KOReader) as your primary Home App.
  - Automatically launches into your dedicated app on home button press or system boot.
  - Built-in loop guard mechanism to prevent accidental launch locks.
- **E-Ink High-Contrast Design:**
  - High-legibility monochrome and adaptive icon styling.
  - Text-Island outlines for pristine readability on E-Ink pearl/carta displays.
  - Zero unnecessary background animations or battery-draining rendering overhead.
  - Custom monogram generation mode.
- **Customizable Layout:**
  - Configurable grid rows and columns.
  - Quick A–Z strip navigation and instant search.
  - Pinned app slots for your most frequent tools.
  - Custom TTF/OTF font loading.
- **Quick Settings & Caffeine Companion:**
  - Quick Tiles for toggling Home App Mode and Caffeine (keeps screen awake during reading sessions).
  - Optional Accessibility companion service for e-ink screen refresh and navigation handling.

---

## 🚀 Building from Source

### Prerequisites
- Android SDK (API 35 / Android 15 ready, minSdk 26)
- JDK 17+

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This software is released under the **PolyForm Noncommercial License 1.0.0**.

- ✅ **Free for personal, individual, and educational use.**
- ❌ **Commercial use, redistribution as part of commercial products, or resale is strictly prohibited without prior written authorization.**

See [LICENSE](LICENSE) for full legal text.
