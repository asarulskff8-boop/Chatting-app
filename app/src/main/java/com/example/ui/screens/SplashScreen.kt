package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChatSparkEmblem
import com.example.ui.components.OrbitalPortalBackground
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SkySoft
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(600))
        delay(950)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitalPortalBackground(
            modifier = Modifier.fillMaxSize(),
            centerYFraction = 0.45f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.scale(scale.value)) {
                ChatSparkEmblem(size = 96.dp)
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Chatting App",
                style = MaterialTheme.typography.displayMedium.copy(
                    letterSpacing = 0.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(alpha.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connect Beyond Messages.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha.value)
            )
        }

        Surface(
            shape = CircleShape,
            color = SkySoft.copy(alpha = 0.9f * alpha.value),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(alpha.value)
        ) {
            Text(
                text = "ENCRYPTED • REAL-TIME • SOCIAL",
                style = MaterialTheme.typography.labelSmall,
                color = SkyDeep,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
