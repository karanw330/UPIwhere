package com.example.hello_kotlin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.ui.theme.*
import com.example.hello_kotlin.viewmodel.AutoTagViewModel
import com.example.hello_kotlin.viewmodel.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTagRulesScreen(
    autoTagViewModel: AutoTagViewModel,
    tagViewModel: TagViewModel
) {
    val rules by autoTagViewModel.rules.collectAsState()
    val allTags by tagViewModel.allTags.collectAsState()

    var showSettingsBottomSheet by remember { mutableStateOf(false) }

    /* Disabled SMS settings states & triggers (Commented Out)
    val senderPattern by autoTagViewModel.senderPattern.collectAsState()
    val excludePattern by autoTagViewModel.excludePattern.collectAsState()
    val debitKeywords by autoTagViewModel.debitKeywords.collectAsState()
    val creditKeywords by autoTagViewModel.creditKeywords.collectAsState()
    val promoKeywords by autoTagViewModel.promoKeywords.collectAsState()
    val ignoreStrings by autoTagViewModel.ignoreStrings.collectAsState()
    val isImporting by autoTagViewModel.isImporting.collectAsState()
    val importProgress by autoTagViewModel.importProgress.collectAsState()

    var editSenderPattern by remember { mutableStateOf("") }
    var editExcludePattern by remember { mutableStateOf("") }
    var editDebitKeywords by remember { mutableStateOf("") }
    var editCreditKeywords by remember { mutableStateOf("") }
    var editPromoKeywords by remember { mutableStateOf("") }
    var editIgnoreStrings by remember { mutableStateOf("") }

    var testSender by remember { mutableStateOf("SBIUPI") }
    var testMessage by remember { mutableStateOf("") }

    LaunchedEffect(showSettingsBottomSheet) {
        if (showSettingsBottomSheet) {
            editSenderPattern = senderPattern
            editExcludePattern = excludePattern
            editDebitKeywords = debitKeywords
            editCreditKeywords = creditKeywords
            editPromoKeywords = promoKeywords
            editIgnoreStrings = ignoreStrings
        }
    }
    */

    // Rule Form state
    var ruleKeyword by remember { mutableStateOf("") }
    var selectedTagId by remember { mutableStateOf<Long?>(null) }

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
                text = "Rules",
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
                // 1. Feature Info Card (Smart Auto-Tagging Header Banner)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Smart Auto-Tagging",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Organize your finances automatically. Create rules that map specific keywords in your transaction descriptions to custom tags and spending categories.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Button(
                                onClick = { showSettingsBottomSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchSurfaceContainerLowest,
                                    contentColor = StitchPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Import & Ignore Settings",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Create New Rule Form Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
                        border = BorderStroke(1.dp, StitchSurfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Create New Rule",
                                color = StitchOnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Keyword input field
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Keyword or Sender Name",
                                    color = StitchOutline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                OutlinedTextField(
                                    value = ruleKeyword,
                                    onValueChange = { ruleKeyword = it },
                                    placeholder = { Text("e.g. Netflix, Zomato, Rent") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = StitchOutline
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StitchPrimary,
                                        unfocusedBorderColor = StitchOutlineVariant,
                                        cursorColor = StitchPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Dynamic checkable category grid
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Map to Category",
                                    color = StitchOutline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                if (allTags.isEmpty()) {
                                    Text(
                                        text = "No tags available. Create a tag first under the Tags tab.",
                                        color = StitchError,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    val chunkedTags = allTags.chunked(2)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        chunkedTags.forEach { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                row.forEach { tag ->
                                                    val isSelected = selectedTagId == tag.id
                                                    val tagColor = try {
                                                        Color(android.graphics.Color.parseColor(tag.colorHex))
                                                    } catch (e: Exception) {
                                                        StitchPrimary
                                                    }
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedTagId = tag.id },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSelected) StitchPrimary.copy(alpha = 0.08f) else StitchSurfaceContainerLow
                                                        ),
                                                        border = BorderStroke(
                                                            width = 1.dp,
                                                            color = if (isSelected) StitchPrimary else Color.Transparent
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(tagColor)
                                                            )
                                                            Text(
                                                                text = tag.name,
                                                                color = if (isSelected) StitchPrimary else StitchOnSurface,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                if (row.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Add Automation Rule button
                            Button(
                                onClick = {
                                    if (ruleKeyword.isNotBlank() && selectedTagId != null) {
                                        autoTagViewModel.addRule(ruleKeyword, selectedTagId!!)
                                        ruleKeyword = ""
                                        selectedTagId = null
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
                                enabled = ruleKeyword.isNotBlank() && selectedTagId != null
                            ) {
                                Text(
                                    text = "Add Automation Rule",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Active Rules List
                item {
                    Text(
                        text = "Active Rules",
                        color = StitchOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (rules.isEmpty()) {
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
                                    Text(text = "🤖", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No active auto-tag rules",
                                        color = StitchOnSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(rules, key = { index, rule -> "${rule.id}_$index" }) { index, rule ->
                        val tagColor = remember(rule.tagColor) {
                            try {
                                Color(android.graphics.Color.parseColor(rule.tagColor))
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
                                    // Rule icon box
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(StitchPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "⚙️", fontSize = 20.sp)
                                    }

                                    Column {
                                        Text(
                                            text = "\"${rule.accountKeyword}\"",
                                            color = StitchOnSurface,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(tagColor)
                                            )
                                            Text(
                                                text = rule.tagName,
                                                color = StitchOnSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "•",
                                                color = StitchOutlineVariant,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Auto-tags",
                                                color = StitchOnSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { autoTagViewModel.deleteRule(rule.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete rule",
                                        tint = StitchError
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    if (showSettingsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsBottomSheet = false },
            containerColor = StitchSurfaceContainerLowest,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                com.example.hello_kotlin.ui.components.PassbookSettingsContent(
                    autoTagViewModel = autoTagViewModel,
                    onImportComplete = { showSettingsBottomSheet = false }
                )
            }
        }
    }
}
