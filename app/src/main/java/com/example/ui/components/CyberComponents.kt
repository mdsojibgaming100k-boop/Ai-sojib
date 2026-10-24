package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.voice.SpeechState

@Composable
fun StatusPill(
    speechState: SpeechState,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (dotColor, text) = when {
        isOffline -> Color.Gray to "OFFLINE // LOCAL MODE"
        speechState == SpeechState.LISTENING -> NeonCyan to "LISTENING // SPEECH INPUT"
        speechState == SpeechState.PROCESSING -> Color(0xFF00F5D4) to "NEURAL REASONING..."
        speechState == SpeechState.SPEAKING -> NeonPurple to "OUTPUTTING SPEECH..."
        speechState == SpeechState.ERROR -> CyberRed to "SYSTEM ALERT // ERROR"
        else -> CyberGreen to "ONLINE // SOJIB NEURAL CORE"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pill_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceElevated.copy(alpha = 0.85f))
            .border(1.dp, dotColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = dotAlpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = TextCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    testTag: String = "neon_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = CyberBlack,
            disabledContainerColor = CyberSurfaceElevated,
            disabledContentColor = TextMuted
        ),
        modifier = modifier
            .testTag(testTag)
            .border(
                width = 1.dp,
                color = if (enabled) NeonCyan else CyberSurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = CyberBlack,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CyberSurfaceBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurface.copy(alpha = 0.95f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}
