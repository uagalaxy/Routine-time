package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val isActive: Boolean = true,
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false
) {
    fun isRepeating(): Boolean {
        return monday || tuesday || wednesday || thursday || friday || saturday || sunday
    }

    fun getRepeatedDaysSummary(): String {
        if (!isRepeating()) return "Once"
        if (monday && tuesday && wednesday && thursday && friday && saturday && sunday) return "Every day"
        if (monday && tuesday && wednesday && thursday && friday && !saturday && !sunday) return "Weekdays"
        if (!monday && !tuesday && !wednesday && !thursday && !friday && saturday && sunday) return "Weekends"
        
        val list = mutableListOf<String>()
        if (monday) list.add("Mon")
        if (tuesday) list.add("Tue")
        if (wednesday) list.add("Wed")
        if (thursday) list.add("Thu")
        if (friday) list.add("Fri")
        if (saturday) list.add("Sat")
        if (sunday) list.add("Sun")
        return list.joinToString(", ")
    }
}
