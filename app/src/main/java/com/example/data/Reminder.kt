package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val date: String, // Format: YYYY-MM-DD
    val hour: Int,
    val minute: Int,
    val isCompleted: Boolean = false
)
