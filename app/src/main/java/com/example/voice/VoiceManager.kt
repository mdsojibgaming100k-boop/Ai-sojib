package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceLanguage(val code: String, val displayName: String, val locale: Locale) {
    BENGALI("bn", "বাংলা (Bengali)", Locale.forLanguageTag("bn-BD")),
    ENGLISH("en", "English", Locale.US),
    AUTO("auto", "Auto (Device)", Locale.getDefault())
}

enum class SpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _speechState = MutableStateFlow(SpeechState.IDLE)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    var currentLanguage: VoiceLanguage = VoiceLanguage.BENGALI
    var speechRate: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }
    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }
    var wakePhrase: String = "Hey Sojib"
    var isTtsEnabled: Boolean = true

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceManager)
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            _speechState.value = SpeechState.ERROR
            return
        }

        // Stop speaking if currently active
        stopSpeaking()

        initSpeechRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            val locale = when (currentLanguage) {
                VoiceLanguage.BENGALI -> Locale.forLanguageTag("bn-BD")
                VoiceLanguage.ENGLISH -> Locale.US
                VoiceLanguage.AUTO -> Locale.getDefault()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "en-US"))
        }

        try {
            _partialText.value = ""
            _audioRms.value = 0f
            _speechState.value = SpeechState.LISTENING
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.ERROR
            onError("Failed to start voice listener: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            if (_speechState.value == SpeechState.LISTENING) {
                _speechState.value = SpeechState.IDLE
            }
        } catch (e: Exception) {
            // Ignore stop errors
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            _speechState.value = SpeechState.IDLE
            _audioRms.value = 0f
            _partialText.value = ""
        } catch (e: Exception) {
            // Ignore cancel errors
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsEnabled || !isTtsInitialized || textToSpeech == null) {
            onComplete?.invoke()
            return
        }

        val cleanText = text
            .replace(Regex("[*#`_~]"), "") // Strip markdown symbols
            .trim()

        if (cleanText.isEmpty()) {
            onComplete?.invoke()
            return
        }

        _speechState.value = SpeechState.SPEAKING

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speechState.value = SpeechState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                _speechState.value = SpeechState.IDLE
                onComplete?.invoke()
            }

            override fun onError(utteranceId: String?) {
                _speechState.value = SpeechState.IDLE
                onComplete?.invoke()
            }
        })

        val utteranceId = "sojib_tts_${System.currentTimeMillis()}"
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        if (_speechState.value == SpeechState.SPEAKING) {
            _speechState.value = SpeechState.IDLE
        }
    }

    fun isTtsSpeaking(): Boolean = textToSpeech?.isSpeaking == true

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            setupTtsLanguage()
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)
        }
    }

    fun setupTtsLanguage() {
        val tts = textToSpeech ?: return
        val targetLocale = when (currentLanguage) {
            VoiceLanguage.BENGALI -> {
                val bd = Locale.forLanguageTag("bn-BD")
                val res = tts.isLanguageAvailable(bd)
                if (res >= TextToSpeech.LANG_AVAILABLE) bd else {
                    val bn = Locale.forLanguageTag("bn")
                    if (tts.isLanguageAvailable(bn) >= TextToSpeech.LANG_AVAILABLE) bn else Locale.US
                }
            }
            VoiceLanguage.ENGLISH -> Locale.US
            VoiceLanguage.AUTO -> Locale.getDefault()
        }
        tts.language = targetLocale
    }

    // SpeechRecognizer Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _speechState.value = SpeechState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _speechState.value = SpeechState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize typical rms dB (-2 to 10) to 0f..1f range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _speechState.value = SpeechState.PROCESSING
        _audioRms.value = 0f
    }

    override fun onError(error: Int) {
        _audioRms.value = 0f
        val (state, message) = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> SpeechState.ERROR to "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> SpeechState.IDLE to "" // Handled or cancelled
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechState.ERROR to "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK -> SpeechState.ERROR to "Network error during speech recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechState.ERROR to "Speech network timeout."
            SpeechRecognizer.ERROR_NO_MATCH -> SpeechState.IDLE to "No speech was detected. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechState.IDLE to "Speech engine is busy. Please wait a moment."
            SpeechRecognizer.ERROR_SERVER -> SpeechState.ERROR to "Server error during speech recognition."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechState.IDLE to "No speech detected within time limit."
            else -> SpeechState.ERROR to "Speech recognition error ($error)."
        }

        _speechState.value = state
        if (message.isNotEmpty() && state == SpeechState.ERROR) {
            onError(message)
        }
    }

    override fun onResults(results: Bundle?) {
        _speechState.value = SpeechState.IDLE
        _audioRms.value = 0f

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognized = matches?.firstOrNull() ?: ""

        if (recognized.isNotBlank()) {
            // Check wake phrase if present
            var cleanText = recognized.trim()
            val wp = wakePhrase.trim()
            if (wp.isNotEmpty() && cleanText.startsWith(wp, ignoreCase = true)) {
                cleanText = cleanText.substring(wp.length).trim().removePrefix(",").trim()
            }
            if (cleanText.isEmpty()) {
                cleanText = recognized.trim()
            }
            _partialText.value = cleanText
            onSpeechRecognized(cleanText)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: ""
        if (partial.isNotEmpty()) {
            _partialText.value = partial
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
