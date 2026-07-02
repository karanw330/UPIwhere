package com.example.hello_kotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.repository.ExpenseRepository
import com.example.hello_kotlin.navigation.AppNavigation
import com.example.hello_kotlin.notification.NotificationHelper
import com.example.hello_kotlin.ui.theme.AccentPrimary
import com.example.hello_kotlin.ui.theme.DarkBackground
import com.example.hello_kotlin.ui.theme.Hello_kotlinTheme
import com.example.hello_kotlin.ui.theme.TextPrimary
import com.example.hello_kotlin.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    // Disabled SMS status tracking (Commented Out)
    // private var isImporting by mutableStateOf(false)
    // private var importProgress by mutableStateOf("")
    private var permissionsGranted by mutableStateOf(false)

    private val requiredPermissions = buildList {
        // Disabled SMS permissions (Commented Out)
        // add(Manifest.permission.RECEIVE_SMS)
        // add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Disabled SMS verification callback (Commented Out)
        // val smsGranted = permissions[Manifest.permission.READ_SMS] == true
        //         && permissions[Manifest.permission.RECEIVE_SMS] == true
        // if (smsGranted) {
        //     permissionsGranted = true
        //     checkAndImportHistory()
        // } else {
        //     permissionsGranted = true
        //     Toast.makeText(this, "SMS permissions denied. Auto-tracking won't work.", Toast.LENGTH_LONG).show()
        // }
        permissionsGranted = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("expense_tracker", MODE_PRIVATE)
        com.example.hello_kotlin.ui.theme.AppThemeState.isDark = prefs.getBoolean("dark_mode", false)
        val lastCrash = prefs.getString("last_crash", null)

        // Handle passbook reminder notification click
        if (intent?.getBooleanExtra(com.example.hello_kotlin.notification.PassbookReminderReceiver.EXTRA_OPEN_PASSBOOK, false) == true) {
            prefs.edit().putBoolean("trigger_passbook_sheet", true).commit()
        }

        // Migrate default sender pattern to support major banks
        val savedSender = prefs.getString("sender_pattern", "SBIUPI")
        if (savedSender == "SBIUPI") {
            prefs.edit().putString("sender_pattern", "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK").apply()
        }

        // Set uncaught exception handler to log crash details
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            prefs.edit().putString("last_crash", throwable.stackTraceToString()).commit()
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Initialize PDFBox
        try {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize database
        val db = AppDatabase.getInstance(this)

        // Clear existing SMS transactions
        CoroutineScope(Dispatchers.IO).launch {
            db.transactionDao().deleteSmsTransactions()
        }

        // Reschedule/Validate Passbook upload alarm
        com.example.hello_kotlin.notification.PassbookReminderReceiver.scheduleNextReminder(this)

        // Check permissions
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            permissionsGranted = true
        } else {
            permissionLauncher.launch(requiredPermissions)
        }

        setContent {
            Hello_kotlinTheme(darkTheme = com.example.hello_kotlin.ui.theme.AppThemeState.isDark) {
                var currentCrashLog by remember { mutableStateOf(lastCrash) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentCrashLog != null) {
                        CrashReportScreen(
                            stackTrace = currentCrashLog!!,
                            onDismiss = {
                                prefs.edit().remove("last_crash").apply()
                                currentCrashLog = null
                            }
                        )
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(com.example.hello_kotlin.notification.PassbookReminderReceiver.EXTRA_OPEN_PASSBOOK, false)) {
            getSharedPreferences("expense_tracker", MODE_PRIVATE).edit().putBoolean("trigger_passbook_sheet", true).commit()
        }
    }

    /* Disabled checkAndImportHistory (Commented Out)
    private fun checkAndImportHistory() {
        val prefs = getSharedPreferences("expense_tracker", MODE_PRIVATE)
        val hasImported = prefs.getBoolean("history_imported", false)

        if (!hasImported && ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            isImporting = true

            val db = AppDatabase.getInstance(this)
            val repository = ExpenseRepository(
                db.transactionDao(),
                db.tagDao(),
                db.autoTagRuleDao()
            )

            CoroutineScope(Dispatchers.Main).launch {
                val count = SmsHistoryImporter.importHistoricalMessages(
                    context = this@MainActivity,
                    repository = repository,
                    onProgress = { imported, total ->
                        CoroutineScope(Dispatchers.Main).launch {
                            importProgress = "Found $imported transactions (scanned ${total} messages)"
                        }
                    }
                )

                prefs.edit().putBoolean("history_imported", true).apply()
                isImporting = false

                if (count > 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "Imported $count transactions from SMS history!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    */
}

@Composable
fun CrashReportScreen(
    stackTrace: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "💥 APP CRASH DETECTED",
            color = Color(0xFFFF6B6B),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "The app crashed during the last session. Below is the error report. Please copy and share it to help us fix the issue:",
            color = Color(0xFFE2E8F0),
            fontSize = 14.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val scrollState = rememberScrollState()
            SelectionContainer {
                Text(
                    text = stackTrace,
                    color = Color(0xFFFFB86C),
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Crash Log", stackTrace)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("COPY ERROR", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("CLEAR & CONTINUE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}