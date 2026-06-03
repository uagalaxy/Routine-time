package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        val title = intent.getStringExtra("title") ?: "Daily Rhythm"
        val message = intent.getStringExtra("message") ?: "Time for your scheduled event"
        val type = intent.getStringExtra("type") ?: "" // "reminder" or "activity"

        showNotification(context, id, title, message, type)
    }

    private fun showNotification(
        context: Context,
        id: Long,
        title: String,
        message: String,
        type: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_rhythm_channel_id"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Daily Rhythm Channel"
            val channelDescription = "Notifications for routine events and personalized reminders."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when clicking the notification - opens MainActivity
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val offset = when (type) {
            "reminder" -> 100000
            "alarm" -> 300000
            else -> 200000
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.toInt() + offset,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = android.media.RingtoneManager.getDefaultUri(
            if (type == "alarm") android.media.RingtoneManager.TYPE_ALARM 
            else android.media.RingtoneManager.TYPE_NOTIFICATION
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // Use standard alarm clock android default icon
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) 
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)

        val notificationId = id.toInt() + when (type) {
            "reminder" -> 300000
            "alarm" -> 500000
            else -> 400000
        }
        notificationManager.notify(notificationId, builder.build())
    }
}
