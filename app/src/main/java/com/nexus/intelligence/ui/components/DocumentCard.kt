package com.nexus.intelligence.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.SearchResult
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import java.text.SimpleDateFormat
import java.util.*

// ── Document Card (Holographic Style) ────────────────────────────

@Composable
fun DocumentCard(
    document: DocumentInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val typeColor = getDocTypeColor(document.fileType)

    val infiniteTransition = rememberInfiniteTransition(label = "docCard")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NexusColors.CardBackground)
            .border(1.dp, typeColor.copy(alpha = borderAlpha), shape)
            .clickable { onClick() }
            .drawBehind {
                // Top accent line
                drawLine(
                    color = typeColor.copy(alpha = 0.6f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.3f, 0f),
                    strokeWidth = 2f
                )
                // Corner accent
                drawLine(
                    color = typeColor.copy(alpha = 0.3f),
                    start = Offset(size.width - 30f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
            .padding(12.dp)
    ) {
        // Header row: type badge + filename
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentTypeBadge(type = document.fileType)
            Text(
                text = document.fileSizeFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = NexusColors.TextDim
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filename
        Text(
            text = document.fileName,
            style = MaterialTheme.typography.titleMedium,
            color = NexusColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Content preview
        if (document.contentPreview.isNotBlank()) {
            Text(
                text = document.contentPreview,
                style = MaterialTheme.typography.bodySmall,
                color = NexusColors.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Metadata row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Path
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = NexusColors.TextDim
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = document.parentDirectory.takeLast(30),
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = NexusColors.TextDim
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTimestamp(document.lastModified),
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusColors.TextDim
                )
            }

            if (document.pageCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = NexusColors.TextDim
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${document.pageCount}p",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusColors.TextDim
                    )
                }
            }
        }
    }
}

// ── Search Result Card ───────────────────────────────────────────

@Composable
fun SearchResultCard(
    result: SearchResult,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val typeColor = getDocTypeColor(result.document.fileType)

    HolographicCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = typeColor,
        onClick = onClick
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DocumentTypeBadge(type = result.document.fileType)
                Text(
                    text = result.searchType,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (result.searchType == "SEMANTIC") NexusColors.Magenta else NexusColors.Cyan
                )
            }

            // Relevance score
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RELEVANCE ",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusColors.TextDim
                )
                Text(
                    text = "${result.relevancePercentage}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        result.relevancePercentage >= 80 -> NexusColors.Green
                        result.relevancePercentage >= 50 -> NexusColors.Amber
                        else -> NexusColors.Red
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filename
        Text(
            text = result.document.fileName,
            style = MaterialTheme.typography.titleMedium,
            color = NexusColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Matched snippet
        Text(
            text = result.matchedSnippet,
            style = MaterialTheme.typography.bodySmall,
            color = NexusColors.TextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Footer metadata
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = result.document.parentDirectory.takeLast(40),
                style = MaterialTheme.typography.labelSmall,
                color = NexusColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = result.document.fileSizeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = NexusColors.TextDim
            )
        }
    }
}

// ── HUD Search Bar ───────────────────────────────────────────────

@Composable
fun HudSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search your documents with natural language…"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "searchBar")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searchBorder"
    )

    val shape = RoundedCornerShape(4.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NexusColors.CardBackground)
            .border(1.dp, NexusColors.Cyan.copy(alpha = borderAlpha), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = NexusColors.Cyan,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontFamily = NexusMonospace,
                fontSize = 14.sp,
                color = NexusColors.TextPrimary,
                letterSpacing = 0.5.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(NexusColors.Cyan),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = NexusMonospace,
                            fontSize = 14.sp,
                            color = NexusColors.TextDim,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                innerTextField()
            }
        )

        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "[ENTER]",
                style = MaterialTheme.typography.labelSmall,
                color = NexusColors.Cyan.copy(alpha = 0.5f),
                modifier = Modifier.clickable { onSearch() }
            )
        }
    }
}

// ── Helper Functions ─────────────────────────────────────────────

fun getDocTypeColor(type: String): Color {
    return when (type.uppercase()) {
        "PDF" -> NexusColors.PdfColor
        "WORD" -> NexusColors.WordColor
        "EXCEL" -> NexusColors.ExcelColor
        "POWERPOINT" -> NexusColors.PowerPointColor
        "IMAGE" -> NexusColors.ImageColor
        "TEXT" -> NexusColors.TextColor
        "CSV" -> NexusColors.CsvColor
        else -> NexusColors.TextSecondary
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatTimestampRelative(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "${seconds}s ago"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> formatTimestamp(timestamp)
    }
}
