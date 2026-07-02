package com.example.hello_kotlin.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.hello_kotlin.MainActivity
import com.example.hello_kotlin.R
import com.example.hello_kotlin.data.db.entity.TagEntity
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "expense_alerts"
    const val CHANNEL_NAME = "Expense Alerts"
    const val ACTION_TAG_SELECTED = "com.example.hello_kotlin.ACTION_TAG_SELECTED"
    const val ACTION_NEW_TAG = "com.example.hello_kotlin.ACTION_NEW_TAG"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    const val EXTRA_TAG_ID = "extra_tag_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val KEY_NEW_TAG_NAME = "key_new_tag_name"

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for detected expense transactions"
            enableVibration(true)
            setShowBadge(true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Shows a heads-up notification with tag selection buttons.
     * Up to 3 most-used tags as action buttons + a "New Tag" RemoteInput action.
     */
    fun showTagSelectionNotification(
        context: Context,
        transactionId: Long,
        amount: Double,
        accountName: String?,
        senderAddress: String,
        topTags: List<TagEntity>
    ) {
        createNotificationChannel(context)

        val notificationId = transactionId.toInt()
        val formattedAmount = currencyFormat.format(amount)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💸 $formattedAmount debited")
            .setContentText(
                if (accountName != null) "From: $accountName • Tap to tag this expense"
                else "From: $senderAddress • Tap to tag this expense"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setGroup("expense_group")

        // Content intent — opens the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, notificationId, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(openAppPendingIntent)

        // Add tag action buttons (up to 3 most used tags)
        topTags.forEachIndexed { index, tag ->
            val tagIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_TAG_SELECTED
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
                putExtra(EXTRA_TAG_ID, tag.id)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val tagPendingIntent = PendingIntent.getBroadcast(
                context, notificationId * 10 + index, tagIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val label = "${tag.emoji ?: "🏷️"} ${tag.name}"
            builder.addAction(0, label, tagPendingIntent)
        }

        // Add "New Tag" action with RemoteInput for inline text entry
        val newTagRemoteInput = RemoteInput.Builder(KEY_NEW_TAG_NAME)
            .setLabel("Enter tag name...")
            .build()

        val newTagIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_NEW_TAG
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val newTagPendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10 + 9, newTagIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val newTagAction = NotificationCompat.Action.Builder(0, "➕ New Tag", newTagPendingIntent)
            .addRemoteInput(newTagRemoteInput)
            .build()
        builder.addAction(newTagAction)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    /**
     * Shows a brief confirmation notification when a transaction is auto-tagged.
     */
    fun showAutoTaggedNotification(
        context: Context,
        transactionId: Long,
        amount: Double,
        tagName: String,
        tagEmoji: String?,
        accountName: String?
    ) {
        createNotificationChannel(context)

        val notificationId = transactionId.toInt()
        val formattedAmount = currencyFormat.format(amount)
        val displayEmoji = tagEmoji ?: "🏷️"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$displayEmoji $formattedAmount → $tagName")
            .setContentText(
                if (accountName != null) "Auto-tagged from $accountName"
                else "Auto-tagged expense"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, notificationId + 1000, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(openAppPendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
