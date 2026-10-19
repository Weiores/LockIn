# LockIn 🔒📱

> **Context-Aware, Hardware-Triggered Deep-Work & Study Timer for University Students**  
> Native Android · Kotlin · Jetpack Compose · Material 3 · Room · Hilt · Apple Glassmorphism UI

---

## 🌟 Overview
**LockIn** eliminates study procrastination by binding Pomodoro and deep-work timers to physical sensors, contactless cards, optical barcodes, and indoor network contexts—requiring zero dedicated external hardware.

### 🎯 Key Multi-Modal Triggers
1. **Flip-to-Lock (Posture Sensor):** Accelerometer ($Z$-axis $\approx -9.8\text{ m/s}^2$) + Proximity sensor ($0\text{ cm}$) to verify the phone is face-down on a desk. Lifting the phone initiates a 10-second grace period with vibration before breaking the streak.
2. **Barcode / QR Checkpoint (Optical):** CameraX + Google ML Kit Barcode Scanning (bundled, offline) to require scanning a desk/textbook barcode to begin or strictly unlock a session.
3. **Contactless Card Hardware Key (NFC UID):** `NfcAdapter.enableReaderMode()` reads the permanent hardware UID of matriculation cards or transit cards without needing writable tags.
4. **Study Zone Auto-Prompt (Wi-Fi BSSID):** `ConnectivityManager` detects library/campus router BSSIDs and prompts to begin focused sessions.
5. **Distraction Guard:** `UsageStatsManager` polling detects if blacklisted entertainment apps are opened during active lock sessions and presents a high-friction reminder overlay.

---

## 🎨 UI/UX: Apple Glassmorphism Design System
- **OLED Slate Gradient:** `#0D0F14` to `#141721` deep dark canvas.
- **`GlassCard` Architecture:** Frosted glass containers featuring translucent vertical fills, ultra-crisp 1.dp edge highlights, and soft ambient drop shadows.
- **Apple Accent Palette:** System Blue (`#0A84FF`), Cyan (`#64D2FF`), Success Green (`#34C759`), and Warning Red (`#FF453A`).

---

## 🛠️ Tech Stack & Architecture
- **Language:** Kotlin 2.0 (Coroutines + `Flow` / `StateFlow` / `SharedFlow`)
- **UI Framework:** Jetpack Compose (Material 3 Dark-First, Custom `Canvas` Rings)
- **Architecture:** Clean MVVM (UI -> ViewModel -> Use Cases -> Repository -> Data Sources)
- **Dependency Injection:** Hilt 2.55 (KSP)
- **Persistence:** Room Database 2.6 (`ModuleEntity`, `SessionEntity`, `SavedTriggerEntity`) + Jetpack DataStore Preferences
- **Background Services:** Typed Foreground Service (`specialUse`), `AlarmManager` for drift-free wakeups, `WorkManager` for daily streaks
- **Hardware Integration:** CameraX, ML Kit Barcode Scanning, `SensorManager`, `NfcAdapter` wrapped as lifecycle-aware `callbackFlow` streams
- **Minimum SDK:** 31 (Android 12+) | **Target/Compile SDK:** 35 (Android 15)

---

## 📂 Project Structure
```
com.lockin.app/
├── data/
│   ├── local/db/         # Room Database, Entities, DAOs, TypeConverters
│   ├── local/datastore/  # UserPreferencesDataStore with crash-recovery snapshots
│   └── repository/       # Repository implementations
├── domain/
│   ├── model/            # Clean domain models & state machine interfaces
│   └── repository/       # Repository contracts
├── ui/
│   ├── components/       # GlassCard, AccentGlassCard, Canvas components
│   ├── focushub/         # Tab 1: Streak header, Wi-Fi pill, timer dial, CTA
│   ├── navigation/       # Type-safe @Serializable Compose routes
│   └── theme/            # Apple Glassmorphism tokens, Dark-first theme
├── service/              # Persistent Timer Foreground Service
├── di/                   # Hilt dependency injection modules
└── util/                 # App-wide constants & helpers
```

---

## 📄 License
MIT License. Built for university students seeking deep focus.

