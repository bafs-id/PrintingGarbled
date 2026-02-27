package com.example.printinggarbled

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.example.tscdll.TSCActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private val tscPrinter = TSCActivity()
    private lateinit var etBluetoothAddress: EditText
    private lateinit var btnCheckStatus: Button
    private lateinit var btnPrint: Button
    private lateinit var spinnerPdf: Spinner
    private lateinit var btnPrintSelected: Button
    private lateinit var tvStatus: TextView

    companion object {
        private const val REQUEST_BT_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBluetoothAddress = findViewById(R.id.etBluetoothAddress)
        btnCheckStatus = findViewById(R.id.btnCheckStatus)
        btnPrint = findViewById(R.id.btnPrint)
        spinnerPdf = findViewById(R.id.spinnerPdf)
        btnPrintSelected = findViewById(R.id.btnPrintSelected)
        tvStatus = findViewById(R.id.tvStatus)

        // Populate spinner with PDF files from assets
        val pdfList = assets.list("")?.filter { it.lowercase().endsWith(".pdf") } ?: emptyList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pdfList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPdf.adapter = adapter

        btnCheckStatus.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ),
                        REQUEST_BT_PERMISSION
                    )
                    return@setOnClickListener
                }
            }
            checkPrinterStatus()
        }

        btnPrint.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ),
                        REQUEST_BT_PERMISSION
                    )
                    return@setOnClickListener
                }
            }
            startPrinting()
        }

        btnPrintSelected.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ),
                        REQUEST_BT_PERMISSION
                    )
                    return@setOnClickListener
                }
            }
            printSelected()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BT_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPrinting()
            } else {
                tvStatus.text = "Status: Bluetooth permission denied"
            }
        }
    }

    private fun checkPrinterStatus() {
        val address = etBluetoothAddress.text.toString().trim()
        if (address.isEmpty()) {
            tvStatus.text = "Status: Please enter Bluetooth MAC address"
            return
        }

        btnCheckStatus.isEnabled = false
        btnPrint.isEnabled = false
        tvStatus.text = "Status: Connecting..."

        Thread {
            try {
                val connectResult = tscPrinter.openport(address)
                if (connectResult != "1") {
                    runOnUiThread {
                        tvStatus.text = "Status: Failed to connect (result: $connectResult)"
                        btnCheckStatus.isEnabled = true
                        btnPrint.isEnabled = true
                    }
                    return@Thread
                }

                runOnUiThread { tvStatus.text = "Status: Connected. Querying status..." }

                val printerStatus = tscPrinter.printerstatus(1000)

                tscPrinter.closeport(500)

                runOnUiThread {
                    tvStatus.text = "Status: Connected OK | Printer status: $printerStatus"
                    btnCheckStatus.isEnabled = true
                    btnPrint.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = "Status: Error - ${e.message}"
                    btnCheckStatus.isEnabled = true
                    btnPrint.isEnabled = true
                }
            }
        }.start()
    }

    private fun printSelected() {
        val address = etBluetoothAddress.text.toString().trim()
        if (address.isEmpty()) {
            tvStatus.text = "Status: Please enter Bluetooth MAC address"
            return
        }

        val selectedPdf = spinnerPdf.selectedItem?.toString()
        if (selectedPdf.isNullOrEmpty()) {
            tvStatus.text = "Status: No PDF selected"
            return
        }

        btnPrintSelected.isEnabled = false
        btnPrint.isEnabled = false
        btnCheckStatus.isEnabled = false
        tvStatus.text = "Status: Printing $selectedPdf..."

        Thread {
            try {
                // Copy PDF from assets to cache
                val pdfFile = File(cacheDir, selectedPdf)
                assets.open(selectedPdf).use { input ->
                    FileOutputStream(pdfFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Connect to printer via Bluetooth
                val connectResult = tscPrinter.openport(address)
                if (connectResult != "1") {
                    runOnUiThread {
                        tvStatus.text = "Status: Failed to connect to printer"
                        btnPrintSelected.isEnabled = true
                        btnPrint.isEnabled = true
                        btnCheckStatus.isEnabled = true
                    }
                    return@Thread
                }

                // Print the PDF file
                val printResult = tscPrinter.printPDFbyFile(pdfFile, 0, 0, 200)

                // Close the connection
                tscPrinter.closeport(500)

                runOnUiThread {
                    if (printResult == "1") {
                        tvStatus.text = "Status: Printed $selectedPdf successfully"
                    } else {
                        tvStatus.text = "Status: Print failed (result: $printResult)"
                    }
                    btnPrintSelected.isEnabled = true
                    btnPrint.isEnabled = true
                    btnCheckStatus.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = "Status: Error - ${e.message}"
                    btnPrintSelected.isEnabled = true
                    btnPrint.isEnabled = true
                    btnCheckStatus.isEnabled = true
                }
            }
        }.start()
    }

    private fun startPrinting() {
        val address = etBluetoothAddress.text.toString().trim()
        if (address.isEmpty()) {
            tvStatus.text = "Status: Please enter Bluetooth MAC address"
            return
        }

        // Find all PDF files in assets
        val pdfFiles = assets.list("")?.filter { it.lowercase().endsWith(".pdf") } ?: emptyList()
        if (pdfFiles.isEmpty()) {
            tvStatus.text = "Status: No PDF files found in assets"
            return
        }

        btnPrint.isEnabled = false
        btnCheckStatus.isEnabled = false
        tvStatus.text = "Status: Printing ${pdfFiles.size} PDF(s)..."

        Thread {
            try {
                // Connect to printer via Bluetooth
                val connectResult = tscPrinter.openport(address)
                if (connectResult != "1") {
                    runOnUiThread {
                        tvStatus.text = "Status: Failed to connect to printer"
                        btnPrint.isEnabled = true
                        btnCheckStatus.isEnabled = true
                    }
                    return@Thread
                }

                for ((index, pdfName) in pdfFiles.withIndex()) {
                    runOnUiThread {
                        tvStatus.text = "Status: Printing ${index + 1}/${pdfFiles.size}: $pdfName"
                    }

                    // Copy PDF from assets to cache directory
                    val pdfFile = File(cacheDir, pdfName)
                    assets.open(pdfName).use { input ->
                        FileOutputStream(pdfFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Print the PDF file
                    val printResult = tscPrinter.printPDFbyFile(pdfFile, 0, 0, 200)
                    if (printResult != "1") {
                        runOnUiThread {
                            tvStatus.text = "Status: Failed printing $pdfName (result: $printResult)"
                        }
                    }
                }

                // Close the connection
                tscPrinter.closeport(500)

                runOnUiThread {
                    tvStatus.text = "Status: Done. Printed ${pdfFiles.size} PDF(s)"
                    btnPrint.isEnabled = true
                    btnCheckStatus.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = "Status: Error - ${e.message}"
                    btnPrint.isEnabled = true
                    btnCheckStatus.isEnabled = true
                }
            }
        }.start()
    }
}