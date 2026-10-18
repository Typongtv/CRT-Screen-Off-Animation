<p align="center">
  <img src="docs/images/crt_hero_banner.jpg" alt="CRT Screen-Off Presentation Banner" width="100%" />
</p>

# CRT Screen-Off Animation

<p align="center">
  <strong>CyanogenMod-Inspired Retro Electron-Beam Sleep Animation Engine for Modern Android (Android 13–16+)</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2013%2B%20(API%2033%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Root-100%25%20Rootless-brightgreen?style=flat-square" alt="Rootless" />
  <img src="https://img.shields.io/badge/GPU-AGSL%20RuntimeShader-blueviolet?style=flat-square" alt="AGSL" />
  <img src="https://img.shields.io/badge/Knox%20%2F%20SafetyNet-100%25%20Safe-success?style=flat-square" alt="SafetyNet Safe" />
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square" alt="License" />
</p>

---

## Project Heritage & Lineage

During the early days of Android (Android 2.3+ and the classic CyanogenMod era), one visual touch captured everyone's imagination: the iconic **CRT screen-off animation**. Handled natively inside Android's display compositor (`ElectronBeam.cpp`), it gave touchscreens the tactile soul of an old cathode-ray television.

As deflection coils collapsed the vertical sweep, the electron beam density flared into a searing horizontal razor line before vanishing into pure cathode darkness. When newer Android releases retired this native effect, achieving it historically required device rooting, custom ROMs, or invasive framework-hooking tools that tripped Knox hardware fuses and broke SafetyNet / Play Integrity.

**CRT Screen-Off Animation** revives that beloved retro electron-beam experience completely **rootless on Android 13 through Android 16+**, combining official Android Accessibility APIs with high-performance, hardware-accelerated **AGSL (Android Graphics Shading Language)** rendering.

---

## Key Features

- **100% Rootless & Knox Safe:** Zero root, zero Magisk, and zero framework modification required. Knox fuses and Play Integrity remain untouched.
- **Dual Physics Animation Engines:**
  - **CyanogenMod Retro Engine:** Inspired by the classic CM7/CM9 `ElectronBeam` sweep with dynamic vertical squish, true-color prism split, delayed crescendo flare, and clean horizontal beam zip.
  - **Phosphor Overdrive Engine:** Simulates vintage CRT cathode tubes with customizable phosphor persistence (Green P1, Amber P3, Blue P4, White P4, Cyan), chromatic aberration, raster scanlines, and discharge flicker.
- **120Hz Hardware Acceleration:** Powered by native Android AGSL shaders running directly on the GPU for zero-stutter 120Hz deflection.
- **Zero Idle Battery Impact:** No persistent foreground service, background wake locks, or polling loops. The engine is completely dormant until invoked.
- **One-Tap & Gesture Triggers:**
  - Dedicated zero-flicker headless trigger (`LockTriggerActivity`).
  - Home screen shortcuts & launcher icons.
  - Native integration with **Samsung Good Lock (One Hand Operation+)** for edge-swipe CRT screen-off gestures.
  - Headless broadcast receiver (`com.typongtv.crtscreenoff.TRIGGER_LOCK`) for automation tools (Tasker, Macrodroid, ADB).

---

> [!CAUTION]
> ### Hardware Advisory: Capacitive Phosphor Bleed 😜
> When interacting with the settings dashboard, you may notice micro-static visual arcs tracing your fingertips across the AMOLED panel. Please remain calm—it is merely localized harmonic resonance between your screen glass and the virtual cathode-ray tube.
> 
> *Just kidding!* That’s my custom touch-static overlay at work. It's a purely aesthetic homage to the days when touching your TV screen gave you an electrostatic high-five and made the hairs on your forearm stand on end. No rubber-soled shoes required.

---

## How It Works

1. **Trigger:** You tap the **CRT Lock** shortcut, trigger a gesture via Samsung One Hand Operation+, or fire the broadcast.
2. **Instant Capture:** The lightweight `CrtAccessibilityService` captures the top-level screen frame entirely in-memory using Android's official `takeScreenshot` API.
3. **GPU Deflection:** An overlay window renders the collapsing frame through AGSL fragment shaders, reproducing electromagnetic deflection, chromatic aberration, and phosphor burn-in fade.
4. **Hardware Sleep:** Once the cathode flare collapses into darkness, the service calls `GLOBAL_ACTION_LOCK_SCREEN` to put the display to sleep cleanly.

---

## Security & Privacy Transparency

Because this app utilizes Android's **Accessibility Service** API, complete transparency is paramount:

* **NO INTERNET PERMISSION:** The application does **not** declare `android.permission.INTERNET` in its manifest. It is physically impossible for the app to send data off your device.
* **Ephemeral In-Memory Frame:** The screen capture is only held in volatile memory for the duration of the 300–400ms animation and is immediately recycled.
* **Zero Telemetry / Zero Tracking:** No analytics libraries, third-party SDKs, or background trackers are included.

---

## Installation & Android 13–16+ First-Time Setup Guide

Because this app is distributed directly via GitHub and utilizes Android's official Accessibility API, modern Android security layers require two quick one-time confirmations during installation:

### 1. Google Play Protect Warning

Google Play Protect actively flags APKs that are sideloaded outside the Google Play Store. Depending on your Android version and region, follow whichever option appears on your screen:

#### • Scenario A: Standard Warning ("Install anyway" is visible)
1. On the Play Protect pop-up, tap **"More details"**.
2. Tap **"Install anyway"**.

#### • Scenario B: Strict Block (No "Install anyway" option available)
If Google completely blocks the installation without an "Install anyway" button:
1. Open the **Google Play Store** app.
2. Tap your **Profile icon** (top-right) ➔ Tap **Play Protect**.
3. Tap the **gear icon** in the top-right corner.
4. Temporarily toggle **OFF** *"Scan apps with Play Protect"*.
5. Return to your file manager and install the APK (e.g., `CRT-Screen-Off-v1.1.0.apk`).
6. *(Recommended)* Return to Play Protect settings and toggle scanning back **ON**.

> **Note on Safety:** As verified in our open codebase and manifest, this application requests **0 internet permissions** (`android.permission.INTERNET` is not present). The app physically cannot connect to any server, collect data, or transmit information off your device.

---

### 2. Accessibility Toggle Greyed Out? ("Restricted Setting")

On **Android 13, 14, 15, and 16+**, Android automatically greys out Accessibility toggles for all sideloaded APKs by default. Use either of these methods to unlock it:

#### • Method A: The 3-Dot App Info Shortcut (All Android Devices)
1. On your home screen, **long-press the CRT Screen-Off app icon** and tap the **(App Info)** button.
2. Tap the **three dots (⋮)** in the top-right corner.
3. Tap **"Allow restricted settings"** and confirm with your fingerprint or PIN.
4. Now go to **Settings** ➔ **Accessibility** ➔ **Installed apps** ➔ **CRT Screen-Off** and toggle it **ON**.

#### • Method B: Samsung Galaxy (One UI) Settings Menu
1. Go to: **Settings** ➔ **Security and privacy**.
2. Tap **Controls and alerts** *(or "More security settings")*.
3. Tap **Accessibility special access** *(or "Allow restricted settings")*.
4. Turn on the toggle for **CRT Screen-Off**.
5. Return to **Accessibility** to activate the service.

---

## Building From Source

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 36 (targetSdk 36, minSdk 33)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Typongtv/CRT-Screen-Off-Animation.git
cd CRT-Screen-Off-Animation

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```
The compiled APK will be output to:
`app/build/outputs/apk/release/`

---

## License

**Copyright (c) 2026 Typongtv. All Rights Reserved.**

This repository and its source code are made publicly available for viewing, educational purposes, and personal security inspection only. Unauthorized copying, modification, distribution, redistribution, or publishing of this software, in source or binary form, is strictly prohibited without prior written permission from the copyright holder.
