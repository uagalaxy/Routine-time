package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_activities")
data class RoutineActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val startH: Int,
    val startM: Int,
    val endH: Int,
    val endM: Int,
    val color: String // Hex color format, e.g., "#FF123477"
)
