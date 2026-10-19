package com.example.simplecustomvoice

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvVoiceStatus: TextView

    private val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveVoiceFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("voice_app_prefs", MODE_PRIVATE)

        tvVoiceStatus = findViewById(R.id.tvVoiceStatus)
        val editPackage = findViewById<EditText>(R.id.editTargetPackage)
        val editKeyword = findViewById<EditText>(R.id.editRequireText)
        val chkActiveOnly = findViewById<CheckBox>(R.id.chkActiveOnly)
        val chkScreenOn = findViewById<CheckBox>(R.id.chkScreenOn)
        val chkScreenOff = findViewById<CheckBox>(R.id.chkScreenOff)
        val chkSuspend = findViewById<CheckBox>(R.id.chkSuspend)

        updateVoiceStatus()
        editPackage.setText(prefs.getString("target_package", ""))
        editKeyword.setText(prefs.getString("require_text", ""))
        chkActiveOnly.isChecked = prefs.getBoolean("only_when_active", false)
        chkScreenOn.isChecked = prefs.getBoolean("speak_screen_on", true)
        chkScreenOff.isChecked = prefs.getBoolean("speak_screen_off", true)
        chkSuspend.isChecked = prefs.getBoolean("is_suspended", false)

        findViewById<Button>(R.id.btnGrantNotif).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnGrantUsage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        findViewById<Button>(R.id.btnSelectVoice).setOnClickListener {
            audioPicker.launch("audio/*")
        }

        findViewById<Button>(R.id.btnTestVoice).setOnClickListener {
            val voiceFile = File(filesDir, "custom_voice.mp3")
            if (voiceFile.exists()) {
                MediaPlayer.create(this, Uri.fromFile(voiceFile))?.start()
            } else {
                Toast.makeText(this, "Please select a custom voice file first!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            prefs.edit()
                .putString("target_package", editPackage.text.toString().trim())
                .putString("require_text", editKeyword.text.toString().trim())
                .putBoolean("only_when_active", chkActiveOnly.isChecked)
                .putBoolean("speak_screen_on", chkScreenOn.isChecked)
                .putBoolean("speak_screen_off", chkScreenOff.isChecked)
                .putBoolean("is_suspended", chkSuspend.isChecked)
                .apply()
            Toast.makeText(this, "All Settings Saved Offline!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateVoiceStatus() {
        val voiceFile = File(filesDir, "custom_voice.mp3")
        tvVoiceStatus.text = if (voiceFile.exists()) {
            "Status: Custom Voice Loaded (Offline Ready)"
        } else {
            "Status: No Custom Voice Selected Yet"
        }
    }

    private fun saveVoiceFile(uri: Uri) {
        try {
            val destFile = File(filesDir, "custom_voice.mp3")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            updateVoiceStatus()
            Toast.makeText(this, "Custom voice saved offline!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
