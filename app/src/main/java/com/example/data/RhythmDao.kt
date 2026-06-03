package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RhythmDao {
    // Routine Activities
    @Query("SELECT * FROM routine_activities ORDER BY startH * 60 + startM ASC")
    fun getAllActivities(): Flow<List<RoutineActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: RoutineActivity): Long

    @Update
    suspend fun updateActivity(activity: RoutineActivity)

    @Query("DELETE FROM routine_activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("SELECT * FROM routine_activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: Long): RoutineActivity?

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY date ASC, hour ASC, minute ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): Reminder?

    // Alarms
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): Alarm?
}
