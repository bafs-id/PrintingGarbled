package com.example.printinggarbled

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.graphics.createBitmap
import com.example.tscdll.TSCActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private val tscPrinter = TSCActivity()
    private lateinit var etBluetoothAddress: EditText
    private lateinit var btnGetMac: Button
    private lateinit var btnCheckStatus: Button
    private lateinit var btnPrint: Button
    private lateinit var spinnerPdf: Spinner
    private lateinit var btnPrintSelected: Button
    private lateinit var tvStatus: TextView

    companion object {
        private const val TAG = "PrintingGarbled"
        private const val PRINT_COPIES = 6

        // One request code per action, so the action runs once the permission is granted
        private const val REQUEST_LOAD_MAC = 1001
        private const val REQUEST_CHECK_STATUS = 1002
        private const val REQUEST_PRINT_SELECTED = 1003
        private const val REQUEST_PRINT_ALL = 1004

        // Fix for the garbled tickets: limit the Bluetooth write speed.
        // setWriteSpeedKBPerSecond() multiplies by 1024 itself, so the old value 10 * 1024
        // meant ~10 MB/s, which is effectively no limit.
        private const val WRITE_SPEED_KB_PER_SECOND = 5

        // Segmented transmission suggested by TSC (Benson Liao, 28 Oct 2026). A ticket is about
        // 32 KB after compression, so 65,535-byte segments would send it in one piece.
        private const val SEGMENT_SIZE = 1024
        private const val SEGMENT_DELAY_MS = 15L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBluetoothAddress = findViewById(R.id.etBluetoothAddress)
        btnGetMac = findViewById(R.id.btnGetMac)
        btnCheckStatus = findViewById(R.id.btnCheckStatus)
        btnPrint = findViewById(R.id.btnPrint)
        spinnerPdf = findViewById(R.id.spinnerPdf)
        btnPrintSelected = findViewById(R.id.btnPrintSelected)
        tvStatus = findViewById(R.id.tvStatus)

        tscPrinter.setWriteSpeedKBPerSecond(WRITE_SPEED_KB_PER_SECOND)

        // Populate spinner with PDF files from assets
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listPdfAssets())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPdf.adapter = adapter

        btnGetMac.setOnClickListener { runWithBluetoothPermission(REQUEST_LOAD_MAC) }
        btnCheckStatus.setOnClickListener { runWithBluetoothPermission(REQUEST_CHECK_STATUS) }
        btnPrintSelected.setOnClickListener { runWithBluetoothPermission(REQUEST_PRINT_SELECTED) }
        btnPrint.setOnClickListener { runWithBluetoothPermission(REQUEST_PRINT_ALL) }

        // Auto pre-fill MAC from the paired printer on first launch
        if (savedInstanceState == null) {
            runWithBluetoothPermission(REQUEST_LOAD_MAC)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            runAction(requestCode)
        } else {
            setStatus("Bluetooth permission denied")
        }
    }

    private fun runWithBluetoothPermission(requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                requestCode
            )
            return
        }
        runAction(requestCode)
    }

    private fun runAction(requestCode: Int) {
        when (requestCode) {
            REQUEST_LOAD_MAC -> getPairedPrinterMac()
            REQUEST_CHECK_STATUS -> checkPrinterStatus()
            REQUEST_PRINT_SELECTED -> printSelected()
            REQUEST_PRINT_ALL -> printAll()
        }
    }

    // Prefer devices reporting the Imaging (printer) class, or a TSC/printer-like name
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun isLikelyPrinter(device: BluetoothDevice): Boolean {
        if (device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.IMAGING) return true
        val name = device.name?.lowercase() ?: return false
        return "tsc" in name || "alpha" in name || "printer" in name
    }

    private fun getPairedPrinterMac() {
        try {
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null) {
                setStatus("Bluetooth not supported on this device")
                return
            }
            if (!adapter.isEnabled) {
                setStatus("Please enable Bluetooth")
                return
            }

            val bondedDevices = adapter.bondedDevices.orEmpty()
            val device = bondedDevices.firstOrNull { isLikelyPrinter(it) } ?: bondedDevices.firstOrNull()
            if (device == null) {
                setStatus("No paired devices found")
                return
            }

            etBluetoothAddress.setText(device.address)
            setStatus("Loaded MAC from ${device.name}: ${device.address}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to read paired devices", e)
            setStatus("Error - ${e.message}")
        }
    }

    private fun checkPrinterStatus() {
        val address = printerAddress() ?: return
        setStatus("Connecting...")

        withPrinter(address) {
            setStatus("Connected. Querying status...")
            "Connected OK | Printer status: ${tscPrinter.printerstatus(1000)}"
        }
    }

    private fun printSelected() {
        val address = printerAddress() ?: return
        val pdfName = spinnerPdf.selectedItem?.toString()
        if (pdfName.isNullOrEmpty()) {
            setStatus("No PDF selected")
            return
        }
        setStatus("Printing $pdfName...")

        withPrinter(address) {
            // Print copy 1, then reprint the buffered label for the remaining copies
            val printResult = printPdfSegmented(copyAssetToCache(pdfName), 0, 0, 203)
            if (printResult != "1") return@withPrinter "Print failed (result: $printResult)"

            val copiesResult = tscPrinter.printlabel(1, PRINT_COPIES - 1)
            if (copiesResult != "1") {
                "Printed 1 copy, extra copies failed (result: $copiesResult)"
            } else {
                "Printed $PRINT_COPIES copies of $pdfName successfully"
            }
        }
    }

    private fun printAll() {
        val address = printerAddress() ?: return
        val pdfNames = listPdfAssets()
        if (pdfNames.isEmpty()) {
            setStatus("No PDF files found in assets")
            return
        }
        setStatus("Printing ${pdfNames.size} PDF(s)...")

        withPrinter(address) {
            val failed = mutableListOf<String>()
            for ((index, pdfName) in pdfNames.withIndex()) {
                setStatus("Printing ${index + 1}/${pdfNames.size}: $pdfName")
                if (printPdfSegmented(copyAssetToCache(pdfName), 0, 0, 203) != "1") failed += pdfName
            }
            Thread.sleep(3000)

            if (failed.isEmpty()) {
                "Done. Printed ${pdfNames.size} PDF(s)"
            } else {
                "Done. Failed: ${failed.joinToString()}"
            }
        }
    }

    // Connects to the printer on a background thread, runs block, closes the port and shows
    // the status message that block returns
    private fun withPrinter(address: String, block: () -> String) {
        setButtonsEnabled(false)
        Thread {
            try {
                val connectResult = tscPrinter.openport(address)
                if (connectResult != "1") {
                    setStatus("Failed to connect to printer (result: $connectResult)")
                    return@Thread
                }
                val status = try {
                    block()
                } finally {
                    tscPrinter.closeport(3000)
                }
                setStatus(status)
            } catch (e: Exception) {
                Log.e(TAG, "Printer error", e)
                setStatus("Error - ${e.message}")
            } finally {
                runOnUiThread { setButtonsEnabled(true) }
            }
        }.start()
    }

    // Same as TSCActivity.printPDFbyFile() in the updated SDK (render each page, then send
    // CLS + BITMAP + PRINT 1), except that the data is sent in segments
    private fun printPdfSegmented(file: File, x: Int, y: Int, dpi: Int): String {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            for (pageIndex in 0 until renderer.pageCount) {
                val bitmap = renderer.openPage(pageIndex).use { page ->
                    val width = page.width * dpi / 72
                    val height = page.height * dpi / 72
                    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, Rect(0, 0, width, height), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }

                // getSendBitmapBytes() builds the LZSS-compressed bitmap command, as printPDFbyFile() does
                val data = "CLS\r\n".toByteArray(Charsets.US_ASCII) +
                    tscPrinter.getSendBitmapBytes(x, y, bitmap) +
                    "PRINT 1\r\n".toByteArray(Charsets.US_ASCII)
                bitmap.recycle()

                if (sendSegmented(data) != "1") return "-1"
            }
        }
        return "1"
    }

    private fun sendSegmented(data: ByteArray): String {
        for (offset in data.indices step SEGMENT_SIZE) {
            val segment = data.copyOfRange(offset, minOf(offset + SEGMENT_SIZE, data.size))
            // sendcommand() writes the segment at the speed set by setWriteSpeedKBPerSecond() and flushes
            if (tscPrinter.sendcommand(segment) != "1") return "-1"

            // Allow time for buffering and processing by the printer's Bluetooth chip
            Thread.sleep(SEGMENT_DELAY_MS)
        }
        Log.d(TAG, "Sent ${data.size} bytes in ${(data.size + SEGMENT_SIZE - 1) / SEGMENT_SIZE} segment(s)")
        return "1"
    }

    private fun printerAddress(): String? {
        val address = etBluetoothAddress.text.toString().trim()
        if (address.isEmpty()) {
            setStatus("Please enter Bluetooth MAC address")
            return null
        }
        return address
    }

    private fun listPdfAssets(): List<String> =
        assets.list("")?.filter { it.lowercase().endsWith(".pdf") } ?: emptyList()

    private fun copyAssetToCache(name: String): File {
        val file = File(cacheDir, name)
        assets.open(name).use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnCheckStatus.isEnabled = enabled
        btnPrintSelected.isEnabled = enabled
        btnPrint.isEnabled = enabled
    }

    // Safe to call from any thread
    private fun setStatus(message: String) {
        runOnUiThread { tvStatus.text = "Status: $message" }
    }
}
