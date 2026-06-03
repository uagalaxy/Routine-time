package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Alarm
import com.example.data.Reminder
import com.example.data.RhythmRepository
import com.example.data.RoutineActivity
import com.example.notification.NotificationScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class RhythmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RhythmRepository

    val activities: StateFlow<List<RoutineActivity>>
    val reminders: StateFlow<List<Reminder>>
    val alarms: StateFlow<List<Alarm>>

    private val _currentTab = MutableStateFlow(TabState.ROUTINE)
    val currentTab = _currentTab.asStateFlow()

    private val _activeActivity = MutableStateFlow<RoutineActivity?>(null)
    val activeActivity = _activeActivity.asStateFlow()

    private val _selectedChartActivity = MutableStateFlow<RoutineActivity?>(null)
    val selectedChartActivity = _selectedChartActivity.asStateFlow()

    private val _currentTime = MutableStateFlow(Date())
    val currentTime = _currentTime.asStateFlow()

    enum class TabState {
        ROUTINE, REMINDERS, ALARMS
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RhythmRepository(database.rhythmDao())

        activities = repository.allActivities.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        reminders = repository.allReminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        alarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Periodic tick to update current time and active activity
        viewModelScope.launch {
            while (true) {
                val now = Date()
                _currentTime.value = now
                updateActiveActivity(now)
                delay(1000) // update every second
            }
        }
    }

    fun selectTab(tab: TabState) {
        _currentTab.value = tab
    }

    fun selectChartActivity(activity: RoutineActivity?) {
        _selectedChartActivity.value = activity
    }

    private fun updateActiveActivity(now: Date) {
        val calendar = Calendar.getInstance().apply { time = now }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val activityList = activities.value
        if (activityList.isEmpty()) {
            _activeActivity.value = null
            return
        }

        var active: RoutineActivity? = null
        for (activity in activityList) {
            val startMinutes = activity.startH * 60 + activity.startM
            var endMinutes = activity.endH * 60 + activity.endM

            if (endMinutes <= startMinutes) {
                // Overnight activity
                val adjustedEnd = endMinutes + 24 * 60
                val adjustedCurrent = if (currentMinutes < startMinutes) currentMinutes + 24 * 60 else currentMinutes
                if (adjustedCurrent in startMinutes until adjustedEnd) {
                    active = activity
                    break
                }
            } else {
                if (currentMinutes in startMinutes until endMinutes) {
                    active = activity
                    break
                }
            }
        }
        _activeActivity.value = active
    }

    // --- Activity Operations ---
    fun saveActivity(id: Long, label: String, startH: Int, startM: Int, endH: Int, endM: Int, color: String) {
        viewModelScope.launch {
            val activity = RoutineActivity(
                id = id,
                label = label,
                startH = startH,
                startM = startM,
                endH = endH,
                endM = endM,
                color = color
            )
            val newId = if (id == 0L) {
                repository.insertActivity(activity)
            } else {
                repository.updateActivity(activity)
                NotificationScheduler.cancelRoutine(getApplication(), id)
                id
            }
            // Schedule new/updated alarm helper
            NotificationScheduler.scheduleRoutine(getApplication(), activity.copy(id = newId))
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            NotificationScheduler.cancelRoutine(getApplication(), id)
            repository.deleteActivityById(id)
            if (_selectedChartActivity.value?.id == id) {
                _selectedChartActivity.value = null
            }
        }
    }

    // --- Reminder Operations ---
    fun saveReminder(id: Long, text: String, date: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val reminder = Reminder(
                id = id,
                text = text,
                date = date,
                hour = hour,
                minute = minute,
                isCompleted = false
            )
            val newId = if (id == 0L) {
                repository.insertReminder(reminder)
            } else {
                repository.updateReminder(reminder)
                NotificationScheduler.cancelReminder(getApplication(), id)
                id
            }
            NotificationScheduler.scheduleReminder(getApplication(), reminder.copy(id = newId))
        }
    }

    fun toggleReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            repository.updateReminder(updated)
            if (updated.isCompleted) {
                NotificationScheduler.cancelReminder(getApplication(), reminder.id)
            } else {
                NotificationScheduler.scheduleReminder(getApplication(), updated)
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            NotificationScheduler.cancelReminder(getApplication(), id)
            repository.deleteReminderById(id)
        }
    }

    // --- Alarm Operations ---
    fun saveAlarm(
        id: Long,
        label: String,
        hour: Int,
        minute: Int,
        isActive: Boolean,
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean
    ) {
        viewModelScope.launch {
            val alarm = Alarm(
                id = id,
                label = label,
                hour = hour,
                minute = minute,
                isActive = isActive,
                monday = monday,
                tuesday = tuesday,
                wednesday = wednesday,
                thursday = thursday,
                friday = friday,
                saturday = saturday,
                sunday = sunday
            )
            val newId = if (id == 0L) {
                repository.insertAlarm(alarm)
            } else {
                repository.updateAlarm(alarm)
                NotificationScheduler.cancelAlarm(getApplication(), id)
                id
            }
            NotificationScheduler.scheduleAlarm(getApplication(), alarm.copy(id = newId))
        }
    }

    fun toggleAlarmActive(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isActive = !alarm.isActive)
            repository.updateAlarm(updated)
            if (updated.isActive) {
                NotificationScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                NotificationScheduler.cancelAlarm(getApplication(), alarm.id)
            }
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            NotificationScheduler.cancelAlarm(getApplication(), id)
            repository.deleteAlarmById(id)
        }
    }
}
