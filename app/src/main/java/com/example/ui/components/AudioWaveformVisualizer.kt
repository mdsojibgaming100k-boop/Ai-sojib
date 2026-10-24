package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun AudioWaveformVisualizer(
    modifier: Modifier = Modifier,
    audioRms: Float = 0f,
    barCount: Int = 11,
    height: Dp = 36.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_ambient")
    val waveA by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_a"
    )
    val waveB by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_b"
    )

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val middleIndex = barCount / 2
        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - middleIndex).toFloat() / middleIndex
            val centerWeight = 1f - (distFromCenter * 0.45f)

            val baseAnim = if (i % 2 == 0) waveA else waveB
            val dynamicScale = (0.15f + (audioRms * 0.85f * centerWeight) + (baseAnim * 0.15f)).coerceIn(0.12f, 1f)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(dynamicScale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NeonCyan,
                                NeonPurple
                            )
                        )
                    )
            )
        }
    }
}
