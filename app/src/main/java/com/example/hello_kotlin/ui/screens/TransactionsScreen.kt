package com.example.hello_kotlin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    dashboardViewModel: DashboardViewModel,
    transactionViewModel: TransactionViewModel,
    tagViewModel: TagViewModel
) {
    val selectedMonth by dashboardViewModel.selectedMonth.collectAsState()
    val allTransactions by dashboardViewModel.recentTransactions.collectAsState()
    val allTags by tagViewModel.allTags.collectAsState()
    val untaggedCount by dashboardViewModel.untaggedCount.collectAsState()

    val monthFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()) }
    val formattedMonth = remember(selectedMonth) { selectedMonth.format(monthFormatter) }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val availableMonths = remember {
        val current = java.time.YearMonth.now()
        (0..11).map { current.minusMonths(it.toLong()) }
    }

    var selectedFilter by remember { mutableStateOf<String?>(null) } // null = All, "untagged", or tag name
    var currentSort by remember { mutableStateOf("recent") } // "recent", "lowest", "highest"
    var showTagDialog by remember { mutableStateOf<TransactionWithTag?>(null) }
    val activePeriod by dashboardViewModel.activePeriod.collectAsState()
    var warningDismissed by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val filteredTransactions = when (selectedFilter) {
        null -> allTransactions
        "untagged" -> allTransactions.filter { !it.isTagged }
        else -> allTransactions.filter { it.tagName == selectedFilter }
    }

    val sortedTransactions = remember(filteredTransactions, currentSort) {
        when (currentSort) {
            "lowest" -> filteredTransactions.sortedBy { it.amount }
            "highest" -> filteredTransactions.sortedByDescending { it.amount }
            else -> filteredTransactions.sortedByDescending { it.timestamp }
        }
    }

    // Group transactions logically by relative date (TODAY, YESTERDAY, etc.) or sort header
    val groupedTransactions = remember(sortedTransactions, currentSort) {
        if (currentSort != "recent") {
            val title = when (currentSort) {
                "lowest" -> "LOWEST AMOUNT FIRST"
                "highest" -> "HIGHEST AMOUNT FIRST"
                else -> "SORTED"
            }
            linkedMapOf(title to sortedTransactions)
        } else {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val yesterday = today - 24 * 60 * 60 * 1000

            sortedTransactions.groupBy { tx ->
                when {
                    tx.timestamp >= today -> "TODAY"
                    tx.timestamp >= yesterday -> "YESTERDAY"
                    else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.timestamp)).uppercase()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        // Top Header - Display Tab Name and Sort Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                color = StitchPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            var sortMenuExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(StitchSurfaceContainer)
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sort:",
                        color = StitchOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (currentSort) {
                            "lowest" -> "Lowest"
                            "highest" -> "Highest"
                            else -> "Recent"
                        },
                        color = StitchPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "▼",
                        color = StitchOutline,
                        fontSize = 9.sp
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Recent (Default)") },
                        onClick = {
                            currentSort = "recent"
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Lowest Amount") },
                        onClick = {
                            currentSort = "lowest"
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Amount") },
                        onClick = {
                            currentSort = "highest"
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector & Period Toggles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                        dashboardViewModel.selectMonth(month)
                                    }
                                )
                            }
                        }
                    }

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
                                    .clickable { dashboardViewModel.selectPeriod(period) }
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
            }

            // Untagged warning box
            if (untaggedCount > 0 && !warningDismissed) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchErrorContainer),
                        border = BorderStroke(1.dp, StitchError.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "You have $untaggedCount untagged expense${if (untaggedCount > 1) "s" else ""}.",
                                    color = StitchError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { warningDismissed = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    color = StitchError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }



            // Categories Filter Chips Scrollable Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Chip
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = {
                            Text(
                                text = "All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = StitchSurfaceContainerLowest,
                            labelColor = StitchOnSurfaceVariant
                        )
                    )

                    // Untagged Chip
                    val untaggedCount = allTransactions.count { !it.isTagged }
                    if (untaggedCount > 0) {
                        FilterChip(
                            selected = selectedFilter == "untagged",
                            onClick = { selectedFilter = if (selectedFilter == "untagged") null else "untagged" },
                            label = {
                                Text(
                                    text = "Untagged ($untaggedCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StitchError,
                                selectedLabelColor = Color.White,
                                containerColor = StitchSurfaceContainerLowest,
                                labelColor = StitchError
                            )
                        )
                    }

                    // Mapped Category Chips
                    val usedTags = allTransactions
                        .filter { it.tagName != null }
                        .groupBy { it.tagName }
                        .toList()
                        .sortedByDescending { it.second.size }

                    usedTags.forEach { (tagName, txns) ->
                        FilterChip(
                            selected = selectedFilter == tagName,
                            onClick = { selectedFilter = if (selectedFilter == tagName) null else tagName },
                            label = {
                                Text(
                                    text = tagName ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StitchPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = StitchSurfaceContainerLowest,
                                labelColor = StitchOnSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Operations Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Operations",
                        color = StitchOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View All",
                        color = StitchPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Toggle full list view */ }
                    )
                }
            }

            // Grouped Transactions list
            if (groupedTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                        border = BorderStroke(1.dp, StitchSurfaceContainerLow)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📭", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No transactions found",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                groupedTransactions.forEach { (dateHeader, txns) ->
                    item {
                        Text(
                            text = dateHeader,
                            color = StitchOutline,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    itemsIndexed(txns, key = { index, tx -> "${tx.id}_${dateHeader}_$index" }) { index, tx ->
                        val isDebit = tx.type == "DEBIT"
                        val tagColor = try {
                            Color(android.graphics.Color.parseColor(tx.tagColor ?: "#4648d4"))
                        } catch (e: Exception) {
                            StitchPrimary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTagDialog = tx },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                            border = BorderStroke(1.dp, StitchSurfaceContainerLow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(tagColor.copy(alpha = 0.12f))
                                    ) {
                                        Text(
                                            text = tx.tagEmoji ?: if (tx.isTagged) "🏷️" else "❓",
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = 18.sp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.tagName ?: "Untagged",
                                            color = StitchOnSurface,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tx.accountName ?: tx.senderAddress,
                                            color = StitchOnSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = "${if (isDebit) "- " else "+ "}${currencyFormat.format(tx.amount)}",
                                        color = if (isDebit) StitchError else StitchAccentGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = dateFormat.format(Date(tx.timestamp)),
                                        color = StitchOnSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
}
