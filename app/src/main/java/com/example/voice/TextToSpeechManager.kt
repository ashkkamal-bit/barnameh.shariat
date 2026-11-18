package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsStatus(
    val isInitialized: Boolean = false,
    val isPersianAvailable: Boolean = false,
    val statusMessage: String = "در حال بررسی بسته صوتی فارسی..."
)

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var currentRate = 1.0f

    private val _status = MutableStateFlow(TtsStatus())
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (_: Exception) {
            _status.value = TtsStatus(
                isInitialized = false,
                isPersianAvailable = false,
                statusMessage = "موتور تبدیل متن به گفتار روی این دستگاه نصب نیست."
            )
        }
    }

    override fun onInit(statusCode: Int) {
        val engine = tts ?: return
        if (statusCode != TextToSpeech.SUCCESS) {
            _status.value = TtsStatus(
                isInitialized = false,
                isPersianAvailable = false,
                statusMessage = "پخش صوتی روی این دستگاه پشتیبانی نمی‌شود؛ پاسخ‌ها به صورت متنی نمایش داده می‌شوند."
            )
            return
        }

        val faLocale = Locale.forLanguageTag("fa-IR")
        val langResult = engine.isLanguageAvailable(faLocale)
        val persianOk = langResult >= TextToSpeech.LANG_AVAILABLE

        if (persianOk) {
            engine.language = faLocale
            engine.setSpeechRate(currentRate)
            _status.value = TtsStatus(
                isInitialized = true,
                isPersianAvailable = true,
                statusMessage = "صدای فارسی (fa-IR) آماده است."
            )
        } else {
            _status.value = TtsStatus(
                isInitialized = true,
                isPersianAvailable = false,
                statusMessage = "بسته صدای فارسی در تنظیمات سیستم نصب نیست؛ پاسخ‌های متنی فعال هستند."
            )
        }
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate.coerceIn(0.6f, 1.6f)
        tts?.setSpeechRate(currentRate)
    }

    fun speakIfAllowed(text: String, ttsEnabled: Boolean): Boolean {
        if (!ttsEnabled || text.isBlank()) return false
        val current = _status.value
        if (!current.isInitialized || !current.isPersianAvailable) return false
        val engine = tts ?: return false
        engine.setSpeechRate(currentRate)
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nocturne_reply_${System.currentTimeMillis()}") == TextToSpeech.SUCCESS
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }
}
