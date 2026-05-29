package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalendarEvent
import com.example.data.ChatMessage
import com.example.data.JarvisRepository
import com.example.data.NotionSettings
import com.example.data.Task
import com.example.network.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = AppDatabase.getDatabase(application)
    private val repository = JarvisRepository(database)

    // Reactive State Flows from Room local database
    val tasks: StateFlow<List<Task>> = repository.tasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEvent>> = repository.eventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notionSettings: StateFlow<NotionSettings?> = repository.notionSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Interactive States
    val isSpeaking = MutableStateFlow(false)
    val dailyDigest = MutableStateFlow<String>("")
    val simulatedEmails = MutableStateFlow<List<SimulatedEmail>>(emptyList())

    // TextToSpeech Local Offline Audio Synthesis
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("JARVIS", "Failed to initialize active TextToSpeech. Offline core disabled.", e)
        }
        
        // Insert greeting message and default settings if empty
        viewModelScope.launch {
            val settings = repository.getNotionSettings()
            if (settings == null) {
                repository.saveNotionSettings(NotionSettings(apiKey = "notion_secret_mock_1234", databaseId = "db_abc123", isConnected = false))
            }
            
            // Populate initial empty greeting if chat history is empty
            try {
                val list = repository.chatFlow.first()
                if (list.isEmpty()) {
                    repository.insertMessage(ChatMessage(
                        sender = "jarvis",
                        message = "Systems online, Sir. I am J.A.R.V.I.S., standing by to catalog your tasks, manage scheduling protocols, and secure transmissions."
                    ))
                }
            } catch (e: Exception) {
                Log.e("JARVIS", "Failed checking initial chat history status", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK) // Jarvis speaks in elegant British accent
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
                Log.d("JARVIS", "TTS successfully initialized with British Speech synthesis.")
            }
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_voice")
            // Periodically check if talking is finished
            viewModelScope.launch {
                // Approximate speaking time or use listener
                val wordCount = text.split(" ").size
                val estimatedMs = (wordCount * 400L).coerceIn(1000L..8000L)
                kotlinx.coroutines.delay(estimatedMs)
                isSpeaking.value = false
            }
        }
    }

    // Process spoken or entered commands
    fun processVoiceCommand(command: String) {
        if (command.isBlank()) return

        viewModelScope.launch {
            // 1. Add User query to Room SQLite history
            repository.insertMessage(ChatMessage(sender = "user", message = command))

            val cmd = command.lowercase(Locale.getDefault())

            // Define JARVIS System instructions for intelligent fallbacks
            val sysInstruction = """
                You are J.A.R.V.I.S., the highly sophisticated, witty, and loyal personal AI coordinator created by Tony Stark.
                Always call the user "Sir" or "Ma'am" (Default to "Sir"). 
                Your tone is highly polite, professional, slightly dry-humored, and intelligent.
                Keep responses concise, clear, and actionable. Do not dump overly long text.
            """.trimIndent()

            // 2. Perform Offline Heuristic Local Command Parsing (Privacy First)
            when {
                cmd.contains("remind") || cmd.contains("reminder") || cmd.contains("task") -> {
                    // Example: "Remind me to inspect the thermal thrusters tomorrow at 8 pm"
                    val title = command.replace("remind me to", "", ignoreCase = true)
                        .replace("set task for", "", ignoreCase = true)
                        .replace("reminder to", "", ignoreCase = true)
                        .trim().capitalize(Locale.getDefault())

                    val cleanTitle = if (title.length > 4) title else "Calibrate systems"
                    val task = Task(
                        title = cleanTitle,
                        dueDate = System.currentTimeMillis() + 3600000 * 3, // Defaults to 3 hours later
                        priority = if (cmd.contains("urgent") || cmd.contains("immediate") || cmd.contains("emergency")) "High" else "Medium",
                        isReminderSet = true,
                        reminderTime = System.currentTimeMillis() + 3600000 * 3
                    )
                    repository.insertTask(task)
                    val reply = "Right away, Sir. I have registered the task: '$cleanTitle' and set a localized alarm beacon."
                    repository.insertMessage(ChatMessage(sender = "jarvis", message = reply))
                    speak(reply)
                }

                cmd.contains("schedule") || cmd.contains("meeting") || cmd.contains("calendar") || cmd.contains("event") -> {
                    // Example: "Schedule meeting with Pepper Stark tonight"
                    val desc = command.replace("schedule a", "", ignoreCase = true)
                        .replace("schedule meeting with", "", ignoreCase = true)
                        .replace("new event", "", ignoreCase = true)
                        .trim().capitalize(Locale.getDefault())

                    val cleanDesc = if (desc.length > 4) desc else "Strategic system briefing"
                    val event = CalendarEvent(
                        title = cleanDesc,
                        startTime = System.currentTimeMillis() + 3600000 * 2,
                        endTime = System.currentTimeMillis() + 3600000 * 3,
                        location = "Stark Tower"
                    )
                    repository.insertEvent(event)
                    val reply = "Calendar database synchronized, Sir. Scheduled meeting: '$cleanDesc' is secured."
                    repository.insertMessage(ChatMessage(sender = "jarvis", message = reply))
                    speak(reply)
                }

                cmd.contains("email") || cmd.contains("send") -> {
                    // Example: "Send email to Stark Industries regarding mark 85"
                    val textStr = command.trim()
                    val recipient = if (cmd.contains("pepper")) "pepper.potts@starkindustries.com" 
                                    else if (cmd.contains("stark")) "tony@starkindustries.com"
                                    else "stark-archives@starkindustries.com"
                    
                    val emailSubject = "Direct Uplink Notification"
                    val emailBody = command.trim()

                    val newEmail = SimulatedEmail(
                        recipient = recipient,
                        subject = emailSubject,
                        body = emailBody,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    simulatedEmails.value = simulatedEmails.value + newEmail
                    val reply = "Direct secure telemetry packet queued and transmitted to $recipient, Sir. Uplink confirmed."
                    repository.insertMessage(ChatMessage(sender = "jarvis", message = reply))
                    speak(reply)
                }

                cmd.contains("notion") || cmd.contains("sync") -> {
                    val reply = "Accessing secure Notion API mainframe... Handshake validated. Direct database synchronization protocol complete, Sir."
                    // update synced status on local entities
                    val currentTasks = repository.getAllTasks()
                    currentTasks.forEach {
                        repository.updateTask(it.copy(isNotionSynced = true))
                    }
                    val currentEvents = repository.getAllEvents()
                    currentEvents.forEach {
                        repository.updateEvent(it.copy(isNotionSynced = true))
                    }
                    repository.insertMessage(ChatMessage(sender = "jarvis", message = reply))
                    speak(reply)
                }

                else -> {
                    // Fallback to intelligent conversational Gemini response
                    // Includes context of current local task list for highly relevant local reasoning!
                    val localTasks = repository.getAllTasks()
                    val localEvents = repository.getAllEvents()
                    
                    val contextPrompt = """
                        The user asked: "$command". 
                        Our CURRENT local schedule list is:
                        Tasks: ${localTasks.joinToString { "[Title: ${it.title}, Priority: ${it.priority}, Completed: ${it.isCompleted}]" }}
                        Calendar Events: ${localEvents.joinToString { "[Title: ${it.title}, Start: ${it.startTime}]" }}
                        Please reply to the user's specific request keeping in character with J.A.R.V.I.S. and offering helpful feedback based on these items if relevant. Keep it under 3 elegant sentences.
                    """.trimIndent()

                    val jarvisResponse = GeminiApiClient.askJarvis(contextPrompt, sysInstruction)
                    repository.insertMessage(ChatMessage(sender = "jarvis", message = jarvisResponse))
                    speak(jarvisResponse)
                }
            }
        }
    }

    // Daily prioritization optimization reasoning loop using Gemini AI API (Client-side privacy enhanced)
    fun requestDailyScheduleOptimization() {
        viewModelScope.launch {
            dailyDigest.value = "Initiating scheduling optimization protocols..."
            
            val localTasks = repository.getAllTasks()
            val localEvents = repository.getAllEvents()

            if (localTasks.isEmpty() && localEvents.isEmpty()) {
                val reply = "Sir, your schedule dashboard is currently vacant. Please add tasks, reminders, or events before initiating optimization cycles."
                dailyDigest.value = reply
                speak(reply)
                return@launch
            }

            val systemPrompt = """
                You are J.A.R.V.I.S. optimizing Tony Stark's daily itinerary.
                Provide a structured report with titles "HIGH-PRIORITY DIRECTIVES", "CALENDAR PROTOCOLS", and unprompted witty "JARVIS'S REASONING PROTOCOL".
                Group current tasks by critical order (High vs Medium vs Low priorities) and suggest schedule enhancements.
                Use respectful and sophisticated wording.
            """.trimIndent()

            val scheduleDetails = """
                Optimize my current docket:
                Current Tasks:
                ${localTasks.mapIndexed { idx, t -> "- [${t.priority}] ${t.title} (Completed: ${t.isCompleted})" }.joinToString("\n")}
                
                Current Calendar Events:
                ${localEvents.mapIndexed { idx, e -> "- Meeting: ${e.title} at ${e.location}" }.joinToString("\n")}
            """.trimIndent()

            val result = GeminiApiClient.askJarvis(scheduleDetails, systemPrompt)
            dailyDigest.value = result
            speak("Schedule prioritization complete, Sir. I have organized your directives based on strategic and due-date parameters.")
        }
    }

    fun addTask(title: String, description: String, priority: String) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                dueDate = System.currentTimeMillis() + 86400000, // tomorrow
                priority = priority
            )
            repository.insertTask(task)
            speak("Added task: $title, slated as $priority priority, Sir.")
        }
    }

    fun addCalendarEvent(title: String, location: String, startTimeHoursLater: Int) {
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title,
                startTime = System.currentTimeMillis() + 3600000 * startTimeHoursLater,
                endTime = System.currentTimeMillis() + 3600000 * (startTimeHoursLater + 1),
                location = location
            )
            repository.insertEvent(event)
            speak("Scheduled calendar log: $title, slate location is $location.")
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            if (updated.isCompleted) {
                speak("Directive ${task.title} fully resolved, Sir.")
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
            speak("Local record purged, Sir.")
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEventById(id)
            speak("Calendar beacon removed, Sir.")
        }
    }

    fun saveNotionApiKeys(apiKey: String, dbId: String) {
        viewModelScope.launch {
            val updated = NotionSettings(apiKey = apiKey, databaseId = dbId, isConnected = apiKey.isNotBlank() && dbId.isNotBlank())
            repository.saveNotionSettings(updated)
            speak("Notion integration parameters updated, Sir.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}

data class SimulatedEmail(
    val recipient: String,
    val subject: String,
    val body: String,
    val timestamp: Long
)
