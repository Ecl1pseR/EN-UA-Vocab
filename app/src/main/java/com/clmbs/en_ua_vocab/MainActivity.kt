package com.clmbs.en_ua_vocab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.clmbs.en_ua_vocab.service.VocabService
import com.clmbs.en_ua_vocab.ui.DictionaryActivity

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startVocabService()
        } else {
            Toast.makeText(this, "Дозвіл на сповіщення відхилено. Сервіс не може працювати.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            checkAndStartService()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopVocabService()
        }

        findViewById<Button>(R.id.btnDictionaries).setOnClickListener {
            startActivity(Intent(this, DictionaryActivity::class.java))
        }
    }

    private fun checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startVocabService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startVocabService()
        }
    }

    private fun startVocabService() {
        Toast.makeText(this, "Запуск сервісу...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, VocabService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVocabService() {
        Toast.makeText(this, "Зупинка сервісу...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, VocabService::class.java)
        stopService(intent)
    }
}
