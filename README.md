# PrintingGarbled

A minimal Android app to reproduce **garbled printing issues** when using the TSC Printer SDK (`tscsdk.jar`) to print PDF files via Bluetooth on the **TSC Alpha-40L** printer.

This project is intended for the **TSC Printer dev team** to reproduce and diagnose the issue.

## Problem Description

When printing certain PDF files using `TSCActivity.printPDFbyFile()` on the **TSC Alpha-40L**, the output is garbled/corrupted. The issue is intermittent — some PDFs print correctly while others do not.

## Test PDF Files

The `app/src/main/assets/` folder contains 6 test PDFs:

| File                                  | Result              |
| ------------------------------------- | ------------------- |
| `1772170565703_250646575 - Pass.pdf`  | ✅ Prints correctly |
| `1772172257369_250846099 - Pass.pdf`  | ✅ Prints correctly |
| `1772172361576_250050845  - Pass.pdf` | ✅ Prints correctly |
| `1772172626834_250033423 - Fail.pdf`  | ❌ Garbled output   |
| `1772172993599_250313201 - Fail.pdf`  | ❌ Garbled output   |
| `1772173144558_250448540 - Fail.pdf`  | ❌ Garbled output   |

## Project Setup & Run Instructions

### Prerequisites

| Requirement                 | Version                          |
| --------------------------- | -------------------------------- |
| Android Studio              | Latest stable (Ladybug or newer) |
| JDK                         | 11+                              |
| Gradle                      | 8.13 (bundled via wrapper)       |
| AGP (Android Gradle Plugin) | 8.11.2                           |
| Kotlin                      | 2.0.21                           |
| Android SDK                 | compileSdk 36, minSdk 24         |

You also need:

- A physical Android device (API 24+) with Bluetooth
- A **TSC Alpha-40L** printer paired with the device via Bluetooth

### Step 1: Clone / Open the Project

```bash
git clone https://github.com/bafs-id/PrintingGarbled.git
```

Open the project folder in **Android Studio**:
**File → Open → select the `PrintingGarbled` root folder**

Android Studio will automatically download the Gradle wrapper (8.13) and sync dependencies.

### Step 2: Verify the TSC SDK

The TSC SDK (`tscsdk.jar`) is already included at:

```
app/libs/tscsdk.jar
```

No additional SDK setup is required.

### Step 3: Build the Project

From Android Studio:
**Build → Make Project** (or press `Ctrl+F9` / `Cmd+F9`)

Or from terminal:

```bash
./gradlew assembleDebug
```

### Step 4: Connect a Device

1. Connect an Android device via USB (API 24+ / Android 7.0+).
2. Enable **Developer Options** and **USB Debugging** on the device.
3. Pair the **TSC Alpha-40L** printer with the device via Android Bluetooth settings.

> **Note:** This app requires a physical device — Bluetooth printing cannot be tested on an emulator.

### Step 5: Run the App

From Android Studio:
**Run → Run 'app'** (or press `Shift+F10` / `Ctrl+R`)

Or from terminal:

```bash
./gradlew installDebug
```

Then launch **"Printing Garbled"** from the device's app drawer.

### Step 6: Reproduce the Issue

1. Enter the printer's **Bluetooth MAC address** (e.g. `00:11:22:33:44:55`).
2. Tap **Check Status** to verify the connection — status should show "Connected OK".
3. Select a **Fail** PDF from the dropdown and tap **Print Selected** to see the garbled output.
4. Select a **Pass** PDF and tap **Print Selected** to see correct output for comparison.
5. Tap **Print All** to print all 6 PDFs in sequence.

### Permissions

On Android 12+ (API 31+), the app will request:

- `BLUETOOTH_CONNECT`
- `BLUETOOTH_SCAN`

Grant these when prompted. On older Android versions, legacy `BLUETOOTH` and `BLUETOOTH_ADMIN` permissions are used (granted at install time).

## Printing Flow

The app uses the following TSC SDK calls:

```
openport(bluetoothAddress)      → Connect via Bluetooth
printPDFbyFile(file, 0, 0, 200) → Print PDF
closeport(500)                  → Disconnect
```

## Project Structure

```
PrintingGarbled/
├── app/
│   ├── libs/
│   │   └── tscsdk.jar              ← TSC Printer SDK
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── *Pass.pdf            ← PDFs that print correctly
│       │   └── *Fail.pdf            ← PDFs that print garbled
│       ├── java/.../MainActivity.kt ← Main activity with print logic
│       └── res/layout/activity_main.xml
├── gradle/
│   └── libs.versions.toml          ← Version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

## Tech Stack

- **Language:** Kotlin 2.0.21
- **Min SDK:** 24 / **Target SDK:** 36
- **Gradle:** 8.13 / **AGP:** 8.11.2
- **TSC SDK:** `tscsdk.jar` (`com.example.tscdll.TSCActivity`)
- **Printer:** TSC Alpha-40L
- **Connection:** Bluetooth Classic
