package com.example.hello_kotlin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.example.hello_kotlin.data.db.entity.TagEntity
import com.example.hello_kotlin.ui.components.ColorPicker
import com.example.hello_kotlin.ui.theme.*
import com.example.hello_kotlin.viewmodel.DashboardViewModel
import com.example.hello_kotlin.viewmodel.TagViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerScreen(
    viewModel: TagViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val tags by viewModel.allTags.collectAsState()
    val transactions by dashboardViewModel.recentTransactions.collectAsState()
    val totalExpenditure by dashboardViewModel.totalExpenditure.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<TagEntity?>(null) }
    var deleteConfirmTag by remember { mutableStateOf<TagEntity?>(null) }

    // Form state
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf("#4648d4") }
    var tagEmoji by remember { mutableStateOf("🏷️") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Calculate dynamic stats
    val activeTagsCount = tags.size
    
    val topCategory = remember(transactions) {
        if (transactions.isEmpty()) "None"
        else {
            transactions.filter { it.isTagged }
                .groupBy { it.tagName }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }?.key ?: "None"
        }
    }

    val usageRate = remember(transactions) {
        if (transactions.isEmpty()) 0
        else {
            val tagged = transactions.count { it.isTagged }
            (tagged * 100) / transactions.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header - Display Tab Name Only
            Text(
                text = "Tags",
                color = StitchPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Auto-Tag Health Card (Moved to the very top)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Auto-Tag Health",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Optimal ($usageRate% Tagged)",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "🟢",
                                    fontSize = 16.sp
                                )
                            }

                            LinearProgressIndicator(
                                progress = { usageRate / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )
                        }
                    }
                }

                // 1. Tag Stats Summary Grid (Usage Rate card removed)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Active Tags
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                            border = BorderStroke(1.dp, StitchSurfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Active Tags",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$activeTagsCount",
                                        color = StitchOnSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(StitchPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🏷️", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Card 2: Top Category
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                            border = BorderStroke(1.dp, StitchSurfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Top Category",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = topCategory,
                                        color = StitchOnSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(StitchSecondaryContainer.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🏆", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Categories & Tags List Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories & Tags",
                            color = StitchOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reorder",
                            color = StitchPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { /* Decorative */ }
                        )
                    }
                }

                if (tags.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
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
                                    Text(text = "🏷️", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No tags created yet",
                                        color = StitchOnSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(tags, key = { index, tag -> "${tag.id}_$index" }) { index, tag ->
                        val parsedColor = remember(tag.colorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(tag.colorHex))
                            } catch (e: Exception) {
                                StitchPrimary
                            }
                        }
                        val count = remember(transactions, tag.name) {
                            transactions.count { it.tagName == tag.name }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                            border = BorderStroke(1.dp, StitchSurfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Emoji box
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(parsedColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = tag.emoji ?: "🏷️", fontSize = 20.sp)
                                    }

                                    Column {
                                        Text(
                                            text = tag.name,
                                            color = StitchOnSurface,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Used in $count transactions",
                                            color = StitchOutline,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Status color dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                    )

                                    // Dropdown options menu
                                    var showMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = StitchOnSurfaceVariant
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                                onClick = {
                                                    showMenu = false
                                                    editingTag = tag
                                                    tagName = tag.name
                                                    tagEmoji = tag.emoji ?: "🏷️"
                                                    tagColor = tag.colorHex
                                                    showBottomSheet = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = StitchError) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StitchError) },
                                                onClick = {
                                                    showMenu = false
                                                    deleteConfirmTag = tag
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                editingTag = null
                tagName = ""
                tagColor = "#4648d4"
                tagEmoji = "🏷️"
                showBottomSheet = true
            },
            containerColor = StitchPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Tag")
        }
    }

    // Add / Edit Tag Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = StitchSurfaceContainerLowest,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (editingTag != null) "Edit Tag" else "Create New Tag",
                    color = StitchOnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Tag Name Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tag Name",
                        color = StitchOutline,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        placeholder = { Text("e.g. Dining, Utilities, Shopping") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StitchPrimary,
                            unfocusedBorderColor = StitchOutlineVariant,
                            cursorColor = StitchPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Emoji Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Choose Icon / Emoji",
                        color = StitchOutline,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val emojiPresets = listOf("🍔", "🏠", "🚗", "🛍️", "🍿", "🏥", "💡", "✈️", "💰", "🎓", "🎁", "🏷️")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emojiPresets.forEach { emoji ->
                            val isSelected = tagEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) StitchPrimary else StitchSurfaceContainerLow)
                                    .clickable { tagEmoji = emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Color Picker
                ColorPicker(
                    selectedColor = tagColor,
                    onColorSelected = { tagColor = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Preview Box
                val previewColor = remember(tagColor) {
                    try {
                        Color(android.graphics.Color.parseColor(tagColor))
                    } catch (e: Exception) {
                        StitchPrimary
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                    border = BorderStroke(1.dp, StitchSurfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(previewColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = tagEmoji, fontSize = 20.sp)
                        }

                        Column {
                            Text(
                                text = tagName.ifBlank { "Preview Tag" },
                                color = StitchOnSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Preview Allocation color",
                                color = StitchOutline,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Save button
                Button(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            if (editingTag != null) {
                                viewModel.updateTag(
                                    editingTag!!.copy(
                                        name = tagName.trim(),
                                        colorHex = tagColor,
                                        emoji = tagEmoji
                                    )
                                )
                            } else {
                                viewModel.createTag(
                                    name = tagName.trim(),
                                    colorHex = tagColor,
                                    emoji = tagEmoji
                                )
                            }
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = tagName.isNotBlank()
                ) {
                    Text(
                        text = if (editingTag != null) "Save Tag" else "Create Tag",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Confirmation dialog
    deleteConfirmTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleteConfirmTag = null },
            title = {
                Text(
                    text = "Delete '${tag.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface
                )
            },
            text = {
                Text(
                    text = "Transactions tagged with '${tag.name}' will become untagged.",
                    color = StitchOnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTag(tag.id)
                    deleteConfirmTag = null
                }) {
                    Text(
                        text = "DELETE",
                        color = StitchError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTag = null }) {
                    Text(
                        text = "CANCEL",
                        color = StitchOutline,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = StitchSurfaceContainerLowest
        )
    }
}
