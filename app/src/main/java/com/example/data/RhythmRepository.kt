package com.example.data

import kotlinx.coroutines.flow.Flow

class RhythmRepository(private val dao: RhythmDao) {
    val allActivities: Flow<List<RoutineActivity>> = dao.getAllActivities()
    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()

    suspend fun insertActivity(activity: RoutineActivity): Long = dao.insertActivity(activity)
    suspend fun updateActivity(activity: RoutineActivity) = dao.updateActivity(activity)
    suspend fun deleteActivityById(id: Long) = dao.deleteActivityById(id)
    suspend fun getActivityById(id: Long): RoutineActivity? = dao.getActivityById(id)

    suspend fun insertReminder(reminder: Reminder): Long = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = dao.updateReminder(reminder)
    suspend fun deleteReminderById(id: Long) = dao.deleteReminderById(id)
    suspend fun getReminderById(id: Long): Reminder? = dao.getReminderById(id)

    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()
    suspend fun insertAlarm(alarm: Alarm): Long = dao.insertAlarm(alarm)
    suspend fun updateAlarm(alarm: Alarm) = dao.updateAlarm(alarm)
    suspend fun deleteAlarmById(id: Long) = dao.deleteAlarmById(id)
    suspend fun getAlarmById(id: Long): Alarm? = dao.getAlarmById(id)
}
