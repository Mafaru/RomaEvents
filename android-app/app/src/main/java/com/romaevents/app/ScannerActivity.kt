package com.romaevents.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
import com.google.android.material.card.MaterialCardView
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

        // 🔹 CAMERA PREVIEW
        previewView = PreviewView(this)

        root.addView(
            previewView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.2f
            )
        )

        // 🔹 CARD OCR TEXT
        val ocrCard = MaterialCardView(this).apply {
            radius = 22f
            cardElevation = 3f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(24, 20, 24, 20)
        }

        resultText = TextView(this).apply {
            text = "Punta la camera su una locandina..."
            textSize = 15f
            setTextColor(0xFF1B1B1B.toInt())
            maxLines = 4
        }

        ocrCard.addView(resultText)

        root.addView(
            ocrCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 20, 24, 12)
            }
        )

        // 🔹 BUTTON
        searchButton = Button(this).apply {
            text = "🔍 Cerca evento"
            textSize = 16f
            setBackgroundColor(0xFF1565C0.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(20, 14, 20, 14)
            setOnClickListener {
                searchEventFromOcr()
            }
        }

        root.addView(
            searchButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 0, 24, 12)
            }
        )

        // 🔹 RISULTATI HEADER
        val resultsTitle = TextView(this).apply {
            text = "Risultati"
            textSize = 18f
            setTextColor(0xFF1B1B1B.toInt())
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 10, 24, 6)
        }

        root.addView(resultsTitle)

        // 🔹 LISTA RISULTATI
        resultsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ScannerActivity)
        }

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
                "Testo OCR non sufficiente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

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
                    "Errore: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun cleanOcrText(text: String): String {
        return text
            .lowercase()
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
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