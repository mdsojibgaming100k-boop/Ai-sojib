package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ApiKeyGateScreen
import com.example.ui.screens.MainAssistantScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.voice.VoiceManager

enum class Screen {
    API_GATE,
    MAIN_ASSISTANT,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private var voiceManager: VoiceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SojibApplication
        val keyStorage = app.secureKeyStorage
        val geminiClient = app.geminiApiClient
        val chatRepository = app.chatRepository
        val settingsRepository = app.settingsRepository
        val actionBridge = app.actionBridge

        setContent {
            MyApplicationTheme {
                val initialScreen = if (keyStorage.isValidationPassed()) {
                    Screen.MAIN_ASSISTANT
                } else {
                    Screen.API_GATE
                }

                var currentScreen by remember { mutableStateOf(initialScreen) }

                // Manage VoiceManager lifecycle
                val activeVoiceManager = remember {
                    VoiceManager(
                        context = this@MainActivity,
                        onSpeechRecognized = { speech ->
                            // Voice recognized handler will be invoked
                        },
                        onError = { err ->
                            // Voice error callback
                        }
                    ).also {
                        val currentSettings = settingsRepository.settings.value
                        it.currentLanguage = currentSettings.voiceLanguage
                        it.speechRate = currentSettings.speechRate
                        it.speechPitch = currentSettings.speechPitch
                        it.wakePhrase = currentSettings.wakePhrase
                        it.isTtsEnabled = currentSettings.ttsEnabled
                        voiceManager = it
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        activeVoiceManager.destroy()
                    }
                }

                // Handle system back navigation
                BackHandler(enabled = currentScreen != Screen.MAIN_ASSISTANT && keyStorage.isValidationPassed()) {
                    currentScreen = Screen.MAIN_ASSISTANT
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBlack)
                        .safeDrawingPadding()
                ) {
                    when (currentScreen) {
                        Screen.API_GATE -> {
                            ApiKeyGateScreen(
                                keyStorage = keyStorage,
                                geminiClient = geminiClient,
                                onKeyValidatedAndSaved = {
                                    currentScreen = Screen.MAIN_ASSISTANT
                                }
                            )
                        }
                        Screen.MAIN_ASSISTANT -> {
                            MainAssistantScreen(
                                keyStorage = keyStorage,
                                geminiClient = geminiClient,
                                chatRepository = chatRepository,
                                settingsRepository = settingsRepository,
                                actionBridge = actionBridge,
                                voiceManager = activeVoiceManager,
                                onOpenSettings = {
                                    currentScreen = Screen.SETTINGS
                                },
                                onReturnToKeyGate = {
                                    currentScreen = Screen.API_GATE
                                }
                            )
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                settingsRepository = settingsRepository,
                                keyStorage = keyStorage,
                                actionBridge = actionBridge,
                                voiceManager = activeVoiceManager,
                                onNavigateBack = {
                                    currentScreen = Screen.MAIN_ASSISTANT
                                },
                                onNavigateToApiGate = {
                                    currentScreen = Screen.API_GATE
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        voiceManager?.destroy()
        super.onDestroy()
    }
}
