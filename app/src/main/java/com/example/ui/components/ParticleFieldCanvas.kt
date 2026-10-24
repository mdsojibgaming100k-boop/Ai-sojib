package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.random.Random

private data class Particle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float,
    val isCyan: Boolean
)

@Composable
fun ParticleFieldCanvas(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activityFactor: Float = 0.2f
) {
    if (!enabled) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberBackground, CyberBlack)
                )
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particle_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_loop"
    )

    val particles = remember {
        val rand = Random(42)
        List(40) {
            Particle(
                xRatio = rand.nextFloat(),
                yRatio = rand.nextFloat(),
                radius = rand.nextFloat() * 2.5f + 1.2f,
                speed = rand.nextFloat() * 0.4f + 0.1f,
                alpha = rand.nextFloat() * 0.5f + 0.2f,
                isCyan = rand.nextBoolean()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Deep cyber gradient backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF190F38),
                    CyberBackground,
                    CyberBlack
                ),
                center = Offset(size.width * 0.5f, size.height * 0.4f),
                radius = size.width * 0.85f
            )
        )

        // Draw animated cyber floating dust/sparks
        val w = size.width
        val h = size.height

        for (p in particles) {
            val dynamicSpeed = p.speed * (1f + activityFactor * 2f)
            val yPos = ((p.yRatio + animProgress * dynamicSpeed) % 1f) * h
            val xPos = (p.xRatio * w) + kotlin.math.sin((animProgress + p.xRatio) * 6.28f) * 15f
            val baseColor = if (p.isCyan) NeonCyan else NeonPurple
            val alpha = (p.alpha * (0.6f + activityFactor * 0.4f)).coerceIn(0.1f, 1f)

            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = p.radius * (1f + activityFactor * 0.5f),
                center = Offset(xPos, yPos)
            )
        }
    }
}
