package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val db: AppDatabase) {
    val tasksFlow: Flow<List<Task>> = db.taskDao().getAllTasksFlow()
    val eventsFlow: Flow<List<CalendarEvent>> = db.calendarEventDao().getAllEventsFlow()
    val chatFlow: Flow<List<ChatMessage>> = db.chatMessageDao().getChatHistoryFlow()
    val notionSettingsFlow: Flow<NotionSettings?> = db.notionSettingsDao().getSettingsFlow()

    suspend fun getAllTasks(): List<Task> = db.taskDao().getAllTasks()
    suspend fun insertTask(task: Task): Long = db.taskDao().insertTask(task)
    suspend fun updateTask(task: Task) = db.taskDao().updateTask(task)
    suspend fun deleteTaskById(id: Long) = db.taskDao().deleteById(id)

    suspend fun getAllEvents(): List<CalendarEvent> = db.calendarEventDao().getAllEvents()
    suspend fun insertEvent(event: CalendarEvent): Long = db.calendarEventDao().insertEvent(event)
    suspend fun updateEvent(event: CalendarEvent) = db.calendarEventDao().updateEvent(event)
    suspend fun deleteEventById(id: Long) = db.calendarEventDao().deleteById(id)

    suspend fun insertMessage(message: ChatMessage): Long = db.chatMessageDao().insertMessage(message)
    suspend fun clearChatHistory() = db.chatMessageDao().clearHistory()

    suspend fun getNotionSettings(): NotionSettings? = db.notionSettingsDao().getSettings()
    suspend fun saveNotionSettings(settings: NotionSettings) = db.notionSettingsDao().saveSettings(settings)
}
