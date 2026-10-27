# Printing Garbled

A minimal Android app that reproduces a **garbled-output** issue when printing PDF tickets with the TSC Printer SDK (`tscsdk.jar`) to a **TSC Alpha-40L** printer over Bluetooth.

The issue still occurs with the updated SDK from TSC. See **[ISSUE_REPORT.md](ISSUE_REPORT.md)** for the full report.

## Quick Start

You need Android Studio, a physical Android device (API 24+) with Bluetooth, and a TSC Alpha-40L printer paired with it.

1. Open the project in Android Studio and run it on the device, or run `./gradlew installDebug`.
2. Open the app. The paired printer's MAC address is filled in (or tap **Get Paired Printer MAC**). Tap **Check Status**.
3. Select a PDF and tap **Print Selected** (prints 6 copies), or tap **Print All** (prints one copy of each PDF).

## Contents

| Path                                                                 | Description                                                 |
| -------------------------------------------------------------------- | ----------------------------------------------------------- |
| [ISSUE_REPORT.md](ISSUE_REPORT.md)                                   | Current issue report                                        |
| [app/src/main/assets/](app/src/main/assets/)                         | Sample PDF tickets that print garbled                       |
| [Print Results/](Print%20Results/)                                   | Photos of the garbled output                                |
| [app/libs/tscsdk.jar](app/libs/tscsdk.jar)                           | TSC Printer SDK (updated build, 2026-05-29)                 |
| [TSC_Alpha_40L_Configuration.dcf](TSC_Alpha_40L_Configuration.dcf)   | Printer configuration used during testing                   |
| [archived/](archived/)                                               | Previous reports, sample PDFs and photos (Feb and May 2026) |

## Tech Stack

Kotlin 2.0.21 · AGP 8.11.2 · Gradle 8.13 · compileSdk 36 / minSdk 24 · TSC Alpha-40L, firmware B1.17 · Bluetooth Classic
