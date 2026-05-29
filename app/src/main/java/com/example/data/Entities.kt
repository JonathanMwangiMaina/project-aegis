package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long, // timestamp
    val priority: String = "Medium", // "High", "Medium", "Low"
    val isCompleted: Boolean = false,
    val isReminderSet: Boolean = false,
    val reminderTime: Long = 0,
    val isNotionSynced: Boolean = false
)

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val location: String = "",
    val isNotionSynced: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user", "jarvis"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notion_settings")
data class NotionSettings(
    @PrimaryKey val id: Int = 1,
    val apiKey: String = "",
    val databaseId: String = "",
    val isConnected: Boolean = false
)
