package com.example.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.api.ApiValidationResult
import com.example.api.GeminiApiClient
import com.example.security.SecureKeyStorage
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
import kotlinx.coroutines.launch

@Composable
fun ApiKeyGateScreen(
    keyStorage: SecureKeyStorage,
    geminiClient: GeminiApiClient,
    onKeyValidatedAndSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKeyInput by remember {
        mutableStateOf(keyStorage.getApiKey() ?: "")
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<ApiValidationResult?>(null) }
    var selectedModel by remember { mutableStateOf(keyStorage.getSelectedModel()) }

    // Check if key is already validated
    val isAlreadyValidated = remember { keyStorage.isValidationPassed() }

    fun runConnectionTest() {
        if (apiKeyInput.isBlank()) {
            validationResult = ApiValidationResult.InvalidKey("Please enter a non-empty Gemini API key.")
            return
        }

        isTestingConnection = true
        validationResult = null

        scope.launch {
            val result = geminiClient.testConnection(apiKeyInput, selectedModel)
            isTestingConnection = false
            validationResult = result

            if (result is ApiValidationResult.Success) {
                keyStorage.saveApiKey(apiKeyInput)
                keyStorage.setValidationPassed(true)
            } else {
                keyStorage.setValidationPassed(false)
            }
        }
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
            ParticleFieldCanvas(enabled = true, activityFactor = 0.2f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cyber Emblem Header
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DeepViolet)
                        .border(2.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Security Gate",
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SOJIB AI // ACCESS GATE",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Google Gemini Neural API Configuration",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Input Card
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (validationResult is ApiValidationResult.Success) CyberGreen else CyberSurfaceBorder
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "GEMINI API CREDENTIAL",
                                color = TextCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (validationResult is ApiValidationResult.Success) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Validated",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VERIFIED",
                                    color = CyberGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                // Invalidate previous test result when key text edits
                                validationResult = null
                                keyStorage.setValidationPassed(false)
                            },
                            label = { Text("Gemini API Key (AIzaSy...)") },
                            placeholder = { Text("Paste your Google Gemini API key") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isPasswordVisible = !isPasswordVisible },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isPasswordVisible) "Hide Key" else "Show Key",
                                            tint = TextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = clipboard?.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                                apiKeyInput = pasted.trim()
                                                validationResult = null
                                                keyStorage.setValidationPassed(false)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste Key",
                                            tint = NeonCyan
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberSurfaceBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextMuted,
                                focusedContainerColor = CyberSurfaceElevated,
                                unfocusedContainerColor = CyberSurfaceElevated
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick action buttons under key field
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // If BuildConfig has a key, allow quick auto-fill
                            val buildConfigKey = BuildConfig.GEMINI_API_KEY
                            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyInput = buildConfigKey.trim()
                                        validationResult = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    modifier = Modifier.testTag("autofill_env_button")
                                ) {
                                    Text("Fill from Env", fontSize = 11.sp)
                                }
                            }

                            if (apiKeyInput.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyInput = ""
                                        validationResult = null
                                        keyStorage.clearApiKey()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                                    modifier = Modifier.testTag("remove_key_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Key",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Key", fontSize = 11.sp)
                                }
                            }
                        }

                        // Model Selector Chip
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ACTIVE MODEL: $selectedModel",
                            color = TextDim,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Validation Status Card
                if (validationResult != null) {
                    val result = validationResult!!
                    val isSuccess = result is ApiValidationResult.Success
                    val alertColor = if (isSuccess) CyberGreen else CyberRed
                    val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning

                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = alertColor
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = alertColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val title = when (result) {
                                    is ApiValidationResult.Success -> "CONNECTION CONFIRMED"
                                    is ApiValidationResult.InvalidKey -> "INVALID API KEY"
                                    is ApiValidationResult.QuotaExceeded -> "QUOTA EXCEEDED"
                                    is ApiValidationResult.NetworkError -> "NETWORK FAILURE"
                                    is ApiValidationResult.Timeout -> "REQUEST TIMEOUT"
                                    is ApiValidationResult.ServerError -> "GEMINI SERVER ERROR"
                                    is ApiValidationResult.UnknownError -> "VALIDATION FAILED"
                                }
                                val detail = when (result) {
                                    is ApiValidationResult.Success -> "${result.message}. Ready to launch."
                                    is ApiValidationResult.InvalidKey -> result.details
                                    is ApiValidationResult.QuotaExceeded -> result.details
                                    is ApiValidationResult.NetworkError -> result.details
                                    is ApiValidationResult.Timeout -> result.details
                                    is ApiValidationResult.ServerError -> result.details
                                    is ApiValidationResult.UnknownError -> result.details
                                }

                                Text(
                                    text = title,
                                    color = alertColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detail,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Test Connection Button
                    OutlinedButton(
                        onClick = { runConnectionTest() },
                        enabled = !isTestingConnection && apiKeyInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonCyan,
                            disabledContentColor = TextMuted
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("test_connection_button")
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...", fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Test",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Connection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Save & Continue Button
                    val canContinue = validationResult is ApiValidationResult.Success || isAlreadyValidated
                    NeonButton(
                        text = "Save & Continue",
                        onClick = {
                            keyStorage.saveApiKey(apiKeyInput)
                            keyStorage.setValidationPassed(true)
                            onKeyValidatedAndSaved()
                        },
                        enabled = canContinue,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        testTag = "save_continue_button"
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Security & Privacy Disclosure Card
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyberSurfaceBorder
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Disclosure",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SECURITY & ARCHITECTURE DISCLOSURE",
                                color = TextCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Keystore Encryption: Your key is encrypted using AES-GCM in the hardware-backed Android Keystore and saved strictly to private local app storage.\n" +
                                    "• Zero Telemetry: API keys are never transmitted to third parties or logged in plain text.\n" +
                                    "• Client-Side Reality: Client-side Android apps can be decompiled; client keys cannot be made 100% secret against rooted inspection.\n" +
                                    "• Production Mode: For high-security enterprise deployments, integrate Firebase AI Logic backed by Firebase App Check attestation.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
