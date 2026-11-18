package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SpeechState {
    data object Idle : SpeechState
    data class Listening(val partialTranscript: String = "") : SpeechState
    data class Result(val transcript: String) : SpeechState
    data class Error(val message: String, val isUnavailable: Boolean = false) : SpeechState
}

class SpeechRecognitionManager(context: Context) {

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    fun isRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun startListening(languageTag: String = "fa-IR") {
        if (!hasAudioPermission()) {
            _state.value = SpeechState.Error(
                "دسترسی میکروفون داده نشده است. لطفاً مجوز ضبط صدا را تایید کنید یا فرمان را تایپ نمایید."
            )
            return
        }
        if (!isRecognitionAvailable()) {
            _state.value = SpeechState.Error(
                "سرویس تشخیص گفتار در این دستگاه در دسترس نیست. می‌توانید متن را تایپ کنید.",
                isUnavailable = true
            )
            return
        }

        // Prevent duplicate listening sessions
        stopAndDestroyInternal()

        val activeRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = activeRecognizer
        _state.value = SpeechState.Listening("")

        activeRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechState.Listening("")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    _state.value = SpeechState.Listening(partial)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                stopAndDestroyInternal()
                if (text.isEmpty()) {
                    _state.value = SpeechState.Error("صدایی دریافت نشد؛ لطفاً دوباره صحبت کنید یا متن بنویسید.")
                } else {
                    _state.value = SpeechState.Result(text)
                }
            }

            override fun onError(error: Int) {
                stopAndDestroyInternal()
                _state.value = SpeechState.Error(mapSpeechError(error))
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            activeRecognizer.startListening(intent)
        } catch (e: Exception) {
            stopAndDestroyInternal()
            _state.value = SpeechState.Error("امکان شروع ضبط صدا وجود نداشت؛ لطفاً متن را تایپ کنید.", isUnavailable = true)
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }
        if (_state.value is SpeechState.Listening) {
            val partial = (_state.value as SpeechState.Listening).partialTranscript
            if (partial.isNotBlank()) {
                _state.value = SpeechState.Result(partial)
            } else {
                _state.value = SpeechState.Idle
            }
        }
    }

    fun resetState() {
        stopAndDestroyInternal()
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        stopAndDestroyInternal()
        _state.value = SpeechState.Idle
    }

    private fun stopAndDestroyInternal() {
        recognizer?.let { rec ->
            try {
                rec.cancel()
            } catch (_: Exception) {
            }
            try {
                rec.destroy()
            } catch (_: Exception) {
            }
        }
        recognizer = null
    }

    private fun mapSpeechError(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "خطا در دریافت صدا از میکروفون."
        SpeechRecognizer.ERROR_CLIENT -> "نشست تشخیص گفتار متوقف شد."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز میکروفون فعال نیست."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "برای تشخیص گفتار ابری اتصال اینترنت برقرار نیست؛ لطفاً متن را تایپ کنید."
        SpeechRecognizer.ERROR_NO_MATCH -> "گفتار واضح نبود؛ لطفاً دوباره صحبت کنید یا تایپ نمایید."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس تشخیص گفتار مشغول است؛ لحظه‌ای دیگر تلاش کنید."
        SpeechRecognizer.ERROR_SERVER -> "پاسخی از سرویس تشخیص گفتار دریافت نشد."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "مدتی صدایی شنیده نشد؛ دوباره دکمه میکروفون را لمس کنید."
        else -> "تشخیص گفتار انجام نشد؛ لطفاً متن فرمان را وارد کنید."
    }
}
