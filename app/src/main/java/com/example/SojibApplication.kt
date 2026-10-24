package com.example

import android.app.Application
import com.example.api.GeminiApiClient
import com.example.bridge.ActionBridge
import com.example.data.ChatRepository
import com.example.data.SettingsRepository
import com.example.security.SecureKeyStorage

class SojibApplication : Application() {

    lateinit var secureKeyStorage: SecureKeyStorage
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var actionBridge: ActionBridge
        private set

    lateinit var geminiApiClient: GeminiApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        secureKeyStorage = SecureKeyStorage(this)
        settingsRepository = SettingsRepository(this)
        chatRepository = ChatRepository(this).apply {
            setMemoryEnabled(settingsRepository.settings.value.persistentMemoryEnabled)
        }
        actionBridge = ActionBridge()
        geminiApiClient = GeminiApiClient()
    }

    companion object {
        lateinit var instance: SojibApplication
            private set
    }
}
