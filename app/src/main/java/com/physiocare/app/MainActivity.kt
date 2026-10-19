package com.physiocare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.physiocare.app.data.AppRepository
import com.physiocare.app.ui.PhysioApp
import com.physiocare.app.util.Reminder
import com.physiocare.app.util.Speaker

class MainActivity : ComponentActivity() {
    private lateinit var speaker: Speaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = AppRepository.get(this)
        speaker = Speaker(this)
        speaker.enabled = repo.settings.voice
        Reminder.ensureChannel(this)
        setContent { PhysioApp(repo, speaker) }
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
