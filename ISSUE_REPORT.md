# Printing Issue Report — TSC Alpha-40L

## Summary

We are experiencing a **garbled/corrupted printing** issue when printing PDF documents to the **TSC Alpha-40L** printer via Bluetooth. Some PDF files print correctly while others produce unreadable output.

## Printer & Connection Details

| Item               | Detail                                                       |
| ------------------ | ------------------------------------------------------------ |
| **Printer Model**  | TSC Alpha-40L                                                |
| **Connection**     | Bluetooth                                                    |
| **Software**       | TSC Printer SDK (provided by TSC)                            |
| **Printer Config** | `TSC_Alpha_40L_Configuration.dcf` (included in project root) |

## What is Happening

When our application sends PDF files to the TSC Alpha-40L printer:

- **Some PDFs print correctly** — the output is clear and matches the original document.
- **Some PDFs print garbled** — the printed output is corrupted, unreadable, or contains random characters/symbols instead of the expected content.

This issue is **not caused by the PDF files themselves** — the same files display correctly on screen and can be printed normally from other printers.

## Sample Files for Reproduction

The test project includes 6 sample PDF files to help reproduce the issue:

| #   | File                    | Expected Result       |
| --- | ----------------------- | --------------------- |
| 1   | 1772170565703_250646575 | ✅ Prints correctly   |
| 2   | 1772172257369_250846099 | ✅ Prints correctly   |
| 3   | 1772172361576_250050845 | ✅ Prints correctly   |
| 4   | 1772172626834_250033423 | ❌ **Garbled output** |
| 5   | 1772172993599_250313201 | ❌ **Garbled output** |
| 6   | 1772173144558_250448540 | ❌ **Garbled output** |

These are example files provided to help TSC's development team reproduce the problem. The actual issue occurs across various PDF documents in production.

## What We Have Done

1. Verified that the PDF files are valid and display correctly on screen.
2. Confirmed the Bluetooth connection to the printer is stable (status check passes).
3. Built a **standalone test application** to isolate the issue and rule out our main app's code.
4. Shared the test project with TSC's development team for investigation.

**Test project repository:** https://github.com/bafs-id/PrintingGarbled.git

## What We Need from TSC

We need TSC to investigate and provide answers to the following:

1. **Fix or Workaround** — How can we resolve this issue? Is there an updated SDK, a configuration change, or a workaround available?

We have provided:

- A ready-to-run test application that reproduces the problem.
- The exact PDF files that fail and succeed.
- The printer configuration file (`TSC_Alpha_40L_Configuration.dcf`) used during testing.

We are waiting for TSC to investigate and respond.
