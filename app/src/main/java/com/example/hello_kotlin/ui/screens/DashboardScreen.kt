package com.example.hello_kotlin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.hello_kotlin.data.db.dao.TransactionWithTag
import com.example.hello_kotlin.ui.components.TransactionDetailsDialog
import com.example.hello_kotlin.ui.theme.*
import com.example.hello_kotlin.viewmodel.DashboardViewModel
import com.example.hello_kotlin.viewmodel.TagViewModel
import com.example.hello_kotlin.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    transactionViewModel: TransactionViewModel,
    tagViewModel: TagViewModel,
    autoTagViewModel: com.example.hello_kotlin.viewmodel.AutoTagViewModel
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val totalExpenditure by viewModel.totalExpenditure.collectAsState()
    val topExpenditure by viewModel.topExpenditure.collectAsState()
    val tagBreakdown by viewModel.tagBreakdown.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val untaggedCount by viewModel.untaggedCount.collectAsState()
    val allTags by tagViewModel.allTags.collectAsState()
    val dailySpending by viewModel.dailySpending.collectAsState()
    val activePeriod by viewModel.activePeriod.collectAsState()
    val yearlySpending by viewModel.yearlySpending.collectAsState()

    val monthFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
    val formattedMonth = remember(selectedMonth) { selectedMonth.format(monthFormatter) }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val availableMonths = remember {
        val current = java.time.YearMonth.now()
        (0..11).map { current.minusMonths(it.toLong()) }
    }

    var showTagDialog by remember { mutableStateOf<TransactionWithTag?>(null) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    var showPassbookSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val prefsTemp = context.getSharedPreferences("expense_tracker", android.content.Context.MODE_PRIVATE)
        val trigger = prefsTemp.getBoolean("trigger_passbook_sheet", false)
        if (trigger) {
            showPassbookSheet = true
            prefsTemp.edit().remove("trigger_passbook_sheet").apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Top Header - Display Tab Name & Dark Mode Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Home",
                color = StitchPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (AppThemeState.isDark) "🌙" else "☀️",
                    fontSize = 16.sp
                )
                Switch(
                    checked = AppThemeState.isDark,
                    onCheckedChange = { isChecked ->
                        AppThemeState.isDark = isChecked
                        context.getSharedPreferences("expense_tracker", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("dark_mode", isChecked)
                            .apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StitchPrimary,
                        checkedTrackColor = StitchPrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = StitchOutline,
                        uncheckedTrackColor = StitchSurfaceContainer
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector & Period Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month selector dropdown trigger look
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(StitchSurfaceContainer)
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Spent this",
                            color = StitchOnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formattedMonth,
                            color = StitchOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "▼",
                            color = StitchOutline,
                            fontSize = 10.sp
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        availableMonths.forEach { month ->
                            val monthLabel = month.format(monthFormatter)
                            DropdownMenuItem(
                                text = { Text(text = monthLabel) },
                                onClick = {
                                    dropdownExpanded = false
                                    viewModel.selectMonth(month)
                                }
                            )
                        }
                    }
                }

                // Week / Month / Year toggles
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceContainerLow)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("Week", "Month", "Year").forEach { period ->
                        val isSelected = activePeriod == period
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) StitchPrimary else Color.Transparent)
                                .clickable { viewModel.selectPeriod(period) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = period,
                                color = if (isSelected) Color.White else StitchOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bento Summary Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Spent Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                    border = BorderStroke(1.dp, StitchSurfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StitchSurfaceContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Spent",
                                    tint = StitchPrimary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            Text(
                                text = "Total Spent",
                                color = StitchOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currencyFormat.format(totalExpenditure),
                            color = StitchOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top Expense Card
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                    border = BorderStroke(1.dp, StitchSurfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StitchSurfaceContainerHighest)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = "Top Expense",
                                    tint = StitchSecondaryContainer,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            Text(
                                text = "Top Expense",
                                color = StitchOnSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (topExpenditure != null) {
                                topExpenditure!!.tagName ?: topExpenditure!!.accountName ?: "Unknown"
                            } else "—",
                            color = StitchOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (topExpenditure != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currencyFormat.format(topExpenditure!!.amount),
                                color = StitchPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Main Expense Donut Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                border = BorderStroke(1.dp, StitchSurfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                var selectedSliceIndex by remember { mutableStateOf(-1) }

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedSliceIndex = -1
                        }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Donut Chart Container
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Parse slice segments
                        val segments = remember(tagBreakdown) {
                            val total = tagBreakdown.sumOf { it.totalAmount }.toFloat()
                            if (total == 0f) emptyList()
                            else {
                                var currentSum = 0f
                                tagBreakdown.mapIndexed { idx, breakdown ->
                                    val sweep = (breakdown.totalAmount.toFloat() / total) * 360f
                                    val start = currentSum
                                    currentSum += sweep
                                    Triple(start, sweep, idx)
                                }
                            }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(tagBreakdown) {
                                    detectTapGestures { offset ->
                                        val x = offset.x - size.width / 2f
                                        val y = offset.y - size.height / 2f
                                        val distance = kotlin.math.sqrt(x * x + y * y)
                                        val outerRadius = size.width / 2f
                                        val strokeWidthPx = 24.dp.toPx()
                                        val innerRadius = outerRadius - strokeWidthPx

                                        if (distance > outerRadius) {
                                            selectedSliceIndex = -1
                                        } else if (distance >= innerRadius) {
                                            var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                                            if (angle < 0) angle += 360f

                                            // Find segment matching the angle
                                            val clickedSegment = segments.find { (start, sweep, _) ->
                                                if (start + sweep > 360f) {
                                                    angle >= start || angle <= (start + sweep - 360f)
                                                } else {
                                                    angle >= start && angle <= (start + sweep)
                                                }
                                            }

                                            selectedSliceIndex = clickedSegment?.third ?: -1
                                        }
                                    }
                                }
                        ) {
                            val strokeWidth = 24.dp.toPx()
                            val sizeOffset = strokeWidth
                            val innerSize = Size(size.width - sizeOffset, size.height - sizeOffset)
                            val startOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                            // Background Circle
                            drawArc(
                                color = StitchSurfaceContainerLow,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = startOffset,
                                size = innerSize,
                                style = Stroke(width = strokeWidth)
                            )

                            // Slices
                            tagBreakdown.forEachIndexed { index, breakdown ->
                                val (start, sweep, _) = segments.getOrNull(index) ?: return@forEachIndexed
                                val isSelected = selectedSliceIndex == index
                                val color = try {
                                    Color(android.graphics.Color.parseColor(breakdown.tagColor ?: "#667EEA"))
                                } catch (e: Exception) {
                                    StitchPrimary
                                }

                                drawArc(
                                    color = color,
                                    startAngle = start,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = startOffset,
                                    size = innerSize,
                                    style = Stroke(
                                        width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }

                        // Donut Center Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            val activeItem = tagBreakdown.getOrNull(selectedSliceIndex)
                            val centerLabel = when {
                                selectedSliceIndex == -1 -> "Total Spent"
                                activeItem?.tagName == null -> "Untagged"
                                else -> activeItem.tagName
                            }
                            Text(
                                text = centerLabel,
                                color = StitchOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(activeItem?.totalAmount ?: totalExpenditure),
                                color = StitchOnSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Floating Percentage Badges
                        val totalAmount = remember(tagBreakdown) { tagBreakdown.sumOf { it.totalAmount } }
                        if (totalAmount > 0.0) {
                            tagBreakdown.forEachIndexed { index, breakdown ->
                                val (start, sweep, _) = segments.getOrNull(index) ?: return@forEachIndexed
                                val percentage = ((breakdown.totalAmount / totalAmount) * 100).toInt()

                                if (percentage > 5) {
                                    val midAngleRad = Math.toRadians((start + sweep / 2).toDouble())
                                    val radiusDp = 96.dp
                                    val badgeX = 108.dp + (radiusDp * Math.cos(midAngleRad).toFloat())
                                    val badgeY = 108.dp + (radiusDp * Math.sin(midAngleRad).toFloat())

                                    val color = try {
                                        Color(android.graphics.Color.parseColor(breakdown.tagColor ?: "#667EEA"))
                                    } catch (e: Exception) {
                                        StitchPrimary
                                    }

                                    val badgeBg = if (AppThemeState.isDark) Color(0xFF070709) else Color(0xFFF1F3FD)
                                    val badgeBorderColor = if (AppThemeState.isDark) color.copy(alpha = 0.8f) else color.copy(alpha = 0.35f)
                                    val badgeBorderWidth = if (AppThemeState.isDark) 1.5.dp else 1.dp
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = badgeX - 24.dp, y = badgeY - 12.dp)
                                            .background(
                                                color = badgeBg,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = badgeBorderWidth,
                                                color = badgeBorderColor,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(color = color, shape = CircleShape)
                                            )
                                            Text(
                                                text = "$percentage%",
                                                color = if (AppThemeState.isDark) Color.White else StitchOnSurface,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spending Categories list underneath
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Spending Categories",
                            color = StitchOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        val chunkedCategories = remember(tagBreakdown) { tagBreakdown.chunked(2) }

                        chunkedCategories.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val categoryColor = try {
                                        Color(android.graphics.Color.parseColor(item.tagColor ?: "#4648d4"))
                                    } catch (e: Exception) {
                                        StitchPrimary
                                    }
                                    val itemIndex = tagBreakdown.indexOf(item)
                                    val isSelected = selectedSliceIndex == itemIndex
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) categoryColor.copy(alpha = 0.15f)
                                                else StitchSurfaceContainerLow
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) categoryColor else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selectedSliceIndex = if (isSelected) -1 else itemIndex
                                            }
                                            .padding(12.dp)
                                            .height(72.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val total = tagBreakdown.sumOf { it.totalAmount }
                                        val percent = if (total > 0) ((item.totalAmount / total) * 100).toInt() else 0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = currencyFormat.format(item.totalAmount),
                                                color = StitchOnSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$percent%",
                                                color = StitchOnSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(categoryColor)
                                            ) {
                                                Text(
                                                    text = item.tagEmoji ?: "🏷️",
                                                    modifier = Modifier.align(Alignment.Center),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = item.tagName ?: "Unknown",
                                                color = StitchOnSurface,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Daily Trend Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                border = BorderStroke(1.dp, StitchSurfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Daily Trend",
                                color = StitchOnSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (activePeriod) {
                                    "Week" -> "Last 7 days behavior"
                                    "Month" -> "Daily spending trend"
                                    else -> "Monthly spending trend"
                                },
                                color = StitchOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "📈",
                            fontSize = 16.sp
                        )
                    }

                    // Bar Chart Section
                    var activeBarIndex by remember { mutableStateOf(-1) }

                    // Reset selected index when activePeriod changes
                    LaunchedEffect(activePeriod) {
                        activeBarIndex = -1
                    }

                    when (activePeriod) {
                        "Week" -> {
                            val maxSpend = remember(dailySpending) {
                                dailySpending.maxOfOrNull { it.totalAmount } ?: 1.0
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                repeat(7) { index ->
                                    val item = dailySpending.getOrNull(index)
                                    val daySpend = item?.totalAmount ?: 0.0
                                    val label = item?.date?.let { date ->
                                        date.format(java.time.format.DateTimeFormatter.ofPattern("EEE", Locale.US)).uppercase()
                                    } ?: when (index) {
                                        0 -> "MON"; 1 -> "TUE"; 2 -> "WED"; 3 -> "THU"; 4 -> "FRI"; 5 -> "SAT"; else -> "SUN"
                                    }
                                    val ratio = if (maxSpend > 0) (daySpend / maxSpend).toFloat() else 0f
                                    val isSelected = activeBarIndex == index

                                    val chartHeight = 80.dp
                                    val barHeight = (ratio.coerceIn(0.1f, 1.0f) * chartHeight.value).dp

                                    val animatedBarHeight by androidx.compose.animation.core.animateDpAsState(
                                        targetValue = if (isSelected) (barHeight - 12.dp).coerceAtLeast(8.dp) else barHeight,
                                        label = "barHeight"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .height(animatedBarHeight)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        .background(if (isSelected) StitchPrimary else StitchSurfaceContainerHigh)
                                                        .clickable {
                                                            activeBarIndex = if (isSelected) -1 else index
                                                        }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                color = StitchOutline,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isSelected,
                                            enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                                            exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .wrapContentWidth(unbounded = true)
                                                .offset(y = -(animatedBarHeight + 22.dp))
                                        ) {
                                            val tooltipBg = if (AppThemeState.isDark) Color(0xFF070709) else StitchOnSurface
                                            val tooltipBorderModifier = if (AppThemeState.isDark) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = StitchPrimary.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                            } else Modifier
                                            Box(
                                                modifier = Modifier
                                                    .wrapContentWidth(unbounded = true)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(tooltipBg)
                                                    .then(tooltipBorderModifier)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = currencyFormat.format(daySpend),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Month" -> {
                            val maxSpend = remember(dailySpending) {
                                dailySpending.maxOfOrNull { it.totalAmount } ?: 1.0
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                dailySpending.forEachIndexed { index, item ->
                                    val daySpend = item.totalAmount
                                    val label = item.day.toString()
                                    val ratio = if (maxSpend > 0) (daySpend / maxSpend).toFloat() else 0f
                                    val isSelected = activeBarIndex == index

                                    val chartHeight = 80.dp
                                    val barHeight = (ratio.coerceIn(0.1f, 1.0f) * chartHeight.value).dp

                                    val animatedBarHeight by androidx.compose.animation.core.animateDpAsState(
                                        targetValue = if (isSelected) (barHeight - 12.dp).coerceAtLeast(8.dp) else barHeight,
                                        label = "barHeight"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(120.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .height(animatedBarHeight)
                                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(if (isSelected) StitchPrimary else StitchSurfaceContainerHigh)
                                                        .clickable {
                                                            activeBarIndex = if (isSelected) -1 else index
                                                        }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                color = StitchOutline,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isSelected,
                                            enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                                            exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .wrapContentWidth(unbounded = true)
                                                .offset(y = -(animatedBarHeight + 22.dp))
                                        ) {
                                            val tooltipBg = if (AppThemeState.isDark) Color(0xFF070709) else StitchOnSurface
                                            val tooltipBorderModifier = if (AppThemeState.isDark) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = StitchPrimary.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                            } else Modifier
                                            Box(
                                                modifier = Modifier
                                                    .wrapContentWidth(unbounded = true)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(tooltipBg)
                                                    .then(tooltipBorderModifier)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = currencyFormat.format(daySpend),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Year" -> {
                            val monthsSpends = remember(yearlySpending) {
                                (1..12).map { m ->
                                    val amount = yearlySpending.getOrNull(m - 1) ?: 0.0
                                    val monthName = java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT, Locale.US).uppercase()
                                    Pair(monthName, amount)
                                }
                            }
                            val maxSpend = remember(monthsSpends) {
                                monthsSpends.maxOfOrNull { it.second } ?: 1.0
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                monthsSpends.forEachIndexed { index, (monthName, monthSpend) ->
                                    val ratio = if (maxSpend > 0) (monthSpend / maxSpend).toFloat() else 0f
                                    val isSelected = activeBarIndex == index

                                    val chartHeight = 80.dp
                                    val barHeight = (ratio.coerceIn(0.1f, 1.0f) * chartHeight.value).dp

                                    val animatedBarHeight by androidx.compose.animation.core.animateDpAsState(
                                        targetValue = if (isSelected) (barHeight - 12.dp).coerceAtLeast(8.dp) else barHeight,
                                        label = "barHeight"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .height(animatedBarHeight)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        .background(if (isSelected) StitchPrimary else StitchSurfaceContainerHigh)
                                                        .clickable {
                                                            activeBarIndex = if (isSelected) -1 else index
                                                        }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = monthName,
                                                color = StitchOutline,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isSelected,
                                            enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                                            exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .wrapContentWidth(unbounded = true)
                                                .offset(y = -(animatedBarHeight + 22.dp))
                                        ) {
                                            val tooltipBg = if (AppThemeState.isDark) Color(0xFF070709) else StitchOnSurface
                                            val tooltipBorderModifier = if (AppThemeState.isDark) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = StitchPrimary.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                            } else Modifier
                                            Box(
                                                modifier = Modifier
                                                    .wrapContentWidth(unbounded = true)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(tooltipBg)
                                                    .then(tooltipBorderModifier)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = currencyFormat.format(monthSpend),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


        }
    }

    showTagDialog?.let { tx ->
        TransactionDetailsDialog(
            transaction = tx,
            allTags = allTags,
            onTagSelected = { tagId ->
                transactionViewModel.tagTransaction(tx.id, tagId)
            },
            onDeleteTransaction = {
                transactionViewModel.deleteTransaction(tx.id)
            },
            onDismiss = {
                showTagDialog = null
            }
        )
    }

    if (showPassbookSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPassbookSheet = false },
            containerColor = StitchSurfaceContainerLowest,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                com.example.hello_kotlin.ui.components.PassbookSettingsContent(
                    autoTagViewModel = autoTagViewModel,
                    onImportComplete = { showPassbookSheet = false }
                )
            }
        }
    }
}
