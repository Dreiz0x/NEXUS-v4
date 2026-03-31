package com.nexus.intelligence.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import kotlin.math.cos
import kotlin.math.sin

// ── Animated Grid Background ─────────────────────────────────────

@Composable
fun HudGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexusColors.Black)
            .drawBehind {
                drawGrid(this)
                drawScanLine(this, scanLineY)
            }
    ) {
        content()
    }
}

private fun drawGrid(drawScope: DrawScope) {
    val gridSpacing = 40f
    val gridColor = NexusColors.GridLine

    with(drawScope) {
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.5f
            )
            x += gridSpacing
        }

        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5f
            )
            y += gridSpacing
        }
    }
}

private fun drawScanLine(drawScope: DrawScope, progress: Float) {
    with(drawScope) {
        val y = size.height * progress
        val gradient = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                NexusColors.CyanGlow,
                NexusColors.Cyan.copy(alpha = 0.15f),
                NexusColors.CyanGlow,
                Color.Transparent
            )
        )
        drawRect(
            brush = gradient,
            topLeft = Offset(0f, y - 20f),
            size = Size(size.width, 40f)
        )
    }
}

// ── Holographic Card ─────────────────────────────────────────────

@Composable
fun HolographicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NexusColors.Cyan,
    glowColor: Color = NexusColors.CyanGlow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cardGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(NexusColors.CardBackground)
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = glowAlpha),
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(12.dp),
        content = content
    )
}

// ── Stat Display Panel ───────────────────────────────────────────

@Composable
fun StatPanel(
    label: String,
    value: String,
    color: Color = NexusColors.Cyan,
    modifier: Modifier = Modifier
) {
    HolographicCard(
        modifier = modifier,
        borderColor = color
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Animated Radar ───────────────────────────────────────────────

@Composable
fun AnimatedRadar(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    size: Dp = 120.dp,
    color: Color = NexusColors.Cyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = this.size.minDimension / 2

        // Concentric circles
        for (i in 1..4) {
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = radius * (i / 4f),
                center = center,
                style = Stroke(width = 1f)
            )
        }

        // Cross lines
        drawLine(
            color = color.copy(alpha = 0.1f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, this.size.height),
            strokeWidth = 0.5f
        )
        drawLine(
            color = color.copy(alpha = 0.1f),
            start = Offset(0f, center.y),
            end = Offset(this.size.width, center.y),
            strokeWidth = 0.5f
        )

        if (isActive) {
            // Sweeping beam
            rotate(rotation, pivot = center) {
                val sweepBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.05f to color.copy(alpha = 0.3f),
                    0.1f to color.copy(alpha = pulseAlpha * 0.5f),
                    0.15f to Color.Transparent,
                    1.0f to Color.Transparent,
                    center = center
                )
                drawCircle(
                    brush = sweepBrush,
                    radius = radius,
                    center = center
                )

                // Sweep line
                drawLine(
                    color = color.copy(alpha = 0.8f),
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 2f
                )
            }

            // Center dot
            drawCircle(
                color = color,
                radius = 3f,
                center = center
            )
        }

        // Outer ring
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

// ── HUD Progress Bar ─────────────────────────────────────────────

@Composable
fun HudProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NexusColors.Cyan,
    label: String? = null,
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "progressGlow")
    val glowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowOffset"
    )

    Column(modifier = modifier) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            // Background track
            drawRoundRect(
                color = color.copy(alpha = 0.1f),
                size = Size(size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Progress fill
            val progressWidth = size.width * animatedProgress
            if (progressWidth > 0) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.6f),
                            color,
                            color.copy(alpha = 0.8f)
                        )
                    ),
                    size = Size(progressWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // Glow effect at the tip
                if (animatedProgress < 1f) {
                    drawCircle(
                        color = color.copy(alpha = 0.4f),
                        radius = size.height * 1.5f,
                        center = Offset(progressWidth, size.height / 2)
                    )
                }
            }

            // Tick marks
            val tickSpacing = size.width / 20
            for (i in 0..20) {
                val x = tickSpacing * i
                drawLine(
                    color = color.copy(alpha = 0.15f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 0.5f
                )
            }
        }
    }
}

// ── Status Indicator ─────────────────────────────────────────────

@Composable
fun StatusIndicator(
    isOnline: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    val color = if (isOnline) NexusColors.Green else NexusColors.Red

    val infiniteTransition = rememberInfiniteTransition(label = "statusBlink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isOnline) 2000 else 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(
                color = color.copy(alpha = alpha * 0.3f),
                radius = size.minDimension / 2 * 1.5f
            )
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = size.minDimension / 2
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = alpha),
            fontFamily = NexusMonospace,
            letterSpacing = 1.sp
        )
    }
}

// ── Section Header ───────────────────────────────────────────────

@Composable
fun HudSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = NexusColors.Cyan
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left bracket
        Canvas(modifier = Modifier.size(12.dp, 16.dp)) {
            drawLine(
                color = color,
                start = Offset(size.width, 0f),
                end = Offset(0f, 0f),
                strokeWidth = 2f
            )
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = color,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Decorative line
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
        ) {
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1f
            )
        }
    }
}

// ── Document Type Badge ──────────────────────────────────────────

@Composable
fun DocumentTypeBadge(
    type: String,
    modifier: Modifier = Modifier
) {
    val color = when (type.uppercase()) {
        "PDF" -> NexusColors.PdfColor
        "WORD" -> NexusColors.WordColor
        "EXCEL" -> NexusColors.ExcelColor
        "POWERPOINT" -> NexusColors.PowerPointColor
        "IMAGE" -> NexusColors.ImageColor
        "TEXT" -> NexusColors.TextColor
        "CSV" -> NexusColors.CsvColor
        else -> NexusColors.TextSecondary
    }

    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
