# Printing Issue Report (Blank Page) — TSC Alpha-40L

## About Us

**Bangkok Aviation Fuel Services PCL (BAFS)** provides aircraft fueling services at airports. After each fueling operation is completed, our IRIS mobile application generates a ticket (PDF) that must be printed on-site using a portable printer. Reliable and accurate ticket printing is essential to our daily operations.

This report is submitted by the **BAFS software engineering team** responsible for developing the mobile application used in the field.

> **Note:** This is a **second, separate issue** we have encountered on the same printer/SDK. The previously reported _garbled output_ issue is still under investigation by TSC (see `ISSUE_REPORT_GARBLED.md`). The symptom described below is different and reproduces with a different PDF.

## Summary

We are experiencing a **blank-page printing** issue when printing a PDF ticket to the **TSC Alpha-40L** printer via Bluetooth. The same SDK call (`TSCActivity.printPDFbyFile()`) that prints other PDFs correctly fails for this document in a very specific **two-tap pattern** (the user must tap the Print button **twice** — described below).

**This pattern is 100% reproducible** for the affected PDF — it happens every single time on every test run, not occasionally. This directly impacts our field operations because the ticket never actually reaches paper on the first tap, and the second tap produces an unusable blank page.

## Printer & Connection Details

| Item               | Detail                                                       |
| ------------------ | ------------------------------------------------------------ |
| **Printer Model**  | TSC Alpha-40L                                                |
| **Firmware**       | B1.17 (same version used in production / in the field)       |
| **Connection**     | Bluetooth                                                    |
| **Software**       | TSC Printer SDK (provided by TSC)                            |
| **Printer Config** | `TSC_Alpha_40L_Configuration.dcf` (included in project root) |

## What is Happening

When our application sends the affected PDF to the TSC Alpha-40L printer, the failure happens in **two distinct user actions** (two manual taps of the Print button):

1. **The user taps Print (1st time) — nothing happens.**
   The SDK call returns, but the printer does **not** feed any paper. No output. No error visible to the user. From the printer's perspective it appears as if no job was received.

2. **The user taps Print (2nd time) — a blank page is fed.**
   On the second tap for the **same PDF**, the printer **does** feed paper this time, but the page comes out **completely blank** (empty white page, no content at all).

**This pattern is fully consistent — it happens every time we tap Print for this PDF. The first tap never prints, the second tap always feeds a blank page. It does not work on the first tap, ever.**

**This issue reproduces on every TSC Alpha-40L printer unit we have tested** — it is not a single faulty hardware unit. We have tried multiple physical TSC Alpha-40L printers and every one exhibits the exact same two-tap, blank-page behavior for this PDF. This strongly suggests the root cause is in the SDK or firmware, not the hardware.

The Bluetooth connection check (`Check Status` in the test app) succeeds before each tap, so the connection itself is not the problem.

## Sample File for Reproduction

The test project includes the sample PDF that reproduces this issue:

| File                                                      | Expected Result           |
| --------------------------------------------------------- | ------------------------- |
| `app/src/main/assets/1778666792328_260680042 - Blank.pdf` | ⚠️ Blank page (see above) |

## Steps to Reproduce

1. Build and install the test app on a physical Android device.
2. Pair the **TSC Alpha-40L** printer with the device over Bluetooth.
3. Open the app, enter the printer's Bluetooth MAC address, and tap **Check Status** — confirm it reports "Connected OK".
4. In the spinner, select `1778666792328_260680042 - Blank.pdf`.
5. Tap **Print Selected** (1st tap) — **always** observe that no paper is fed from the printer.
6. Tap **Print Selected** again (2nd tap) — **always** observe that the printer feeds a page, but the page is **completely blank**.

> This sequence reproduces 100% of the time for this PDF. We have never observed the first tap printing successfully, and we have never observed the second tap printing the actual content.

## What We Have Done

1. Verified that the PDF file is valid and displays correctly on screen.
2. Confirmed the Bluetooth connection to the printer is stable (`Check Status` passes).
3. Confirmed the same SDK call (`printPDFbyFile`) prints other PDFs from `assets/` correctly, so the failure is specific to this document / code path.
4. **Reproduced the issue on multiple TSC Alpha-40L printer units** — every unit we tested shows the same two-tap, blank-page behavior for this PDF. This rules out a single faulty hardware unit.
5. Added the reproducing PDF to the existing **PrintingGarbled** test project so TSC can reproduce both issues from a single repository.

**Test project repository:** https://github.com/bafs-id/PrintingGarbled.git

## What We Need from TSC

1. **Fix or Workaround** — How can we resolve this issue? Is there an updated SDK, a configuration change, or a workaround available?
2. **Root-cause assessment** — Identification of where in the SDK or firmware this two-tap, blank-page behavior originates.

We have provided:

- A ready-to-run test application that reproduces the problem.
- The exact PDF file that triggers the blank-page behavior.
- The printer configuration file (`TSC_Alpha_40L_Configuration.dcf`) used during testing.

We are waiting for TSC to investigate and respond.
