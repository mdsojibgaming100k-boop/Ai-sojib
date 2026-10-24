package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.voice.VoiceLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CharacterStyle(val id: String, val displayName: String, val description: String) {
    CYBER_SOJIB("cyber_sojib", "Cybernetic Sojib", "Cyber anime assistant with neon visor and holographic sync rings"),
    NEURAL_CORE("neural_core", "Neural Hologram Core", "Abstract rotating quantum neural rings and particle energy core"),
    MECHA_AI("mecha_ai", "Mecha Defense AI", "Tactical cybernetic defense assistant with high-frequency aura")
}

data class AppSettings(
    val backgroundVoiceEnabled: Boolean = false,
    val animationsEnabled: Boolean = true,
    val autoListenOnLaunch: Boolean = false,
    val characterStyle: CharacterStyle = CharacterStyle.CYBER_SOJIB,
    val characterName: String = "Sojib AI",
    val idleAnimationEnabled: Boolean = true,
    val speakingPulseEnabled: Boolean = true,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.BENGALI,
    val wakeWordEnabled: Boolean = true,
    val wakePhrase: String = "Hey Sojib",
    val ttsEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val persistentMemoryEnabled: Boolean = true,
    val selectedModel: String = "gemini-3.5-flash",
    val customSystemPrompt: String = ""
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sojib_app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val langCode = prefs.getString("voice_lang", VoiceLanguage.BENGALI.code) ?: VoiceLanguage.BENGALI.code
        val lang = VoiceLanguage.values().find { it.code == langCode } ?: VoiceLanguage.BENGALI

        val charId = prefs.getString("char_style", CharacterStyle.CYBER_SOJIB.id) ?: CharacterStyle.CYBER_SOJIB.id
        val charStyle = CharacterStyle.values().find { it.id == charId } ?: CharacterStyle.CYBER_SOJIB

        return AppSettings(
            backgroundVoiceEnabled = prefs.getBoolean("bg_voice", false),
            animationsEnabled = prefs.getBoolean("animations", true),
            autoListenOnLaunch = prefs.getBoolean("auto_listen", false),
            characterStyle = charStyle,
            characterName = prefs.getString("char_name", "Sojib AI") ?: "Sojib AI",
            idleAnimationEnabled = prefs.getBoolean("idle_anim", true),
            speakingPulseEnabled = prefs.getBoolean("speak_pulse", true),
            voiceLanguage = lang,
            wakeWordEnabled = prefs.getBoolean("wake_word", true),
            wakePhrase = prefs.getString("wake_phrase", "Hey Sojib") ?: "Hey Sojib",
            ttsEnabled = prefs.getBoolean("tts_enabled", true),
            speechRate = prefs.getFloat("speech_rate", 1.0f),
            speechPitch = prefs.getFloat("speech_pitch", 1.0f),
            persistentMemoryEnabled = prefs.getBoolean("chat_memory", true),
            selectedModel = prefs.getString("model_name", "gemini-3.5-flash") ?: "gemini-3.5-flash",
            customSystemPrompt = prefs.getString("sys_prompt", "") ?: ""
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit()
            .putBoolean("bg_voice", newSettings.backgroundVoiceEnabled)
            .putBoolean("animations", newSettings.animationsEnabled)
            .putBoolean("auto_listen", newSettings.autoListenOnLaunch)
            .putString("char_style", newSettings.characterStyle.id)
            .putString("char_name", newSettings.characterName)
            .putBoolean("idle_anim", newSettings.idleAnimationEnabled)
            .putBoolean("speak_pulse", newSettings.speakingPulseEnabled)
            .putString("voice_lang", newSettings.voiceLanguage.code)
            .putBoolean("wake_word", newSettings.wakeWordEnabled)
            .putString("wake_phrase", newSettings.wakePhrase)
            .putBoolean("tts_enabled", newSettings.ttsEnabled)
            .putFloat("speech_rate", newSettings.speechRate)
            .putFloat("speech_pitch", newSettings.speechPitch)
            .putBoolean("chat_memory", newSettings.persistentMemoryEnabled)
            .putString("model_name", newSettings.selectedModel)
            .putString("sys_prompt", newSettings.customSystemPrompt)
            .apply()

        _settings.value = newSettings
    }
}
