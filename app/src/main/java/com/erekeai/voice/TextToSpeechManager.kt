package com.erekeai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** "Voice Agent" (вывод) — озвучивание ответов через штатный Android TextToSpeech. */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(onReady: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.getDefault(); ready = true; onReady() }
        }
    }

    fun speak(text: String) {
        if (!ready) { init { speak(text) }; return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ereke_utterance")
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.shutdown(); tts = null; ready = false }
}
