package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.api.ChatMessage
import com.example.api.GeminiApiClient
import com.example.bridge.ActionBridge
import com.example.bridge.ActionRequest
import com.example.bridge.ActionResult
import com.example.data.ChatRepository
import com.example.data.SettingsRepository
import com.example.security.SecureKeyStorage
import com.example.ui.components.AssistantOrbAndCharacter
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ParticleFieldCanvas
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.voice.SpeechState
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainAssistantScreen(
    keyStorage: SecureKeyStorage,
    geminiClient: GeminiApiClient,
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    actionBridge: ActionBridge,
    voiceManager: VoiceManager,
    onOpenSettings: () -> Unit,
    onReturnToKeyGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by settingsRepository.settings.collectAsState()
    val messages by chatRepository.messages.collectAsState()
    val speechState by voiceManager.speechState.collectAsState()
    val partialSpeech by voiceManager.partialText.collectAsState()
    val audioRms by voiceManager.audioRms.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var isStandby by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var pendingConfirmationAction by remember { mutableStateOf<ActionRequest?>(null) }
    var currentStreamingJob by remember { mutableStateOf<Job?>(null) }

    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening()
        } else {
            chatRepository.addMessage(
                ChatMessage(
                    text = "Microphone permission is required for voice recognition.",
                    isUser = false,
                    isError = true
                )
            )
        }
    }

    fun handleUserPrompt(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return

        // Stop active speech playback if any
        voiceManager.stopSpeaking()

        // 1. Add user message
        chatRepository.addMessage(
            ChatMessage(text = cleanPrompt, isUser = true)
        )
        textInput = ""

        // 2. Check ActionBridge for direct system action
        val installedApps = actionBridge.discoverLaunchableApps(context)
        val actionRequest = actionBridge.parseIntentOrCommand(cleanPrompt, installedApps)

        if (actionRequest != null) {
            if (actionRequest.requiresConfirmation) {
                pendingConfirmationAction = actionRequest
                return
            } else {
                val actionResult = actionBridge.executeAction(context, actionRequest)
                chatRepository.addMessage(
                    ChatMessage(
                        text = actionResult.message,
                        isUser = false,
                        actionSummary = "Action: ${actionRequest.type.name}",
                        isError = !actionResult.success
                    )
                )
                if (settings.ttsEnabled) {
                    voiceManager.speak(actionResult.message)
                }
                return
            }
        }

        // 3. Query Gemini Neural API
        val apiKey = keyStorage.getApiKey()
        if (apiKey.isNullOrEmpty() || !keyStorage.isValidationPassed()) {
            chatRepository.addMessage(
                ChatMessage(
                    text = "API Key not validated. Please configure your Gemini API credential in settings.",
                    isUser = false,
                    isError = true
                )
            )
            onReturnToKeyGate()
            return
        }

        isThinking = true
        currentStreamingJob?.cancel()

        // Add a placeholder assistant response
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        chatRepository.addMessage(
            ChatMessage(id = assistantMsgId, text = "...", isUser = false)
        )

        val systemPrompt = buildString {
            append("You are SOJIB AI, an advanced, highly intelligent futuristic cyber assistant created for Android. ")
            append("You are bilingual and fluent in both Bengali (বাংলা) and English. Respond in the language used by the user. ")
            append("Be direct, concise, helpful, and polite. If the user asks about device actions, explain clearly. ")
            if (settings.customSystemPrompt.isNotBlank()) {
                append("\nCustom Persona Instructions: ")
                append(settings.customSystemPrompt)
            }
        }

        currentStreamingJob = scope.launch {
            var fullResponse = ""
            try {
                geminiClient.streamGenerateContent(
                    apiKey = apiKey,
                    history = messages.dropLast(1),
                    newPrompt = cleanPrompt,
                    systemInstruction = systemPrompt,
                    model = settings.selectedModel
                ).collect { chunk ->
                    fullResponse += chunk
                    chatRepository.updateLastMessage(fullResponse)
                }

                isThinking = false
                if (fullResponse.isBlank()) {
                    // Fallback to non-streaming if stream was empty
                    val fallback = geminiClient.generateContent(
                        apiKey = apiKey,
                        history = messages.dropLast(1),
                        newPrompt = cleanPrompt,
                        systemInstruction = systemPrompt,
                        model = settings.selectedModel
                    )
                    fullResponse = fallback.getOrDefault("System could not generate a response.")
                    chatRepository.updateLastMessage(fullResponse)
                }

                if (settings.ttsEnabled && !isStandby) {
                    voiceManager.speak(fullResponse)
                }
            } catch (e: Exception) {
                isThinking = false
                val errorDesc = "Gemini Error: ${e.localizedMessage ?: "Network or request failure"}"
                chatRepository.updateLastMessage(errorDesc, isError = true)
            }
        }
    }

    // Register voice manager recognition listener
    LaunchedEffect(voiceManager) {
        // Automatically link voice callback
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background Cyber Particles
            ParticleFieldCanvas(
                enabled = settings.animationsEnabled && !isStandby,
                activityFactor = if (speechState == SpeechState.LISTENING) audioRms else if (isThinking) 0.6f else 0.15f
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Assistant Title & Standby indicator
                    Column {
                        Text(
                            text = settings.characterName.uppercase(),
                            color = if (isStandby) TextMuted else NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "NEURAL CYBER CORE",
                            color = TextDim,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    // Status Pill
                    StatusPill(
                        speechState = if (isThinking) SpeechState.PROCESSING else speechState,
                        isOffline = isStandby
                    )

                    // Top Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clear chat
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = { chatRepository.clearHistory() },
                                modifier = Modifier.size(36.dp).testTag("clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Power / Standby Toggle
                        IconButton(
                            onClick = {
                                isStandby = !isStandby
                                if (isStandby) {
                                    voiceManager.stopListening()
                                    voiceManager.stopSpeaking()
                                }
                            },
                            modifier = Modifier.size(36.dp).testTag("power_standby_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Standby Toggle",
                                tint = if (isStandby) CyberRed else CyberGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Settings button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(36.dp).testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Central Assistant Avatar Area
                AnimatedVisibility(visible = !isStandby) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        AssistantOrbAndCharacter(
                            characterStyle = settings.characterStyle,
                            speechState = if (isThinking) SpeechState.PROCESSING else speechState,
                            audioRms = audioRms,
                            sizeDp = 180.dp
                        )

                        // Real-time voice RMS visualizer or partial text
                        if (speechState == SpeechState.LISTENING) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AudioWaveformVisualizer(audioRms = audioRms)
                            if (partialSpeech.isNotEmpty()) {
                                Text(
                                    text = "\"$partialSpeech\"",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                )
                            }
                        } else if (isThinking) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Synthesizing neural response...",
                                color = Color(0xFF00F5D4),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Quick Action Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickActions = listOf(
                        "Search YouTube" to "search youtube trending music",
                        "Open Camera" to "open camera",
                        "Flashlight" to "turn on flashlight",
                        "Time" to "what time is it",
                        "Open WhatsApp" to "open whatsapp",
                        "1-Min Timer" to "set timer 60 seconds",
                        "গুগলে খুঁজুন" to "search google Dhaka weather"
                    )
                    items(quickActions) { (label, command) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(12.dp))
                                .clickable { handleUserPrompt(command) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = TextCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Conversation Log (LazyColumn)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SOJIB AI ONLINE",
                                color = TextCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Speak in Bengali/English or type commands below.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatMessageBubble(
                                    message = msg,
                                    onSpeakText = { text ->
                                        voiceManager.speak(text)
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Input & Mic Push-to-Talk Bar
                CyberInputBar(
                    textInput = textInput,
                    onTextChange = { textInput = it },
                    onSend = { handleUserPrompt(textInput) },
                    speechState = speechState,
                    currentLanguage = settings.voiceLanguage,
                    onToggleLanguage = {
                        val nextLang = when (settings.voiceLanguage) {
                            VoiceLanguage.BENGALI -> VoiceLanguage.ENGLISH
                            VoiceLanguage.ENGLISH -> VoiceLanguage.AUTO
                            VoiceLanguage.AUTO -> VoiceLanguage.BENGALI
                        }
                        settingsRepository.updateSettings(settings.copy(voiceLanguage = nextLang))
                        voiceManager.currentLanguage = nextLang
                    },
                    onMicClick = {
                        if (speechState == SpeechState.LISTENING) {
                            voiceManager.stopListening()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                voiceManager.startListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )
            }

            // Sensitive Action Confirmation Dialog
            if (pendingConfirmationAction != null) {
                val action = pendingConfirmationAction!!
                AlertDialog(
                    onDismissRequest = { pendingConfirmationAction = null },
                    containerColor = CyberSurface,
                    title = {
                        Text(
                            text = action.confirmationTitle ?: "Confirm Sensitive Action",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = action.confirmationPrompt ?: "Do you wish to authorize this system action?",
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val result = actionBridge.executeAction(context, action)
                                chatRepository.addMessage(
                                    ChatMessage(
                                        text = result.message,
                                        isUser = false,
                                        actionSummary = "Executed: ${action.type.name}",
                                        isError = !result.success
                                    )
                                )
                                pendingConfirmationAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack)
                        ) {
                            Text("Confirm & Execute")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingConfirmationAction = null }) {
                            Text("Cancel", color = TextMuted)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onSpeakText: (String) -> Unit
) {
    val isUser = message.isUser
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.25f),
                                DeepViolet.copy(alpha = 0.45f)
                            )
                        )
                    } else if (message.isError) {
                        Brush.horizontalGradient(
                            listOf(
                                CyberRed.copy(alpha = 0.25f),
                                CyberSurfaceElevated
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                CyberSurfaceElevated,
                                CyberSurface
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) NeonCyan.copy(alpha = 0.6f) else if (message.isError) CyberRed else CyberSurfaceBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.actionSummary != null) {
                    Text(
                        text = message.actionSummary.uppercase(),
                        color = CyberGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    color = if (message.isError) CyberRed else TextWhite,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        color = TextDim,
                        fontSize = 10.sp
                    )
                    if (!isUser && !message.isError) {
                        IconButton(
                            onClick = { onSpeakText(message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak response",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberInputBar(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    speechState: SpeechState,
    currentLanguage: VoiceLanguage,
    onToggleLanguage: () -> Unit,
    onMicClick: () -> Unit
) {
    val isListening = speechState == SpeechState.LISTENING

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micHaloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface.copy(alpha = 0.98f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(CyberSurfaceBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language quick toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceElevated)
                    .clickable { onToggleLanguage() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (currentLanguage) {
                        VoiceLanguage.BENGALI -> "BN"
                        VoiceLanguage.ENGLISH -> "EN"
                        VoiceLanguage.AUTO -> "AUTO"
                    },
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text Input Field
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = if (isListening) "Listening to speech..." else "Type command or question...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberSurfaceBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = CyberSurfaceElevated,
                    unfocusedContainerColor = CyberSurfaceElevated
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // If text is entered, show Send button; otherwise show central Glowing Mic
            if (textInput.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = CyberBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Mic Push-to-Talk button with glowing halo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(if (isListening) micHaloScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isListening) {
                                Brush.radialGradient(
                                    listOf(NeonCyan, DeepViolet)
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(CyberSurfaceElevated, DeepViolet)
                                )
                            }
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isListening) NeonCyan else NeonCyan.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onMicClick() }
                        .testTag("mic_push_to_talk_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop Listening" else "Push to Talk",
                        tint = if (isListening) CyberBlack else NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
