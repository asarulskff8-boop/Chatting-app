package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MediaFile
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.ShimmerBase
import com.example.ui.theme.SkyBright
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkySoft
import com.example.util.MediaUtils
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitalPortalBackground(
    modifier: Modifier = Modifier,
    centerYFraction: Float = 0.44f
) {
    val infinite = rememberInfiniteTransition(label = "orbital")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.5f, size.height * centerYFraction)
        val maxR = maxOf(size.width, size.height) * 0.78f

        // Calm medium-white sky gradient (glare-free, readable contrast)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE3EEF9), LightBackground, Color(0xFFE5ECF4)),
                center = center,
                radius = maxR
            )
        )

        val ringRadii = listOf(
            size.minDimension * 0.24f,
            size.minDimension * 0.37f,
            size.minDimension * 0.52f
        )

        ringRadii.forEachIndexed { idx, r ->
            drawCircle(
                color = SkyPrimary.copy(alpha = 0.16f - idx * 0.03f),
                radius = r,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Outer dashed orbital circle in soft sky blue
        drawCircle(
            color = SkyBright.copy(alpha = 0.16f),
            radius = size.minDimension * 0.65f,
            center = center,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
            )
        )

        // Orbiting sky-blue & cyan accents
        val p1 = Offset(
            center.x + ringRadii[1] * cos(orbitAngle - 2.3f),
            center.y + ringRadii[1] * sin(orbitAngle - 2.3f)
        )
        drawCircle(color = SkySoft.copy(alpha = 0.85f * pulse), radius = 10.dp.toPx(), center = p1)
        drawCircle(color = SkyPrimary, radius = 4.dp.toPx(), center = p1)

        val p2 = Offset(
            center.x + ringRadii[0] * cos(-orbitAngle + 1.55f),
            center.y + ringRadii[0] * sin(-orbitAngle + 1.55f)
        )
        drawCircle(color = SkySoft, radius = 8.dp.toPx(), center = p2)
        drawCircle(color = CyanGlow, radius = 3.2.dp.toPx(), center = p2)

        val p3 = Offset(
            center.x + ringRadii[2] * cos(orbitAngle * 0.7f - 0.7f),
            center.y + ringRadii[2] * sin(orbitAngle * 0.7f - 0.7f)
        )
        drawCircle(color = SkyBright.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = p3)
        drawCircle(color = SkyDeep, radius = 3.5.dp.toPx(), center = p3)

        val p4 = Offset(
            center.x + (size.minDimension * 0.65f) * cos(-orbitAngle * 0.5f + 0.1f),
            center.y + (size.minDimension * 0.65f) * sin(-orbitAngle * 0.5f + 0.1f)
        )
        drawCircle(color = SkyBright, radius = 2.6.dp.toPx(), center = p4)
    }
}

@Composable
fun ChatSparkEmblem(
    size: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    val cornerShape = RoundedCornerShape((size.value * 0.28f).dp)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = (size.value * 0.12f).coerceIn(4f, 14f).dp,
                shape = cornerShape,
                ambientColor = SkyPrimary.copy(alpha = 0.35f),
                spotColor = SkyDeep.copy(alpha = 0.35f)
            )
            .clip(cornerShape)
            .border(
                width = (size.value * 0.03f).coerceIn(1.5f, 3f).dp,
                color = Color.White,
                shape = cornerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.chatting_app_logo_1795337395527),
            contentDescription = "Chatting App Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String = "",
    avatarMediaId: String = "",
    mediaCache: Map<String, MediaFile> = emptyMap(),
    onRequestMedia: ((String) -> Unit)? = null,
    size: Dp = 48.dp,
    showOnline: Boolean = false,
    online: Boolean = false,
    hasStoryRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cachedMedia = mediaCache[avatarMediaId]
    if (avatarMediaId.isNotBlank() && cachedMedia == null) {
        onRequestMedia?.invoke(avatarMediaId)
    }
    val bitmap: Bitmap? = remember(cachedMedia?.base64Data) {
        cachedMedia?.base64Data?.let { MediaUtils.decodeBase64ToBitmap(it) }
    }

    val ringModifier = if (hasStoryRing) {
        Modifier
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(listOf(SkyDeep, SkyPrimary, CyanGlow)),
                shape = CircleShape
            )
            .padding(3.dp)
    } else {
        Modifier
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(ringModifier)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(SkyPrimary, SkyBright))
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "$name avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                photoUrl.isNotBlank() -> {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "$name avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    val initials = name.trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifBlank { "?" }
                    Text(
                        text = initials,
                        color = Color.White,
                        style = if (size >= 56.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (showOnline && online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.27f).coerceAtLeast(10.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(EmeraldOnline)
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SkeletonConversationList(count: Int = 5) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(ShimmerBase)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(ShimmerBase)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonPostList(count: Int = 3) {
    val infinite = rememberInfiniteTransition(label = "postShimmer")
    val alpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "postAlpha"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ShimmerBase)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ShimmerBase)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ShimmerBase)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
fun SkeletonMessageList(count: Int = 5) {
    val infinite = rememberInfiniteTransition(label = "msgShimmer")
    val alpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "msgAlpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(alpha),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) { idx ->
            val isMine = idx % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isMine) SkySoft else LightSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.width(if (idx % 3 == 0) 210.dp else 160.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ShimmerBase)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing?.invoke()
    }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Now"
    val date = timestamp.toDate()
    val diffMs = System.currentTimeMillis() - date.time
    val mins = diffMs / 60_000L
    return when {
        mins < 1 -> "Now"
        mins < 60 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        mins < 24 * 60 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        mins < 7 * 24 * 60 -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

fun formatCallDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
