package com.example.hello_kotlin.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.hello_kotlin.MainActivity
import com.example.hello_kotlin.R
import java.util.Calendar

class PassbookReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Passbook reminder alarm fired!")
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Notification action - opens MainActivity with an extra to show settings/upload sheet
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PASSBOOK, true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 Time to upload your Passbook!")
            .setContentText("Keep your expense records up to date. Upload your bank statement PDF now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Schedule next month's alarm
        scheduleNextReminder(context)
    }
    
    companion object {
        private const val TAG = "PassbookReminder"
        private const val NOTIFICATION_ID = 9999
        const val EXTRA_OPEN_PASSBOOK = "open_passbook_upload"
        
        fun scheduleNextReminder(context: Context) {
            val prefs = context.getSharedPreferences("expense_tracker", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("passbook_reminder_enabled", false)
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PassbookReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (!enabled) {
                Log.d(TAG, "Reminder is disabled, cancelling alarm.")
                alarmManager.cancel(pendingIntent)
                return
            }
            
            val day = prefs.getInt("passbook_reminder_day", 1)
            val hour = prefs.getInt("passbook_reminder_hour", 9)
            val minute = prefs.getInt("passbook_reminder_minute", 0)
            
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // Adjust day to fit within target month's maximum days
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
                
                // If it has already passed this month, schedule for next month
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.MONTH, 1)
                    val nextMaxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, day.coerceAtMost(nextMaxDay))
                }
            }
            
            Log.d(TAG, "Scheduling next reminder alarm at: ${calendar.time}")
            
            // Schedule the alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
