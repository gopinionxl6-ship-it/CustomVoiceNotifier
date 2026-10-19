package com.example.simplecustomvoice

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.io.File

class SimpleVoiceListener : NotificationListenerService() {

    private var player: MediaPlayer? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val prefs = getSharedPreferences("voice_app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("is_suspended", false)) return

        val pkgName = sbn.packageName
        val monitoredApp = prefs.getString("target_package", "") ?: ""

        // 1. Target App Filter
        if (monitoredApp.isNotEmpty() && pkgName != monitoredApp) return

        // 2. Active/Foreground Only Filter
        val onlyWhenActive = prefs.getBoolean("only_when_active", false)
        if (onlyWhenActive && !isAppInForeground(pkgName)) return

        // 3. Screen State Filter
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive
        if (isScreenOn && !prefs.getBoolean("speak_screen_on", true)) return
        if (!isScreenOn && !prefs.getBoolean("speak_screen_off", true)) return

        // 4. Keyword Require Filter
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val combinedText = "$title $text"
        val requiredText = prefs.getString("require_text", "") ?: ""
        if (requiredText.isNotEmpty() && !combinedText.contains(requiredText, ignoreCase = true)) return

        playOfflineVoice()
    }

    private fun isAppInForeground(targetPkg: String): Boolean {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 15000, now)
        val event = UsageEvents.Event()
        var currentFg = ""
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentFg = event.packageName
            }
        }
        return currentFg == targetPkg
    }

    private fun playOfflineVoice() {
        try {
            val voiceFile = File(filesDir, "custom_voice.mp3")
            if (!voiceFile.exists()) return

            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(this@SimpleVoiceListener, Uri.fromFile(voiceFile))
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
