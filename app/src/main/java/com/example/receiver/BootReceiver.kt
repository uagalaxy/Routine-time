package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.RhythmRepository
import com.example.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val database = AppDatabase.getDatabase(context)
            val repository = RhythmRepository(database.rhythmDao())
            
            CoroutineScope(Dispatchers.IO).launch {
                // Reschedule all active activities
                try {
                    val activities = repository.allActivities.first()
                    for (activity in activities) {
                        NotificationScheduler.scheduleRoutine(context, activity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Reschedule reminders
                try {
                    val reminders = repository.allReminders.first()
                    for (reminder in reminders) {
                        if (!reminder.isCompleted) {
                            NotificationScheduler.scheduleReminder(context, reminder)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
