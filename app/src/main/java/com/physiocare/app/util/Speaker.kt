package com.physiocare.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Spoken coaching using the phone's built-in voice (no download, works offline). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var enabled = true

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale.getDefault())
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }
        tts.setSpeechRate(0.9f)
        ready = true
    }

    fun say(text: String, queue: Boolean = false) {
        if (!enabled || !ready || text.isBlank()) return
        val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts.speak(text, mode, null, "pc_${System.nanoTime()}")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
