package com.example.hello_kotlin.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello_kotlin.service.PassbookParser
import com.example.hello_kotlin.service.model.ParsedTransaction
import com.example.hello_kotlin.ui.theme.*
import com.example.hello_kotlin.viewmodel.AutoTagViewModel
import com.example.hello_kotlin.viewmodel.PreviewTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassbookSettingsContent(
    autoTagViewModel: AutoTagViewModel,
    onImportComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel States
    val latestTs by autoTagViewModel.latestTransactionTimestamp.collectAsState()
    val reminderEnabled by autoTagViewModel.passbookReminderEnabled.collectAsState()
    val reminderDay by autoTagViewModel.passbookReminderDay.collectAsState()
    val reminderHour by autoTagViewModel.passbookReminderHour.collectAsState()
    val reminderMinute by autoTagViewModel.passbookReminderMinute.collectAsState()
    val ignoreStrings by autoTagViewModel.ignoreStrings.collectAsState()

    // Local Parsing UI States
    var isParsing by remember { mutableStateOf(false) }
    var parsedPreviewList by remember { mutableStateOf<List<PreviewTransaction>>(emptyList()) }
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var lastSelectedFileName by remember { mutableStateOf("") }
    
    // Reminder config states
    var editReminderEnabled by remember(reminderEnabled) { mutableStateOf(reminderEnabled) }
    var editReminderDay by remember(reminderDay) { mutableStateOf(reminderDay) }
    var editReminderHour by remember(reminderHour) { mutableStateOf(reminderHour) }
    var editReminderMinute by remember(reminderMinute) { mutableStateOf(reminderMinute) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.US) }
    
    val latestDateStr = remember(latestTs) {
        if (latestTs != null) {
            val date = Instant.ofEpochMilli(latestTs!!).atZone(ZoneId.systemDefault()).toLocalDate()
            date.format(formatter)
        } else {
            "No transactions in app"
        }
    }

    // Gap warning calculation
    val gapInfo = remember(latestTs, parsedPreviewList) {
        if (latestTs != null && parsedPreviewList.isNotEmpty()) {
            val latestDbLocalDate = Instant.ofEpochMilli(latestTs!!).atZone(ZoneId.systemDefault()).toLocalDate()
            // Find the oldest transaction in statement. Google Pay statements are latest-to-oldest, so oldest is last in the list.
            val oldestStatementTs = parsedPreviewList.lastOrNull()?.transaction?.timestamp
            if (oldestStatementTs != null) {
                val oldestStatementLocalDate = Instant.ofEpochMilli(oldestStatementTs).atZone(ZoneId.systemDefault()).toLocalDate()
                if (oldestStatementLocalDate.isAfter(latestDbLocalDate)) {
                    val gapDays = java.time.temporal.ChronoUnit.DAYS.between(latestDbLocalDate, oldestStatementLocalDate)
                    if (gapDays > 3) { // Ignore small weekend gaps
                        Triple(true, gapDays, oldestStatementLocalDate)
                    } else {
                        Triple(false, 0L, oldestStatementLocalDate)
                    }
                } else {
                    Triple(false, 0L, oldestStatementLocalDate)
                }
            } else {
                Triple(false, 0L, null)
            }
        } else {
            Triple(false, 0L, null)
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isParsing = true
            coroutineScope.launch(Dispatchers.IO) {
                // Get filename
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val name = cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1) it.getString(nameIdx) else null
                    } else null
                } ?: "statement.pdf"
                
                withContext(Dispatchers.Main) {
                    lastSelectedFileName = name
                }

                // Parse PDF
                val parsed = PassbookParser.parsePdf(context, uri)
                val preview = autoTagViewModel.previewPassbookTransactions(parsed)
                
                withContext(Dispatchers.Main) {
                    parsedPreviewList = preview
                    // Pre-select non-duplicate items
                    selectedIndices = preview.mapIndexedNotNull { idx, item ->
                        if (!item.isDuplicate) idx else null
                    }.toSet()
                    isParsing = false
                    
                    if (parsed.isEmpty()) {
                        Toast.makeText(context, "No transactions found in this statement.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section Title: Passbook Statement PDF Import
        Text(
            text = "Google Pay PDF Statement Import",
            color = StitchOnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Bento card for current state & upload trigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLow),
            border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Current Database Info",
                        color = StitchOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last recorded transaction date:",
                        color = StitchOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = latestDateStr,
                        color = StitchPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f))
                
                Button(
                    onClick = { filePickerLauncher.launch("application/pdf") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isParsing
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isParsing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(text = "Extracting statement...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (lastSelectedFileName.isEmpty()) "Select Google Pay PDF" else "Change Statement PDF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (lastSelectedFileName.isNotEmpty()) {
                    Text(
                        text = "Selected file: $lastSelectedFileName",
                        color = StitchOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Preview Section
        if (parsedPreviewList.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Statement Preview (${parsedPreviewList.size} found)",
                    color = StitchOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Gap Warning Banner
                if (gapInfo.first) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, AccentOrange)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "⚠️", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = "Data Gap Detected (${gapInfo.second} Days)",
                                    color = StitchOnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your last transaction was on $latestDateStr, but the oldest statement entry starts on ${gapInfo.third?.format(formatter)}. You are missing transactions in between.",
                                    color = StitchOnSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Checkbox controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedIndices = parsedPreviewList.indices.toSet()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchPrimary)
                    }
                    TextButton(
                        onClick = {
                            selectedIndices = emptySet()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Deselect All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchOutline)
                    }
                }

                // Checklist Box (Neo-brutalist solid height scroll container)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, StitchOutlineVariant)
                        .background(StitchSurfaceContainerLowest)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        parsedPreviewList.forEachIndexed { index, preview ->
                            val parsed = preview.transaction
                            val isChecked = selectedIndices.contains(index)
                            val localDate = Instant.ofEpochMilli(parsed.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIndices = if (isChecked) {
                                            selectedIndices - index
                                        } else {
                                            selectedIndices + index
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedIndices = if (checked == true) {
                                            selectedIndices + index
                                        } else {
                                            selectedIndices - index
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = StitchPrimary,
                                        uncheckedColor = StitchOutline
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = parsed.accountName ?: "Unknown Recipient",
                                            color = if (preview.isDuplicate) StitchOutline else StitchOnSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = currencyFormat.format(parsed.amount),
                                            color = if (parsed.type == com.example.hello_kotlin.service.model.TransactionType.CREDIT) StitchAccentGreen else StitchPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = localDate.format(formatter),
                                            color = StitchOnSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                        if (preview.isDuplicate) {
                                            Text(
                                                text = "Already Imported",
                                                color = AccentOrange,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(AccentOrange.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (index < parsedPreviewList.size - 1) {
                                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                // Import Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val selectedItems = parsedPreviewList.filterIndexed { idx, _ -> selectedIndices.contains(idx) }
                                .map { it.transaction }
                            
                            val count = autoTagViewModel.importPassbookTransactions(selectedItems)
                            Toast.makeText(context, "Imported $count transactions successfully!", Toast.LENGTH_LONG).show()
                            parsedPreviewList = emptyList()
                            lastSelectedFileName = ""
                            selectedIndices = emptySet()
                            onImportComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = selectedIndices.isNotEmpty()
                ) {
                    Text(
                        text = "Import Selected (${selectedIndices.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.5f))

        // Section Title: Passbook Monthly Upload Alerts
        Text(
            text = "Monthly Reminder Settings",
            color = StitchOnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
            border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Monthly Reminder Alerts",
                            color = StitchOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sends a notification to upload your statement every month.",
                            color = StitchOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = editReminderEnabled,
                        onCheckedChange = { editReminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = StitchPrimary,
                            uncheckedThumbColor = StitchOutline,
                            uncheckedTrackColor = StitchSurfaceContainerLow
                        )
                    )
                }

                if (editReminderEnabled) {
                    HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f))
                    
                    // Day Selector Card (Neo-brutalist buttons)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reminder Day of Month",
                            color = StitchOutline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { if (editReminderDay > 1) editReminderDay-- },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StitchSurfaceContainerLow)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = StitchOnSurface, modifier = Modifier.size(16.dp))
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, StitchOutlineVariant, RoundedCornerShape(8.dp))
                                    .background(StitchSurfaceContainerLowest)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Day $editReminderDay",
                                    color = StitchOnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { if (editReminderDay < 31) editReminderDay++ },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StitchSurfaceContainerLow)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = StitchOnSurface, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Time Selector Card (Neo-brutalist Hour & Minute increment/decrements)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reminder Time of Day",
                            color = StitchOutline,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour selector
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { editReminderHour = (editReminderHour + 23) % 24 },
                                    modifier = Modifier.size(28.dp).background(StitchSurfaceContainerLow, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease hour", modifier = Modifier.size(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, StitchOutlineVariant, RoundedCornerShape(6.dp))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d:00", editReminderHour),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchOnSurface
                                    )
                                }
                                IconButton(
                                    onClick = { editReminderHour = (editReminderHour + 1) % 24 },
                                    modifier = Modifier.size(28.dp).background(StitchSurfaceContainerLow, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase hour", modifier = Modifier.size(12.dp))
                                }
                            }

                            Text(":", color = StitchOutline, fontWeight = FontWeight.Bold)

                            // Minute selector
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { editReminderMinute = (editReminderMinute + 55) % 60 },
                                    modifier = Modifier.size(28.dp).background(StitchSurfaceContainerLow, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease minute", modifier = Modifier.size(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, StitchOutlineVariant, RoundedCornerShape(6.dp))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d min", editReminderMinute),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchOnSurface
                                    )
                                }
                                IconButton(
                                    onClick = { editReminderMinute = (editReminderMinute + 5) % 60 },
                                    modifier = Modifier.size(28.dp).background(StitchSurfaceContainerLow, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase minute", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Save configuration button
                val settingsChanged = editReminderEnabled != reminderEnabled ||
                        editReminderDay != reminderDay ||
                        editReminderHour != reminderHour ||
                        editReminderMinute != reminderMinute

                Button(
                    onClick = {
                        autoTagViewModel.updatePassbookReminderSettings(
                            context = context,
                            enabled = editReminderEnabled,
                            day = editReminderDay,
                            hour = editReminderHour,
                            minute = editReminderMinute
                        )
                        Toast.makeText(context, "Reminder settings updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settingsChanged
                ) {
                    Text(
                        text = "Save Reminder Schedule",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.5f))

        // Section Title: Ignored Vendors Settings
        Text(
            text = "Ignored Vendors Settings",
            color = StitchOnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerLowest),
            border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ignore Payments to Vendors",
                    color = StitchOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transactions to these vendors will be ignored and not imported. Enter vendor names separated by commas.",
                    color = StitchOnSurfaceVariant,
                    fontSize = 11.sp
                )
                
                var newIgnoreVendor by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newIgnoreVendor,
                        onValueChange = { newIgnoreVendor = it },
                        placeholder = { Text("Enter vendor name...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StitchPrimary,
                            unfocusedBorderColor = StitchOutlineVariant,
                            cursorColor = StitchPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newIgnoreVendor.isNotBlank()) {
                                    val vendorToAdd = newIgnoreVendor.trim()
                                    val currentList = ignoreStrings.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    if (!currentList.any { it.equals(vendorToAdd, ignoreCase = true) }) {
                                        val updatedList = (currentList + vendorToAdd).joinToString(", ")
                                        autoTagViewModel.updateIgnoreStrings(updatedList)
                                    }
                                    newIgnoreVendor = ""
                                }
                            }
                        )
                    )

                    Button(
                        onClick = {
                            if (newIgnoreVendor.isNotBlank()) {
                                val vendorToAdd = newIgnoreVendor.trim()
                                val currentList = ignoreStrings.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                if (!currentList.any { it.equals(vendorToAdd, ignoreCase = true) }) {
                                    val updatedList = (currentList + vendorToAdd).joinToString(", ")
                                    autoTagViewModel.updateIgnoreStrings(updatedList)
                                }
                                newIgnoreVendor = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Add", fontWeight = FontWeight.Bold)
                    }
                }

                // List of ignored vendors
                val currentIgnores = remember(ignoreStrings) {
                    ignoreStrings.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }

                if (currentIgnores.isNotEmpty()) {
                    Text(
                        text = "Currently Ignored Vendors:",
                        color = StitchOnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chunkedIgnores = remember(currentIgnores) {
                            currentIgnores.chunked(3)
                        }
                        chunkedIgnores.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { vendor ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StitchSurfaceContainerLow)
                                            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = vendor,
                                            color = StitchOnSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $vendor",
                                            tint = StitchOutline,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    val newList = currentIgnores.filter { it != vendor }.joinToString(", ")
                                                    autoTagViewModel.updateIgnoreStrings(newList)
                                                }
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
