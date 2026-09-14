package com.clmbs.en_ua_vocab.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.clmbs.en_ua_vocab.MainActivity
import com.clmbs.en_ua_vocab.R
import com.clmbs.en_ua_vocab.data.Word
import com.clmbs.en_ua_vocab.data.WordRepository
import com.clmbs.en_ua_vocab.ui.LockScreenActivity

class VocabService : Service() {

    private val CHANNEL_ID = "VocabServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var repository: WordRepository

    companion object {
        const val ACTION_NEXT = "com.clmbs.en_ua_vocab.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.clmbs.en_ua_vocab.ACTION_PREVIOUS"
        const val ACTION_RELOAD = "com.clmbs.en_ua_vocab.ACTION_RELOAD"
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                updateWordAndNotify(repository.getRandomWord())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = WordRepository(this)
        val count = repository.getWordCount()
        Toast.makeText(this, "Завантажено $count слів", Toast.LENGTH_SHORT).show()
        
        createNotificationChannel()
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NEXT -> updateWordAndNotify(repository.getNextWord())
            ACTION_PREVIOUS -> updateWordAndNotify(repository.getPreviousWord())
            ACTION_RELOAD -> {
                repository.loadWords()
                val word = repository.getCurrentWord()
                updateWordAndNotify(word)
            }
            else -> {
                val word = repository.getCurrentWord()
                val notification = createWordNotification(word)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun updateWordAndNotify(word: Word?) {
        if (word == null) return
        
        val notification = createWordNotification(word)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        showWordOnLockScreen(word)
    }

    private fun showWordOnLockScreen(word: Word) {
        val fullScreenIntent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("WORD_EXTRA", word)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = createWordNotification(word, fullScreenPendingIntent)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createWordNotification(word: Word?, fullScreenIntent: PendingIntent? = null): Notification {
        val title = if (word != null) "${word.word} - ${word.translation}" else "EN-UA Vocab"
        val text = if (word != null) "${word.pos} | ${word.level.uppercase()}" else "Словник не завантажено"

        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, VocabService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, VocabService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_skip_previous, "Назад", prevPendingIntent)
            .addAction(R.drawable.ic_skip_next, "Вперед", nextPendingIntent)
            .setStyle(MediaStyle()
                .setShowActionsInCompactView(0, 1)
                .setMediaSession(null)) // null for simple style without session

        if (fullScreenIntent != null) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Vocab Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
