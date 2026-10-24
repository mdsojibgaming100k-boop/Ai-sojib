package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bridge.ActionBridge
import com.example.bridge.AppInfo
import com.example.data.AppSettings
import com.example.data.CharacterStyle
import com.example.data.SettingsRepository
import com.example.security.SecureKeyStorage
import com.example.service.VoiceForegroundService
import com.example.ui.components.CyberCard
import com.example.ui.components.NeonButton
import com.example.ui.components.ParticleFieldCanvas
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    keyStorage: SecureKeyStorage,
    actionBridge: ActionBridge,
    voiceManager: VoiceManager,
    onNavigateBack: () -> Unit,
    onNavigateToApiGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settings.collectAsState()
    val isServiceRunning by VoiceForegroundService.isServiceRunning.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("General", "Character", "Voice", "System", "About")

    var discoveredApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        discoveredApps = actionBridge.discoverLaunchableApps(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS // CONFIGURATION",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CyberSurfaceElevated,
                contentColor = NeonCyan,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = NeonCyan,
                        height = 3.dp
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("tab_${title.lowercase()}")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                ParticleFieldCanvas(enabled = settings.animationsEnabled, activityFactor = 0.1f)

                when (selectedTabIndex) {
                    0 -> GeneralSettingsTab(
                        settings = settings,
                        isServiceRunning = isServiceRunning,
                        onUpdate = { settingsRepository.updateSettings(it) },
                        onToggleService = { enable ->
                            if (enable) {
                                VoiceForegroundService.startService(context)
                            } else {
                                VoiceForegroundService.stopService(context)
                            }
                        }
                    )
                    1 -> CharacterSettingsTab(
                        settings = settings,
                        onUpdate = { settingsRepository.updateSettings(it) }
                    )
                    2 -> VoiceSettingsTab(
                        settings = settings,
                        keyStorage = keyStorage,
                        voiceManager = voiceManager,
                        onUpdate = {
                            settingsRepository.updateSettings(it)
                            voiceManager.currentLanguage = it.voiceLanguage
                            voiceManager.speechRate = it.speechRate
                            voiceManager.speechPitch = it.speechPitch
                            voiceManager.wakePhrase = it.wakePhrase
                            voiceManager.isTtsEnabled = it.ttsEnabled
                        },
                        onNavigateToApiGate = onNavigateToApiGate
                    )
                    3 -> SystemSettingsTab(
                        context = context,
                        actionBridge = actionBridge,
                        discoveredApps = discoveredApps
                    )
                    4 -> AboutTab(
                        keyStorage = keyStorage
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    settings: AppSettings,
    isServiceRunning: Boolean,
    onUpdate: (AppSettings) -> Unit,
    onToggleService: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CyberCard {
            Column {
                Text(
                    text = "BACKGROUND VOICE LISTENER",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Microphone Foreground Service",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isServiceRunning) "Status: RUNNING (Persistent notification active)" else "Status: STOPPED",
                            color = if (isServiceRunning) CyberGreen else TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { enable ->
                            onToggleService(enable)
                            onUpdate(settings.copy(backgroundVoiceEnabled = enable))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier.testTag("bg_service_switch")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android requires a persistent foreground notification with Pause/Resume/Stop controls while recording in the background.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "UI & GRAPHICS",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Animated Cyber Particles",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = settings.animationsEnabled,
                        onCheckedChange = { onUpdate(settings.copy(animationsEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("particle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Listen on Launch",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = settings.autoListenOnLaunch,
                        onCheckedChange = { onUpdate(settings.copy(autoListenOnLaunch = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persistent Chat Memory",
                            color = TextWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Preserves conversation history locally across restarts",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.persistentMemoryEnabled,
                        onCheckedChange = { onUpdate(settings.copy(persistentMemoryEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlack,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterSettingsTab(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CyberCard {
            Column {
                Text(
                    text = "ASSISTANT AVATAR MODEL",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                CharacterStyle.values().forEach { style ->
                    val isSelected = settings.characterStyle == style
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyberSurfaceElevated else CyberSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonCyan else CyberSurfaceBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUpdate(settings.copy(characterStyle = style)) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = style.displayName,
                                    color = if (isSelected) TextCyan else TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = style.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "ANIMATION MOTIONS",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Idle Breathing Pulse", color = TextWhite, fontSize = 14.sp)
                    Switch(
                        checked = settings.idleAnimationEnabled,
                        onCheckedChange = { onUpdate(settings.copy(idleAnimationEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speaking Waveform Halo", color = TextWhite, fontSize = 14.sp)
                    Switch(
                        checked = settings.speakingPulseEnabled,
                        onCheckedChange = { onUpdate(settings.copy(speakingPulseEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = NeonCyan)
                    )
                }
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "ASSISTANT IDENTITY",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.characterName,
                    onValueChange = { onUpdate(settings.copy(characterName = it)) },
                    label = { Text("Assistant Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceSettingsTab(
    settings: AppSettings,
    keyStorage: SecureKeyStorage,
    voiceManager: VoiceManager,
    onUpdate: (AppSettings) -> Unit,
    onNavigateToApiGate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CyberCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GEMINI API CREDENTIALS",
                        color = TextCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedButton(
                        onClick = onNavigateToApiGate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier.testTag("manage_key_button")
                    ) {
                        Text("Manage / Test Key", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (keyStorage.isValidationPassed()) "Status: Verified & Encrypted in Keystore" else "Status: Key requires validation",
                    color = if (keyStorage.isValidationPassed()) CyberGreen else CyberRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "SPEECH INPUT LANGUAGE",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceLanguage.values().forEach { lang ->
                        val isSelected = settings.voiceLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdate(settings.copy(voiceLanguage = lang)) },
                            label = { Text(lang.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = CyberBlack,
                                containerColor = CyberSurfaceElevated,
                                labelColor = TextWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NeonCyan else CyberSurfaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "WAKE PHRASE",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.wakePhrase,
                    onValueChange = { onUpdate(settings.copy(wakePhrase = it)) },
                    label = { Text("Wake Phrase") },
                    placeholder = { Text("Default: Hey Sojib") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "TEXT-TO-SPEECH (TTS) AUDIO",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spoken Voice Output", color = TextWhite, fontSize = 14.sp)
                    Switch(
                        checked = settings.ttsEnabled,
                        onCheckedChange = { onUpdate(settings.copy(ttsEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberBlack, checkedTrackColor = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Speech Rate: ${String.format("%.1fx", settings.speechRate)}",
                    color = TextWhite,
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = { onUpdate(settings.copy(speechRate = it)) },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speech Pitch: ${String.format("%.1fx", settings.speechPitch)}",
                    color = TextWhite,
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.speechPitch,
                    onValueChange = { onUpdate(settings.copy(speechPitch = it)) },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val testPhrase = if (settings.voiceLanguage == VoiceLanguage.BENGALI) {
                            "হ্যালো! আমি সজীব এআই। আপনার সিস্টেম রেডি আছে।"
                        } else {
                            "Hello! I am Sojib AI. Your system assistant is ready."
                        }
                        voiceManager.speak(testPhrase)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Voice Synthesis")
                }
            }
        }
    }
}

@Composable
private fun SystemSettingsTab(
    context: Context,
    actionBridge: ActionBridge,
    discoveredApps: List<AppInfo>
) {
    val pm = context.packageManager
    var appSearchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(discoveredApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) discoveredApps.take(20)
        else discoveredApps.filter {
            it.name.contains(appSearchQuery, ignoreCase = true) ||
                    it.packageName.contains(appSearchQuery, ignoreCase = true)
        }
    }

    // Permission checks
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CyberCard {
            Column {
                Text(
                    text = "ACTIONBRIDGE STATUS",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Typed Action Registry: ACTIVE (12 System Actions)",
                    color = CyberGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Supported: Open Apps, Google Search, YouTube Search, Alarm, Timer, Flashlight, Volume, Time, Maps, Phone Dial (confirmed), WhatsApp (confirmed).",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "PERMISSION DIAGNOSTICS",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow("Microphone (Audio Recognition)", hasMic)
                PermissionRow("Notifications (Foreground Controls)", hasNotif)
                PermissionRow("Camera / Torch (Flashlight control)", hasCamera)

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Text("Open App Permissions in Settings")
                }
            }
        }

        CyberCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISCOVERED APPS (${discoveredApps.size} Launchable)",
                        color = TextCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Compliant Android 11+ Package Visibility query: discover launchable packages on this device.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    placeholder = { Text("Filter discovered apps...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberSurfaceBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    filteredApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberSurfaceElevated, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(app.packageName, color = TextDim, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        CyberCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = CyberGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BATTERY OPTIMIZATION GUIDANCE",
                        color = TextCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Certain device manufacturers (Samsung, Xiaomi/MIUI, Huawei, OnePlus) aggressively stop background services. If you use the background microphone foreground service, whitelist SOJIB AI under Battery Optimization in system settings to keep the service alive.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = TextWhite, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (granted) CyberGreen else CyberRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (granted) "Granted" else "Denied",
                color = if (granted) CyberGreen else CyberRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AboutTab(
    keyStorage: SecureKeyStorage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CyberCard {
            Column {
                Text(
                    text = "SOJIB AI // NEURAL ASSISTANT",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Version 1.0.0 (Production Core)",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SOJIB AI is a native futuristic Android assistant powered by Google's Gemini models (gemini-3.5-flash, gemini-3.1-pro-preview, gemini-3.1-flash-lite-preview). It features real Bengali & English voice recognition, conversational intelligence, and ActionBridge device controls.",
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "PRIVACY & LOCAL INTEGRITY",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Speech Audio: No audio recordings are ever stored or uploaded secretly. Audio recognition runs only when explicitly triggered via Push-to-Talk or the user-started foreground service.\n" +
                            "• Credentials: API keys are stored in encrypted form using Android Keystore AES-GCM encryption.\n" +
                            "• Actions: Sensitive actions (such as phone calls or sending messages) explicitly require your confirmation.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        CyberCard {
            Column {
                Text(
                    text = "FEATURE LIMITATIONS & HARDWARE",
                    color = TextCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Camera Flashlight: Requires a physical camera flash LED on the device.\n" +
                            "• Bengali Text-to-Speech: Requires a Bengali TTS voice installed in Android Text-to-Speech settings (Google TTS Engine supports Bengali bn-BD/bn-IN).\n" +
                            "• Third-Party Apps: SOJIB AI uses official Android Intents to interface with TikTok, YouTube, WhatsApp, and Google without bypassing security locks.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
