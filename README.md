# Printing Issues

A minimal Android app to reproduce **two printing issues** when using the TSC Printer SDK (`tscsdk.jar`) to print PDF files via Bluetooth on the **TSC Alpha-40L** printer:

1. **Garbled output** — see [ISSUE_REPORT_GARBLED.md](ISSUE_REPORT_GARBLED.md)
2. **Blank page** — see [ISSUE_REPORT_BLANK_PAGE.md](ISSUE_REPORT_BLANK_PAGE.md)

This project is intended for the **TSC Printer dev team** to reproduce and diagnose both issues.

## Problem 1: Garbled Output

When printing certain PDF files using `TSCActivity.printPDFbyFile()` on the **TSC Alpha-40L**, the output is garbled/corrupted. The issue is intermittent — some PDFs print correctly while others do not.

## Problem 2: Blank Page

A separate, more recent issue. When printing the affected PDF, the failure happens in a specific **two-tap pattern** (the user must tap the Print button **twice**):

1. **Tap Print (1st time) — nothing happens.** The SDK call returns but the printer does **not** feed any paper. No output, no error.
2. **Tap Print (2nd time) — blank page is fed.** The printer feeds a page, but the page comes out **completely blank** (no content at all).

**This pattern is 100% consistent for this PDF — the first tap never prints, the second tap always feeds a blank page.** It is not intermittent.

**The issue also reproduces on every TSC Alpha-40L printer unit we have tested**, so it is not a single faulty hardware unit — it points at the SDK or firmware.

The Bluetooth connection check succeeds before each tap, so the connection itself is not the problem. See [ISSUE_REPORT_BLANK_PAGE.md](ISSUE_REPORT_BLANK_PAGE.md) for the full report.

### Blank Page Test PDF

| File                                  | Result        |
| ------------------------------------- | ------------- |
| `1778666792328_260680042 - Blank.pdf` | ⚠️ Blank page |

## Garbled Test PDF Files

The `app/src/main/assets/` folder contains 6 test PDFs for the garbled-output issue:

| File                                 | Result              |
| ------------------------------------ | ------------------- |
| `1772170565703_250646575 - Pass.pdf` | ✅ Prints correctly |
| `1772172257369_250846099 - Pass.pdf` | ✅ Prints correctly |
| `1772172361576_250050845 - Pass.pdf` | ✅ Prints correctly |
| `1772172626834_250033423 - Fail.pdf` | ❌ Garbled output   |
| `1772172993599_250313201 - Fail.pdf` | ❌ Garbled output   |
| `1772173144558_250448540 - Fail.pdf` | ❌ Garbled output   |

## Print Results

Photos of actual printed output are in the `Print Results/` folder:

### ✅ Pass (Printed Correctly)

| 1772170565703_250646575                                           | 1772172257369_250846099                                           | 1772172361576_250050845                                              |
| ----------------------------------------------------------------- | ----------------------------------------------------------------- | -------------------------------------------------------------------- |
| ![Pass 1](Print%20Results/1772170565703_250646575%20-%20Pass.jpg) | ![Pass 2](Print%20Results/1772172257369_250846099%20-%20Pass.jpg) | ![Pass 3](Print%20Results/1772172361576_250050845%20%20-%20Pass.jpg) |

### ❌ Fail (Garbled Output)

| 1772172626834_250033423                                           | 1772172993599_250313201                                           | 1772173144558_250448540                                           |
| ----------------------------------------------------------------- | ----------------------------------------------------------------- | ----------------------------------------------------------------- |
| ![Fail 1](Print%20Results/1772172626834_250033423%20-%20Fail.jpg) | ![Fail 2](Print%20Results/1772172993599_250313201%20-%20Fail.jpg) | ![Fail 3](Print%20Results/1772173144558_250448540%20-%20Fail.jpg) |

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

### Step 6: Reproduce the Issues

1. Enter the printer's **Bluetooth MAC address** (e.g. `00:11:22:33:44:55`).
2. Tap **Check Status** to verify the connection — status should show "Connected OK".

**Garbled output (Problem 1):**

3. Select a **Fail** PDF from the dropdown and tap **Print Selected** to see the garbled output.
4. Select a **Pass** PDF and tap **Print Selected** to see correct output for comparison.
5. Tap **Print All** to print all PDFs in sequence.

**Blank page (Problem 2):**

6. Select the **Blank** PDF (`1778666792328_260680042 - Blank.pdf`) from the dropdown.
7. Tap **Print Selected** once — observe that **no paper is fed** from the printer.
8. Tap **Print Selected** again — observe that the printer feeds a **completely blank** page.

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

## Printer Configuration

The printer configuration file used during testing is included in the project root:

```
TSC_Alpha_40L_Configuration.dcf
```

This `.dcf` file contains the TSC Alpha-40L settings used to reproduce the issue.

## Project Structure

```
PrintingGarbled/
├── TSC_Alpha_40L_Configuration.dcf  ← Printer configuration
├── Print Results/                   ← Photos of actual printed output
│   ├── *Pass.jpg                    ← Correct prints
│   └── *Fail.jpg                    ← Garbled prints
├── app/
│   ├── libs/
│   │   └── tscsdk.jar              ← TSC Printer SDK
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── *Pass.pdf            ← PDFs that print correctly
│       │   ├── *Fail.pdf            ← PDFs that print garbled
│       │   └── *Blank.pdf           ← PDFs that print blank (Problem 2)
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
- **Printer Firmware:** B1.17 (same version used in production / in the field)
- **Printer Config:** `TSC_Alpha_40L_Configuration.dcf`
- **Connection:** Bluetooth Classic
