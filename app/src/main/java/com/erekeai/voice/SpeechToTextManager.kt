package com.erekeai.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ "Voice Agent" (ввод) — распознавание речи через штатный Android SpeechRecognizer
 * (push-to-talk). ЧЕСТНО: постоянный wake-word ("Привет, Ереке") требует отдельного лёгкого
 * нейросетевого детектора (Porcupine/openWakeWord) с фоновой записью аудио — сознательно не
 * включён по умолчанию (батарея + приватность), здесь push-to-talk по нажатию кнопки в UI.
 */
@Singleton
class SpeechToTextManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun listenOnce(languageTag: String = Locale.getDefault().toLanguageTag()): Flow<VoiceEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceEvent.Error("Распознавание речи недоступно на этом устройстве")); close(); return@callbackFlow
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { trySend(VoiceEvent.Listening) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { trySend(VoiceEvent.Error("Ошибка распознавания (код $error)")); close() }
            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                trySend(VoiceEvent.FinalResult(text.orEmpty())); close()
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) trySend(VoiceEvent.PartialResult(text))
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        recognizer.startListening(intent)
        awaitClose { recognizer.stopListening(); recognizer.destroy() }
    }
}

sealed class VoiceEvent {
    object Listening : VoiceEvent()
    data class PartialResult(val text: String) : VoiceEvent()
    data class FinalResult(val text: String) : VoiceEvent()
    data class Error(val message: String) : VoiceEvent()
}
