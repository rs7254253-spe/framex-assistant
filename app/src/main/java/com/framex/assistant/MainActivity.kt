package com.framex.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.framex.assistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var serviceRunning = false

    private val permissionsNeeded = mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                startAssistantService()
            } else {
                binding.statusText.text = "Mic permission needed to run the assistant."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toggleButton.setOnClickListener {
            if (serviceRunning) {
                stopAssistantService()
            } else {
                ensurePermissionsThenStart()
            }
        }
    }

    private fun ensurePermissionsThenStart() {
        val missing = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startAssistantService()
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startAssistantService() {
        val intent = Intent(this, WakeWordService::class.java)
        ContextCompat.startForegroundService(this, intent)
        serviceRunning = true
        binding.toggleButton.text = "Stop Assistant"
        binding.statusText.text = "Running — say \"Jarvis\" anytime to talk."
    }

    private fun stopAssistantService() {
        stopService(Intent(this, WakeWordService::class.java))
        serviceRunning = false
        binding.toggleButton.text = "Start Assistant"
        binding.statusText.text = "Stopped."
    }
}
