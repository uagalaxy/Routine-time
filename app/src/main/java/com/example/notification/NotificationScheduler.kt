package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Reminder
import com.example.data.RoutineActivity
import com.example.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun scheduleReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Parse reminder date and time
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = "${reminder.date} ${String.format("%02d:%02d", reminder.hour, reminder.minute)}"
        
        val date = try {
            formatter.parse(dateStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reminder date time: $dateStr", e)
            null
        } ?: return

        val targetTimeMs = date.time
        if (targetTimeMs <= System.currentTimeMillis()) {
            // Already happened, skip
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("id", reminder.id)
            putExtra("title", "Daily Rhythm Reminder")
            putExtra("message", reminder.text)
            putExtra("type", "reminder")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            }
            Log.d(TAG, "Successfully scheduled alarm for reminder: ${reminder.text} at $dateStr")
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if matching permission denied
            alarmManager.set(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            Log.e(TAG, "SecurityException on exact alarm. Scheduled non-exact backup.", e)
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 100000,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for reminder ID: $reminderId")
        }
    }

    fun scheduleRoutine(context: Context, activity: RoutineActivity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, activity.startH)
            set(Calendar.MINUTE, activity.startM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Schedule for tomorrow
        }

        val targetTimeMs = calendar.timeInMillis

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("id", activity.id)
            putExtra("title", "Daily Rhythm Routine")
            putExtra("message", "Time for your activity: ${activity.label}")
            putExtra("type", "activity")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activity.id.toInt() + 200000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                targetTimeMs,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled repeating routine alarm for: ${activity.label} at ${activity.startH}:${activity.startM}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling repeating routine", e)
        }
    }

    fun cancelRoutine(context: Context, activityId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId.toInt() + 200000,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for routine ID: $activityId")
        }
    }

    fun scheduleAlarm(context: Context, alarm: com.example.data.Alarm) {
        if (!alarm.isActive) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (!alarm.isRepeating()) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("id", alarm.id)
                putExtra("title", "Daily Rhythm Alarm")
                putExtra("message", alarm.label.ifEmpty { "Alarm" })
                putExtra("type", "alarm")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (alarm.id * 10).toInt() + 500000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
                Log.d(TAG, "Scheduled once alarm: ${alarm.label} at ${alarm.hour}:${alarm.minute}")
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            val days = listOf(
                Pair(alarm.sunday, Calendar.SUNDAY),
                Pair(alarm.monday, Calendar.MONDAY),
                Pair(alarm.tuesday, Calendar.TUESDAY),
                Pair(alarm.wednesday, Calendar.WEDNESDAY),
                Pair(alarm.thursday, Calendar.THURSDAY),
                Pair(alarm.friday, Calendar.FRIDAY),
                Pair(alarm.saturday, Calendar.SATURDAY)
            )

            for (i in days.indices) {
                val (enabled, dayOfWeek) = days[i]
                val requestCode = (alarm.id * 10 + (i + 1)).toInt() + 500000
                
                // cancel existing first
                val cancelIntent = Intent(context, AlarmReceiver::class.java)
                val existingPending = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    cancelIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (existingPending != null) {
                    alarmManager.cancel(existingPending)
                    existingPending.cancel()
                }

                if (enabled) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_WEEK, dayOfWeek)
                    }

                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }

                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("id", alarm.id)
                        putExtra("title", "Daily Rhythm Alarm")
                        putExtra("message", alarm.label.ifEmpty { "Alarm" })
                        putExtra("type", "alarm")
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY * 7,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled repeating day $dayOfWeek for alarm: ${alarm.label} at ${alarm.hour}:${alarm.minute}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error scheduling repeating day $dayOfWeek", e)
                    }
                }
            }
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        
        // Cancel single-shot request code (id * 10)
        val oncePending = PendingIntent.getBroadcast(
            context,
            (alarmId * 10).toInt() + 500000,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (oncePending != null) {
            alarmManager.cancel(oncePending)
            oncePending.cancel()
        }

        // Cancel repeating day request codes (id * 10 + 1 to 7)
        for (i in 1..7) {
            val requestCode = (alarmId * 10 + i).toInt() + 500000
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
