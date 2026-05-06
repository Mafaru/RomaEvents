package com.romaevents.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var searchButton: Button
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var cameraExecutor: ExecutorService

    private val repository = EventRepository()
    private var lastDetectedText: String = ""

    private val CAMERA_PERMISSION_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF7F7F7.toInt())
        }

        previewView = PreviewView(this)

        resultText = TextView(this).apply {
            text = "Punta la camera su una locandina..."
            textSize = 15f
            setTextColor(0xFF1B1B1B.toInt())
            setPadding(24, 20, 24, 20)
            maxLines = 4
        }

        searchButton = Button(this).apply {
            text = "Cerca evento"
            setOnClickListener {
                searchEventFromOcr()
            }
        }

        resultsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ScannerActivity)
        }

        root.addView(
            previewView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.2f
            )
        )

        root.addView(resultText)

        root.addView(
            searchButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 0, 24, 12)
            }
        )

        root.addView(
            resultsRecyclerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor, TextAnalyzer { text ->
                if (text.isNotBlank() && !text.startsWith("Errore OCR")) {
                    lastDetectedText = text
                }

                runOnUiThread {
                    resultText.text =
                        if (text.isBlank()) {
                            "Nessun testo rilevato..."
                        } else {
                            text
                        }
                }
            })

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun searchEventFromOcr() {
        val query = cleanOcrText(lastDetectedText)

        if (query.length < 3) {
            Toast.makeText(
                this,
                "Testo OCR non sufficiente per cercare",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        resultText.text = "Cerco eventi collegati a:\n$query"

        lifecycleScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchEvents(query)
                }

                if (results.isEmpty()) {
                    Toast.makeText(
                        this@ScannerActivity,
                        "Nessun evento trovato",
                        Toast.LENGTH_LONG
                    ).show()

                    resultsRecyclerView.adapter = null
                } else {
                    resultsRecyclerView.adapter = EventAdapter(results) { event ->
                        val intent = Intent(this@ScannerActivity, MainActivity::class.java).apply {
                            putExtra("open_event_id", event.id)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }

                        startActivity(intent)
                        finish()
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ScannerActivity,
                    "Errore ricerca: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun cleanOcrText(text: String): String {
        val stopWords = setOf(
            "roma", "evento", "eventi", "presso", "dalle", "della",
            "dello", "delle", "alla", "alle", "con", "per", "dal",
            "del", "via", "ore", "ingresso", "gratuito"
        )

        return text
            .lowercase()
            .replace("\n", " ")
            .replace(Regex("[^a-zà-ù0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .filter { word ->
                word.length >= 4 && word !in stopWords
            }
            .distinct()
            .take(8)
            .joinToString(" ")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }
}