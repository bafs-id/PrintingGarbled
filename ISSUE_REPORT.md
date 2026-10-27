# Printing Issue Report — Garbled Output with the Updated SDK (TSC Alpha-40L)

## About Us

**Bangkok Aviation Fuel Services PCL (BAFS)** provides aircraft fueling services at airports. After each fueling operation is completed, our IRIS mobile application generates a ticket (PDF) that must be printed on-site using a portable printer. Reliable and accurate ticket printing is essential to our daily operations.

This report is submitted by the **BAFS software engineering team** responsible for developing the mobile application used in the field.

## Summary

Earlier this year we reported a garbled-output issue when printing PDF tickets to the **TSC Alpha-40L** over Bluetooth. TSC sent us an updated SDK, and all three failing sample tickets from that report now print correctly. Thank you for the fix.

However, we have found **new tickets that still print garbled with the updated SDK**. We added two of them to this test project. Both reproduce in the test app, so the issue is not specific to our production app.

## Background

| Date                   | Event                                                                                                                              |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Feb 2026               | Garbled-output issue reported with 3 failing sample tickets — [archived/ISSUE_REPORT_GARBLED.md](archived/ISSUE_REPORT_GARBLED.md) |
| May 2026               | Separate blank-page issue reported — [archived/ISSUE_REPORT_BLANK_PAGE.md](archived/ISSUE_REPORT_BLANK_PAGE.md)                    |
| 2026-05-29 (SDK build) | Updated `tscsdk.jar` from TSC. The 3 failing samples now print correctly, and the blank-page issue has not been observed since.    |
| Oct 2026               | New tickets found that print garbled with the updated SDK (this report).                                                          |

The previous reports, sample PDFs and photos are kept in the [archived/](archived/) folder.

## Printer & Connection Details

| Item               | Detail                                                                        |
| ------------------ | ----------------------------------------------------------------------------- |
| **Printer Model**  | TSC Alpha-40L                                                                 |
| **Firmware**       | B1.17 (same version used in production / in the field)                        |
| **Connection**     | Bluetooth                                                                     |
| **Software**       | TSC Printer SDK `tscsdk.jar` — updated build (`TSCActivity` dated 2026-05-29) |
| **Printer Config** | `TSC_Alpha_40L_Configuration.dcf` (included in project root)                  |

## Printing Flow in the Test App

The test app uses the updated SDK with these calls (see `MainActivity.kt`):

```
setWriteSpeedKBPerSecond(10 * 1024) → Limit Bluetooth write speed (set once on start)
openport(bluetoothAddress)          → Connect via Bluetooth
printPDFbyFile(file, 0, 0, 203)     → Print the PDF (copy 1)
printlabel(1, 5)                    → Reprint the buffered label (copies 2–6)
closeport(3000)                     → Disconnect
```

**Print Selected** prints 6 copies of one ticket, the same as in production. **Print All** prints one copy of every PDF in `assets/`.

## What Is Happening

Two tickets printed in production on 25 Oct 2026 come out garbled. Both are included in `app/src/main/assets/`:

| File                                 | Result with updated SDK |
| ------------------------------------ | ----------------------- |
| `1793003002756_260883602 - Fail.pdf` | ❌ **Garbled output**   |
| `1793003132371_260883600 - Fail.pdf` | ❌ **Garbled output**   |

- **Almost every attempt is garbled.** Printing the same file again almost always gives garbled output again.
- **The top of each ticket prints correctly.** On both tickets, the logo, the address and the Airport / Ticket No. line come out fine. The output becomes garbled from the Date line down to the bottom of the ticket (see [Print Results](#print-results)).
- **All six copies are garbled.** Copy 1 is printed with `printPDFbyFile()`, and copies 2–6 with `printlabel(1, 5)`. In the updated SDK, `printlabel()` only sends `PRINT 1, 5` and does not resend any image data, so copies 2–6 are reprints of the image already stored in the printer. Because those copies are garbled too, the image data is already corrupted by the time `printPDFbyFile()` returns. The corruption happens while the PDF is converted or transferred, not when the printer prints it.
- **The PDFs themselves are fine.** Both files display correctly on screen.

## Print Results

Photos of the garbled output are in the `Print Results/` folder:

| 1793003002756_260883602                                                     | 1793003132371_260883600                                                     |
| --------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| ![260883602 garbled](Print%20Results/1793003002756_260883602%20-%20Fail.jpg) | ![260883600 garbled](Print%20Results/1793003132371_260883600%20-%20Fail.jpg) |

## Steps to Reproduce

1. Open the project in **Android Studio** and install it on a physical Android device (API 24+) with Bluetooth, or run `./gradlew installDebug`.
2. Pair the **TSC Alpha-40L** printer with the device over Bluetooth.
3. Open the app. The MAC address is filled in from the paired printer (or tap **Get Paired Printer MAC**). Tap **Check Status** and confirm it reports "Connected OK".
4. Select `1793003002756_260883602 - Fail.pdf` and tap **Print Selected**. The printer prints 6 copies, and all of them are garbled.
5. Repeat with `1793003132371_260883600 - Fail.pdf`.

## What We Need from TSC

1. **Root cause.** What condition in a PDF, or in the data transfer, causes the garbled output? We need to know which tickets are at risk, not only whether these sample files print.
2. **A fix that covers all tickets.** Please verify the next fix against the two files above and against the earlier samples in [archived/assets/](archived/assets/), so that the tickets fixed last time still print correctly. To print the earlier samples from the test app, copy them into `app/src/main/assets/`.

**Test project repository:** https://github.com/bafs-id/PrintingGarbled.git

We are waiting for TSC to investigate and respond.
