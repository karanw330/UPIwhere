package com.example.hello_kotlin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.data.db.dao.TagTotal
import com.example.hello_kotlin.ui.theme.TextPrimary
import com.example.hello_kotlin.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun ExpensePieChart(
    tagTotals: List<TagTotal>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    var selectedIndex by remember { mutableStateOf(-1) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    LaunchedEffect(tagTotals) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val defaultColors = listOf(
        Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFF00C9A7),
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFA07A),
        Color(0xFFDDA0DD), Color(0xFF87CEEB), Color(0xFFFFD700),
        Color(0xFF98FB98)
    )

    val displayTotal = remember(tagTotals, totalAmount) {
        if (totalAmount > 0.0) totalAmount else tagTotals.sumOf { it.totalAmount }
    }

    if (tagTotals.isEmpty() || displayTotal <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses yet",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                selectedIndex = -1
            }
    ) {
        // Donut Chart
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .pointerInput(tagTotals) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius * 0.6f

                            if (distance > outerRadius) {
                                selectedIndex = -1
                            } else if (distance >= innerRadius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                angle = (angle + 360 + 90) % 360 // Normalize from top

                                var cumAngle = 0f
                                tagTotals.forEachIndexed { index, item ->
                                    val sweep = (item.totalAmount / displayTotal * 360).toFloat()
                                    if (angle >= cumAngle && angle < cumAngle + sweep) {
                                        selectedIndex = if (selectedIndex == index) -1 else index
                                        return@detectTapGestures
                                    }
                                    cumAngle += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = 40.dp.toPx()
                val padding = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(padding, padding)

                var startAngle = -90f

                tagTotals.forEachIndexed { index, item ->
                    val sweepAngle = (item.totalAmount / displayTotal * 360).toFloat() * animationProgress.value
                    val color = item.tagColor?.let { parseColor(it) }
                        ?: defaultColors[index % defaultColors.size]

                    val isSelected = selectedIndex == index
                    val currentStrokeWidth = if (isSelected) strokeWidth * 1.25f else strokeWidth

                    drawArc(
                        color = if (isSelected) color else color.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f, // Gap between slices
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedIndex >= 0 && selectedIndex < tagTotals.size) {
                    val selected = tagTotals[selectedIndex]
                    Text(
                        text = "${selected.tagEmoji ?: "📦"} ${selected.tagName ?: "Untagged"}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currencyFormat.format(selected.totalAmount),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val pct = (selected.totalAmount / displayTotal * 100).toInt()
                    Text(
                        text = "$pct%",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Total",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = currencyFormat.format(displayTotal),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Legend
        val chunked = tagTotals.chunked(2)
        chunked.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEachIndexed { idx, item ->
                    val globalIdx = chunked.indexOf(row) * 2 + idx
                    val color = item.tagColor?.let { parseColor(it) }
                        ?: defaultColors[globalIdx % defaultColors.size]
                    val pct = (item.totalAmount / displayTotal * 100).toInt()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.tagEmoji ?: ""} ${item.tagName ?: "Untagged"} ($pct%)",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Pad if odd number
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val colorStr = hex.removePrefix("#")
        val colorLong = colorStr.toLong(16)
        when (colorStr.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> Color(0xFF667EEA)
        }
    } catch (e: Exception) {
        Color(0xFF667EEA)
    }
}
