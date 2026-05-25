package com.romaevents.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.romaevents.app.R
import com.romaevents.app.data.repository.EventRepository
import com.romaevents.app.ui.events.EventAdapter
import com.romaevents.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var searchButton: MaterialButton
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var cameraExecutor: ExecutorService

    private val repository = EventRepository()
    private var lastDetectedText: String = ""

    private val CAMERA_PERMISSION_CODE = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val orange = ContextCompat.getColor(this, R.color.roma_orange)
        val bgDark = ContextCompat.getColor(this, R.color.background_dark)
        val black = ContextCompat.getColor(this, R.color.black)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgDark)
        }

        // 🔹 SEZIONE CAMERA
        val cameraContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.4f
            )
        }

        previewView = PreviewView(this)
        cameraContainer.addView(previewView)

        // Mirino Scanner
        val frameView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(650, 450, Gravity.CENTER)
            background = android.graphics.drawable.GradientDrawable().apply {
                setStroke(6, orange)
                cornerRadius = 48f
            }
        }
        cameraContainer.addView(frameView)

        root.addView(cameraContainer)

        // 🔹 PANNELLO AZIONI
        val infoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 40)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@ScannerActivity, R.color.surface_dark))
                cornerRadii = floatArrayOf(80f, 80f, 80f, 80f, 0f, 0f, 0f, 0f)
            }
        }

        infoPanel.addView(TextView(this).apply {
            text = "INQUADRA IL TITOLO E PREMI CERCA"
            textSize = 12f
            setTextColor(orange)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
            setPadding(0, 0, 0, 32)
        })

        searchButton = MaterialButton(this).apply {
            text = "CERCA EVENTO"
            textSize = 16f
            cornerRadius = 40
            backgroundTintList = ColorStateList.valueOf(orange)
            setTextColor(black)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 36, 0, 36)
            elevation = 15f
            setOnClickListener {
                searchEventFromOcr()
            }
        }

        infoPanel.addView(searchButton)
        root.addView(infoPanel)

        // 🔹 SEZIONE RISULTATI
        resultsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ScannerActivity)
            setPadding(24, 24, 24, 60)
            clipToPadding = false
        }

        root.addView(
            resultsRecyclerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.8f
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

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, TextAnalyzer { text ->
                if (text.isNotBlank() && !text.startsWith("Errore OCR")) {
                    lastDetectedText = text
                }
            })

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Errore Camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun searchEventFromOcr() {
        val query = cleanOcrText(lastDetectedText)

        if (query.length < 3) {
            Toast.makeText(this, "Punta il titolo dell'evento", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchEvents(query)
                }

                if (results.isEmpty()) {
                    Toast.makeText(this@ScannerActivity, "Nessun evento trovato", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@ScannerActivity, "Errore durante la ricerca", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cleanOcrText(text: String): String {
        return text.lowercase().replace("\n", " ").trim().take(100)
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
        if (requestCode == CAMERA_PERMISSION_CODE && allPermissionsGranted()) {
            startCamera()
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            finish()
        }
    }
}
