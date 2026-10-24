package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CharacterStyle
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.voice.SpeechState

@Composable
fun AssistantOrbAndCharacter(
    modifier: Modifier = Modifier,
    characterStyle: CharacterStyle = CharacterStyle.CYBER_SOJIB,
    speechState: SpeechState = SpeechState.IDLE,
    audioRms: Float = 0f,
    sizeDp: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Slow continuous holographic ring rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_spin"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Active state scale modifier
    val stateScale = when (speechState) {
        SpeechState.LISTENING -> 1.0f + (audioRms * 0.18f)
        SpeechState.SPEAKING -> breathingPulse * 1.06f
        SpeechState.PROCESSING -> 1.02f
        SpeechState.ERROR -> 0.98f
        SpeechState.IDLE -> breathingPulse
    }

    val stateColor = when (speechState) {
        SpeechState.LISTENING -> NeonCyan
        SpeechState.SPEAKING -> NeonPurple
        SpeechState.PROCESSING -> Color(0xFF00F5D4)
        SpeechState.ERROR -> CyberRed
        SpeechState.IDLE -> NeonCyan
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .scale(stateScale),
        contentAlignment = Alignment.Center
    ) {
        // Holographic Rings Canvas
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // 1. Ambient outer aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stateColor.copy(alpha = if (speechState == SpeechState.LISTENING) 0.35f else 0.15f),
                        DeepViolet.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Rotating segmented outer HUD ring
            drawCircle(
                color = stateColor.copy(alpha = 0.7f),
                radius = maxRadius - 10f,
                center = center,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f, 60f, 15f), rotationAngle)
                )
            )

            // 3. Counter-rotating inner dotted ring
            drawCircle(
                color = NeonPurple.copy(alpha = 0.6f),
                radius = maxRadius - 24f,
                center = center,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), -rotationAngle * 1.5f)
                )
            )

            // If Neural Core mode, draw intricate inner concentric geometry
            if (characterStyle == CharacterStyle.NEURAL_CORE) {
                drawCircle(
                    color = stateColor.copy(alpha = 0.85f),
                    radius = maxRadius - 48f,
                    center = center,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f), rotationAngle * 2f)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(stateColor, DeepViolet, Color(0xFF0B0716)),
                        center = center,
                        radius = maxRadius - 55f
                    ),
                    radius = maxRadius - 55f,
                    center = center
                )
            }
        }

        // Center Content: Character Avatar or Procedural Mecha Core
        when (characterStyle) {
            CharacterStyle.CYBER_SOJIB, CharacterStyle.MECHA_AI -> {
                Box(
                    modifier = Modifier
                        .size(sizeDp - 48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0819))
                        .border(2.dp, stateColor.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sojib_avatar),
                        contentDescription = "SOJIB AI Assistant Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(sizeDp - 48.dp)
                    )

                    // Subtle cyan scanning line overlay when active
                    if (speechState == SpeechState.PROCESSING || speechState == SpeechState.LISTENING) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            stateColor.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
            CharacterStyle.NEURAL_CORE -> {
                // Procedural quantum core center
                Box(
                    modifier = Modifier
                        .size(sizeDp - 64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    stateColor.copy(alpha = 0.9f),
                                    DeepViolet,
                                    Color(0xFF0B0716)
                                )
                            )
                        )
                        .border(1.5.dp, stateColor, CircleShape)
                )
            }
        }
    }
}
