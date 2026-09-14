package com.clmbs.en_ua_vocab.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clmbs.en_ua_vocab.R
import com.clmbs.en_ua_vocab.data.Word

class LockScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupLockScreenFlags()
        setContentView(R.layout.activity_lock_screen)

        val word = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("WORD_EXTRA", Word::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("WORD_EXTRA") as? Word
        }

        word?.let {
            findViewById<TextView>(R.id.tvWord).text = it.word
            findViewById<TextView>(R.id.tvTranslation).text = it.translation
            findViewById<TextView>(R.id.tvDetails).text = "${it.pos} | ${it.level.uppercase()}"
        }

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            finish()
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
