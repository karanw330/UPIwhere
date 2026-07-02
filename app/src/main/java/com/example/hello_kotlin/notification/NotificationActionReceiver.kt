package com.example.hello_kotlin.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.hello_kotlin.data.db.AppDatabase
import com.example.hello_kotlin.data.db.entity.TagEntity
import com.example.hello_kotlin.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles notification action button clicks (tag selection and new tag creation).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getLongExtra(NotificationHelper.EXTRA_TRANSACTION_ID, -1)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        if (transactionId == -1L) return

        val db = AppDatabase.getInstance(context)
        val repository = ExpenseRepository(
            db.transactionDao(),
            db.tagDao(),
            db.autoTagRuleDao()
        )

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_TAG_SELECTED -> {
                        val tagId = intent.getLongExtra(NotificationHelper.EXTRA_TAG_ID, -1)
                        if (tagId != -1L) {
                            repository.tagTransaction(transactionId, tagId)
                        }
                    }

                    NotificationHelper.ACTION_NEW_TAG -> {
                        val remoteInput = RemoteInput.getResultsFromIntent(intent)
                        val newTagName = remoteInput?.getCharSequence(NotificationHelper.KEY_NEW_TAG_NAME)?.toString()?.trim()

                        if (!newTagName.isNullOrBlank()) {
                            // Create new tag with a random pleasant color
                            val randomColor = PRESET_COLORS.random()
                            val tagId = repository.createTag(
                                name = newTagName,
                                colorHex = randomColor
                            )
                            if (tagId != -1L) {
                                repository.tagTransaction(transactionId, tagId)
                            }
                        }
                    }
                }
            } finally {
                // Dismiss the notification
                if (notificationId != -1) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        val PRESET_COLORS = listOf(
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
            "#FFEAA7", "#DDA0DD", "#F39C12", "#E74C3C",
            "#3498DB", "#2ECC71", "#9B59B6", "#1ABC9C",
            "#E67E22", "#34495E", "#16A085", "#C0392B"
        )
    }
}
