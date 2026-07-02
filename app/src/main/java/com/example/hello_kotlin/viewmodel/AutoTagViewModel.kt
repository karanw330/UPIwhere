package com.example.hello_kotlin.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.db.dao.AutoTagRuleWithTag
import com.example.hello_kotlin.data.repository.ExpenseRepository
import com.example.hello_kotlin.data.db.entity.TransactionEntity
import com.example.hello_kotlin.service.model.ParsedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AutoTagViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        db.transactionDao(),
        db.tagDao(),
        db.autoTagRuleDao()
    )

    private val prefs = application.getSharedPreferences("expense_tracker", android.content.Context.MODE_PRIVATE)

    // Passbook reminder preferences
    private val _passbookReminderEnabled = MutableStateFlow(prefs.getBoolean("passbook_reminder_enabled", false))
    val passbookReminderEnabled: StateFlow<Boolean> = _passbookReminderEnabled.asStateFlow()

    private val _passbookReminderDay = MutableStateFlow(prefs.getInt("passbook_reminder_day", 1))
    val passbookReminderDay: StateFlow<Int> = _passbookReminderDay.asStateFlow()

    private val _passbookReminderHour = MutableStateFlow(prefs.getInt("passbook_reminder_hour", 9))
    val passbookReminderHour: StateFlow<Int> = _passbookReminderHour.asStateFlow()

    private val _passbookReminderMinute = MutableStateFlow(prefs.getInt("passbook_reminder_minute", 0))
    val passbookReminderMinute: StateFlow<Int> = _passbookReminderMinute.asStateFlow()

    // Last recorded transaction details
    private val _latestTransactionTimestamp = MutableStateFlow<Long?>(null)
    val latestTransactionTimestamp: StateFlow<Long?> = _latestTransactionTimestamp.asStateFlow()

    private val _ignoreStrings = MutableStateFlow(prefs.getString("ignore_strings", "") ?: "")
    val ignoreStrings: StateFlow<String> = _ignoreStrings.asStateFlow()

    fun updateIgnoreStrings(value: String) {
        val cleanValue = value.split(Regex("[,;\\n\\r]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        prefs.edit().putString("ignore_strings", cleanValue).apply()
        _ignoreStrings.value = cleanValue
        viewModelScope.launch {
            // Reset ignore flags for all transactions
            repository.resetAllIgnoredTransactions()

            // Mark transactions as ignored for actual ignored vendors
            val ignores = cleanValue.split(", ")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (vendor in ignores) {
                repository.markTransactionsAsIgnored(vendor)
            }
        }
    }

    private fun isIgnored(accountName: String?, ignoreList: String): Boolean {
        if (accountName == null) return false
        val ignores = ignoreList.split(Regex("[,;\\n\\r]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val nameLower = accountName.lowercase()
        return ignores.any { nameLower.contains(it) }
    }

    init {
        refreshLatestTransactionTimestamp()
        viewModelScope.launch {
            cleanExistingRawMessages()
        }
    }

    private suspend fun cleanExistingRawMessages() {
        try {
            val transactions = repository.getAllTransactionsList()
            val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            for (tx in transactions) {
                val msg = tx.rawMessage
                val matcher = emailRegex.toPattern().matcher(msg)
                if (matcher.find()) {
                    val emailEndIdx = matcher.end()
                    val cleaned = msg.substring(0, emailEndIdx).trim().removeSuffix(",").trim()
                    if (cleaned != msg) {
                        repository.updateTransactionRawMessage(tx.id, cleaned)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AutoTagViewModel", "Error cleaning existing raw messages", e)
        }
    }

    fun refreshLatestTransactionTimestamp() {
        viewModelScope.launch {
            _latestTransactionTimestamp.value = repository.getLatestTransactionTimestamp()
        }
    }

    fun updatePassbookReminderSettings(context: android.content.Context, enabled: Boolean, day: Int, hour: Int, minute: Int) {
        prefs.edit().apply {
            putBoolean("passbook_reminder_enabled", enabled)
            putInt("passbook_reminder_day", day)
            putInt("passbook_reminder_hour", hour)
            putInt("passbook_reminder_minute", minute)
            apply()
        }
        _passbookReminderEnabled.value = enabled
        _passbookReminderDay.value = day
        _passbookReminderHour.value = hour
        _passbookReminderMinute.value = minute
        
        com.example.hello_kotlin.notification.PassbookReminderReceiver.scheduleNextReminder(context)
    }

    suspend fun importPassbookTransactions(
        parsedTransactions: List<ParsedTransaction>
    ): Int {
        val ignoreList = ignoreStrings.value
        val filteredTransactions = parsedTransactions.filter { !isIgnored(it.accountName, ignoreList) }
        if (filteredTransactions.isEmpty()) return 0

        val latestTs = repository.getLatestTransactionTimestamp()
        var importedCount = 0

        if (latestTs != null) {
            // Find the oldest transaction timestamp in the parsed statement (minimum)
            val oldestStatementTs = filteredTransactions.minOf { it.timestamp }
            val oldestLocalDate = Instant.ofEpochMilli(oldestStatementTs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val startOfOldestDay = oldestLocalDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // Fetch all database transactions on or after the oldest day to check duplicates
            val existingTransactions = repository.getTransactionsOnOrAfter(startOfOldestDay)

            for (parsed in filteredTransactions) {
                val isDuplicate = existingTransactions.any { existing ->
                    existing.amount == parsed.amount &&
                    existing.type == parsed.type.name &&
                    existing.accountName?.uppercase() == parsed.accountName?.uppercase() &&
                    isSameDay(existing.timestamp, parsed.timestamp)
                }

                if (!isDuplicate) {
                    repository.insertParsedTransaction(parsed)
                    importedCount++
                } else {
                    Log.d("AutoTagViewModel", "Skipped duplicate transaction: ${parsed.accountName} - ${parsed.amount}")
                }
            }
        } else {
            // Empty database: import all
            for (parsed in filteredTransactions) {
                repository.insertParsedTransaction(parsed)
                importedCount++
            }
        }

        refreshLatestTransactionTimestamp()
        return importedCount
    }

    suspend fun previewPassbookTransactions(parsed: List<ParsedTransaction>): List<PreviewTransaction> {
        if (parsed.isEmpty()) return emptyList()
        val ignoreList = ignoreStrings.value
        val filteredParsed = parsed.filter { !isIgnored(it.accountName, ignoreList) }
        
        if (filteredParsed.isEmpty()) return emptyList()
        val latestTs = repository.getLatestTransactionTimestamp() ?: return filteredParsed.map { PreviewTransaction(it, false) }
        
        val oldestStatementTs = filteredParsed.minOf { it.timestamp }
        val oldestLocalDate = Instant.ofEpochMilli(oldestStatementTs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val startOfOldestDay = oldestLocalDate.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val existingTransactions = repository.getTransactionsOnOrAfter(startOfOldestDay)
        
        return filteredParsed.map { item ->
            val isDuplicate = existingTransactions.any { existing ->
                existing.amount == item.amount &&
                existing.type == item.type.name &&
                existing.accountName?.uppercase() == item.accountName?.uppercase() &&
                isSameDay(existing.timestamp, item.timestamp)
            }
            PreviewTransaction(item, isDuplicate)
        }
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val d1 = Instant.ofEpochMilli(ts1).atZone(ZoneId.systemDefault()).toLocalDate()
        val d2 = Instant.ofEpochMilli(ts2).atZone(ZoneId.systemDefault()).toLocalDate()
        return d1 == d2
    }

    /* Disabled SMS states, settings updates, and history refreshers (Commented Out / Greyed Out)
    private val _senderPattern = MutableStateFlow(
        run {
            val current = prefs.getString("sender_pattern", "SBIUPI")
            if (current == "SBIUPI") {
                prefs.edit().putString("sender_pattern", "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK").apply()
                "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK"
            } else {
                current ?: "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK"
            }
        }
    )
    val senderPattern: StateFlow<String> = _senderPattern.asStateFlow()

    private val _excludePattern = MutableStateFlow(prefs.getString("exclude_pattern", "CBSSBI") ?: "CBSSBI")
    val excludePattern: StateFlow<String> = _excludePattern.asStateFlow()

    private val _debitKeywords = MutableStateFlow(
        prefs.getString("debit_keywords", "debited,deducted,spent,paid,withdrawn,purchase,sent,transferred,debit,payment,dr") ?: "debited,deducted,spent,paid,withdrawn,purchase,sent,transferred,debit,payment,dr"
    )
    val debitKeywords: StateFlow<String> = _debitKeywords.asStateFlow()

    private val _creditKeywords = MutableStateFlow(
        prefs.getString("credit_keywords", "credited,received,deposited,refund,cashback,credit,cr") ?: "credited,received,deposited,refund,cashback,credit,cr"
    )
    val creditKeywords: StateFlow<String> = _creditKeywords.asStateFlow()

    private val _promoKeywords = MutableStateFlow(
        prefs.getString("promo_keywords", "congratulations,congrats,lucky,winner,pre-approved,pre approved,pre-qualified,pre qualified,eligible,apply now,apply for,avail,instant loan,pay later,paylater,limit increased,increase limit,credit limit,enhancement,voucher,coupon,promocode,promo code,discount code,cashback offer,added to wallet,credited to wallet,wallet balance,paytm wallet,amazon pay wallet,wallet loaded,add money,limited period,deals of the day,flat off,special offer") ?: "congratulations,congrats,lucky,winner,pre-approved,pre approved,pre-qualified,pre qualified,eligible,apply now,apply for,avail,instant loan,pay later,paylater,limit increased,increase limit,credit limit,enhancement,voucher,coupon,promocode,promo code,discount code,cashback offer,added to wallet,credited to wallet,wallet balance,paytm wallet,amazon pay wallet,wallet loaded,add money,limited period,deals of the day,flat off,special offer"
    )
    val promoKeywords: StateFlow<String> = _promoKeywords.asStateFlow()

    private val _ignoreStrings = MutableStateFlow(prefs.getString("ignore_strings", "") ?: "")
    val ignoreStrings: StateFlow<String> = _ignoreStrings.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow("")
    val importProgress: StateFlow<String> = _importProgress.asStateFlow()

    fun updateSettings(sender: String, exclude: String, debit: String, credit: String, promo: String, ignore: String) {
        prefs.edit().apply {
            putString("sender_pattern", sender)
            putString("exclude_pattern", exclude)
            putString("debit_keywords", debit)
            putString("credit_keywords", credit)
            putString("promo_keywords", promo)
            putString("ignore_strings", ignore)
            apply()
        }
        _senderPattern.value = sender
        _excludePattern.value = exclude
        _debitKeywords.value = debit
        _creditKeywords.value = credit
        _promoKeywords.value = promo
        _ignoreStrings.value = ignore
    }

    fun refreshSmsHistory(context: android.content.Context) {
        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = "Clearing transactions..."
            repository.deleteAllTransactions()

            _importProgress.value = "Scanning SMS inbox..."
            val importedCount = com.example.hello_kotlin.service.SmsHistoryImporter.importHistoricalMessages(
                context = context,
                repository = repository,
                onProgress = { imported, total ->
                    _importProgress.value = "Found $imported transactions (scanned $total messages)"
                }
            )

            _isImporting.value = false
            _importProgress.value = ""

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Imported $importedCount transactions successfully!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    */

    val rules: StateFlow<List<AutoTagRuleWithTag>> = repository.getAllAutoTagRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(keyword: String, tagId: Long) {
        viewModelScope.launch {
            val ruleKeyword = keyword.uppercase().trim()
            repository.addAutoTagRule(ruleKeyword, tagId)
            repository.applyRuleToExistingTransactions(ruleKeyword, tagId)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            repository.deleteAutoTagRule(id)
        }
    }
}

data class PreviewTransaction(
    val transaction: ParsedTransaction,
    val isDuplicate: Boolean
)
