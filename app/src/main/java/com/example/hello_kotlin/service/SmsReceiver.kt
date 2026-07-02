package com.example.hello_kotlin.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.repository.ExpenseRepository
import com.example.hello_kotlin.notification.NotificationHelper
import com.example.hello_kotlin.service.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that listens for incoming SMS messages.
 * When a financial transaction is detected, it saves it to the database
 * and fires an interactive notification for tag selection.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Combine multi-part SMS into a single body
        val senderAddress = messages[0].displayOriginatingAddress ?: "Unknown"
        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        if (fullBody.isBlank()) return

        // Parse the SMS
        val parsed = SmsParser.parse(fullBody, senderAddress, timestamp, context) ?: return

        // Only process debits for expense tracking notifications
        // Credits are still stored but don't trigger notifications
        val db = AppDatabase.getInstance(context)
        val repository = ExpenseRepository(
            db.transactionDao(),
            db.tagDao(),
            db.autoTagRuleDao()
        )

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactionId = repository.insertParsedTransaction(parsed)

                if (parsed.type == TransactionType.DEBIT) {
                    // Check if it was auto-tagged
                    val matchingTag = parsed.accountName?.let {
                        db.autoTagRuleDao().findMatchingTag(it)
                    } ?: db.autoTagRuleDao().findMatchingTagFromMessage(parsed.rawMessage)

                    if (matchingTag != null) {
                        // Auto-tagged: show a brief confirmation notification
                        NotificationHelper.showAutoTaggedNotification(
                            context = context,
                            transactionId = transactionId,
                            amount = parsed.amount,
                            tagName = matchingTag.name,
                            tagEmoji = matchingTag.emoji,
                            accountName = parsed.accountName
                        )
                    } else {
                        // Not auto-tagged: show interactive tag-selection notification
                        val topTags = repository.getMostUsedTags(3)
                        NotificationHelper.showTagSelectionNotification(
                            context = context,
                            transactionId = transactionId,
                            amount = parsed.amount,
                            accountName = parsed.accountName,
                            senderAddress = senderAddress,
                            topTags = topTags
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
