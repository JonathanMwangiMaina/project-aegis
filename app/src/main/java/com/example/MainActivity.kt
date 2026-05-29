package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalendarEvent
import com.example.data.ChatMessage
import com.example.data.Task
import com.example.data.NotionSettings
import com.example.ui.JarvisViewModel
import com.example.ui.SimulatedEmail
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    // Activity Contract to support direct Speech-to-Text extraction
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let {
                viewModel.processVoiceCommand(it)
            }
        } else {
            Toast.makeText(this, "Voice transmission aborted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = JarvisBackground
                ) { innerPadding ->
                    JarvisDashboardScreen(
                        viewModel = viewModel,
                        onMicrophoneClick = { triggerSpeechInput() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun triggerSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.UK) // Elegant British accent
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Instruct J.A.R.V.I.S...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported on this device.", Toast.LENGTH_LONG).show()
            Log.e("JARVIS", "STT Launcher Failure", e)
        }
    }
}

@Composable
fun JarvisDashboardScreen(
    viewModel: JarvisViewModel,
    onMicrophoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val notionSettings by viewModel.notionSettings.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val dailyDigest by viewModel.dailyDigest.collectAsStateWithLifecycle()
    val simulatedEmails by viewModel.simulatedEmails.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("CONSOLE", "AGENDA", "A.I. DIGEST", "NOTION")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. High-Tech System Status Header
        JarvisHeaderPanel()

        // 2. Animated holographic core (Arc Reactor button for quick STT speech commands)
        ArcReactorCore(
            isSpeaking = isSpeaking,
            onCoreClick = onMicrophoneClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Command Tab Controls
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = JarvisBase,
            contentColor = JarvisCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = JarvisCyan
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = title,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_$title")
                )
            }
        }

        // 4. Content Frame rendering active selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(JarvisBase)
        ) {
            when (activeTab) {
                0 -> ConsoleTab(
                    chatHistory = chatHistory,
                    onSendCommand = { viewModel.processVoiceCommand(it) },
                    onQuickSuggest = { viewModel.processVoiceCommand(it) }
                )
                1 -> AgendaTab(
                    tasks = tasks,
                    events = calendarEvents,
                    onAddTask = { t, d, p -> viewModel.addTask(t, d, p) },
                    onAddEvent = { t, l, hr -> viewModel.addCalendarEvent(t, l, hr) },
                    onToggleTask = { viewModel.toggleTaskCompleted(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onDeleteEvent = { viewModel.deleteEvent(it) }
                )
                2 -> AiDigestTab(
                    dailyDigest = dailyDigest,
                    simulatedEmails = simulatedEmails,
                    onTriggerPrioritize = { viewModel.requestDailyScheduleOptimization() }
                )
                3 -> NotionTab(
                    settings = notionSettings,
                    onSave = { key, dbId -> viewModel.saveNotionApiKeys(key, dbId) },
                    onTriggerSync = { viewModel.processVoiceCommand("sync with notion") }
                )
            }
        }
    }
}

@Composable
fun JarvisHeaderPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Status indicator row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                )
                Text(
                    text = "LOCAL CORE ACTIVE",
                    color = JarvisCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure network lock",
                    tint = JarvisTextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "SANDBOX TELEMETRY",
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Header Titles in beautiful light/medium typography layout
        Text(
            text = "Good evening, Sir.",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Systems are nominal. Your local calendar ledger and directives are fully optimized.",
            color = JarvisTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp),
            lineHeight = 18.sp
        )
    }
}

// Custom canvas animation simulating Iron Man's holographic cybernetic arc reactor core
@Composable
fun ArcReactorCore(
    isSpeaking: Boolean,
    onCoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hologram")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 800 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable { onCoreClick() }
                .testTag("microphone_arc_reactor"),
            contentAlignment = Alignment.Center
        ) {
            // Ripple wave backdrop
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(waveScale)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(JarvisBlueGlow, Color.Transparent),
                        center = center,
                        radius = size.width / 1.6f
                    )
                )
                // Draw delicate high-tech concentric arcs
                drawArc(
                    color = JarvisCyan,
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 1.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width / 1.3f, size.height / 1.3f),
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - size.width / 2.6f, center.y - size.height / 2.6f)
                )
            }

            // Inner orbit rings
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .rotate(rotationAngle)
                    .shadow(16.dp, CircleShape, spotColor = JarvisCyan)
                    .border(2.dp, JarvisCyan, CircleShape)
                    .background(JarvisSlate, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.Mic,
                    contentDescription = "Instruction mic trigger",
                    tint = JarvisCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        Text(
            text = if (isSpeaking) "J.A.R.V.I.S. AUDIOPHONIC BROADCAST..." else "TAP REACTOR CORE FOR VOICE FREQUENCY",
            color = if (isSpeaking) JarvisGold else JarvisCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Tab 0: Conversational Console Log
@Composable
fun ConsoleTab(
    chatHistory: List<ChatMessage>,
    onSendCommand: (String) -> Unit,
    onQuickSuggest: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    
    val quickPrompts = listOf(
        "Prioritize my schedule",
        "Remind me to charge suit core",
        "Schedule briefing tomorrow list",
        "Sync local files Notion"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Chat list area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { message ->
                val isJarvis = message.sender == "jarvis"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = if (isJarvis) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isJarvis) 0.dp else 16.dp,
                                    bottomEnd = if (isJarvis) 16.dp else 0.dp
                                )
                            )
                            .background(if (isJarvis) JarvisSlate else JarvisSlateLight)
                            .border(
                                width = 1.dp,
                                color = if (isJarvis) Color(0x1F22D3EE) else Color(0x0DFFFFFF),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isJarvis) 0.dp else 16.dp,
                                    bottomEnd = if (isJarvis) 16.dp else 0.dp
                                )
                            )
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isJarvis) "⚡ J.A.R.V.I.S." else "👤 USER COMMAND",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isJarvis) JarvisCyan else JarvisBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = message.message,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = JarvisTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Suggestions row
        Text(
            text = "SUGGESTED PROTOCOLS",
            color = JarvisTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPrompts.forEach { text ->
                Button(
                    onClick = { onQuickSuggest(text) },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSlate),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = text,
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Send Text Input Command Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSlate, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = rawInput,
                onValueChange = { rawInput = it },
                placeholder = {
                    Text(
                        text = "Transmit cybernetic instructions...",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_textbox_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                )
            )
            IconButton(
                onClick = {
                    if (rawInput.isNotBlank()) {
                        onSendCommand(rawInput)
                        rawInput = ""
                    }
                },
                modifier = Modifier.testTag("submit_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Transmit signal",
                    tint = JarvisCyan
                )
            }
        }
    }
}

// Tab 1: Local Agenda (Tasks with Reminders & Calendar Events)
@Composable
fun AgendaTab(
    tasks: List<Task>,
    events: List<CalendarEvent>,
    onAddTask: (String, String, String) -> Unit,
    onAddEvent: (String, String, Int) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onDeleteEvent: (Long) -> Unit
) {
    var isNewTaskSectionExpanded by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf("Medium") }

    var isNewEventSectionExpanded by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }
    var newEventLocation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // --- Tasks list Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ SECURE DIRECTIVES (${tasks.size})",
                color = JarvisCyan,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { isNewTaskSectionExpanded = !isNewTaskSectionExpanded }) {
                Icon(
                    imageVector = if (isNewTaskSectionExpanded) Icons.Default.ExpandLess else Icons.Default.Add,
                    contentDescription = "Expand task list input",
                    tint = JarvisCyan
                )
            }
        }

        AnimatedVisibility(visible = isNewTaskSectionExpanded) {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                border = BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Directive Title", color = JarvisTextPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("SEVERITY CRITICALITY", fontSize = 10.sp, color = JarvisTextSecondary, fontFamily = FontFamily.Monospace)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { level ->
                            val selected = newTaskPriority == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) JarvisSlateLight else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selected) JarvisCyan else JarvisTextSecondary,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { newTaskPriority = level }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    color = if (selected) JarvisCyan else JarvisTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                onAddTask(newTaskTitle, "Manually created systems log", newTaskPriority)
                                newTaskTitle = ""
                                isNewTaskSectionExpanded = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LOCK DIRECTIVE IN DATABASE", color = JarvisBase, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting local entries, Sir. No active database directives discovered.",
                    color = JarvisTextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            tasks.forEach { t ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSlate),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = t.isCompleted,
                                onCheckedChange = { onToggleTask(t) },
                                colors = CheckboxDefaults.colors(checkedColor = JarvisCyan, checkmarkColor = JarvisBase)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = t.title,
                                    color = if (t.isCompleted) JarvisTextSecondary else JarvisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (t.priority) {
                                                    "High" -> Color(0x33EF4444)
                                                    "Medium" -> Color(0x1F22D3EE)
                                                    else -> Color(0x0DFFFFFF)
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = when (t.priority) {
                                                    "High" -> Color(0xFFEF4444)
                                                    "Medium" -> JarvisCyan
                                                    else -> JarvisTextSecondary
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = t.priority,
                                            color = when (t.priority) {
                                                "High" -> Color(0xFFEF4444)
                                                "Medium" -> JarvisCyan
                                                else -> JarvisTextSecondary
                                            },
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (t.isNotionSynced) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sync: Notion",
                                            color = JarvisCyan,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { onDeleteTask(t.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Purge local task", tint = Color.Red)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Calendar list Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅 CALENDAR BRIEFINGS (${events.size})",
                color = JarvisCyan,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { isNewEventSectionExpanded = !isNewEventSectionExpanded }) {
                Icon(
                    imageVector = if (isNewEventSectionExpanded) Icons.Default.ExpandLess else Icons.Default.Add,
                    contentDescription = "Expand calendar event input",
                    tint = JarvisCyan
                )
            }
        }

        AnimatedVisibility(visible = isNewEventSectionExpanded) {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                border = BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = newEventTitle,
                        onValueChange = { newEventTitle = it },
                        label = { Text("Event Directive", color = JarvisTextPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newEventLocation,
                        onValueChange = { newEventLocation = it },
                        label = { Text("Location", color = JarvisTextPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newEventTitle.isNotBlank()) {
                                onAddEvent(newEventTitle, if (newEventLocation.isNotBlank()) newEventLocation else "Stark Industries", 2)
                                newEventTitle = ""
                                newEventLocation = ""
                                isNewEventSectionExpanded = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("COMMIT TO CALENDAR PROTOCOL", color = JarvisBase, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Calendar ledger clear, Sir.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            events.forEach { e ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSlate),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = e.title,
                                color = JarvisTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Location tag", tint = JarvisCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = e.location,
                                    color = JarvisTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                if (e.isNotionSynced) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "* Notion",
                                        color = JarvisCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onDeleteEvent(e.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Purge local event", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// Tab 2: Daily A.I. Prioritized Scheduler & Digest (Conversational Optimization Display)
@Composable
fun AiDigestTab(
    dailyDigest: String,
    simulatedEmails: List<SimulatedEmail>,
    onTriggerPrioritize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JarvisSlate),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A.I. STRATEGIC PRIORITIZER CORE",
                    color = JarvisCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Using Advanced Reasoning models, I will dynamically reorder, audit, and provide a secure daily optimization brief of all scheduled protocols.",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTriggerPrioritize,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    modifier = Modifier.fillMaxWidth().testTag("optimize_prioritize_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Compute details", tint = JarvisBase)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("INITIATE PRIORITIZATION RUN", color = JarvisBase, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (dailyDigest.isNotBlank()) {
            Text(
                text = "⚡ TELEMETRY TRANSMISSION BRIEF",
                color = JarvisGold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisSlateLight),
                border = BorderStroke(1.dp, Color(0x1FFBBF24))
            ) {
                Text(
                    text = dailyDigest,
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Send Emails Queue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📨 OUTGOING TRANSMISSION QUEUE (${simulatedEmails.size})",
                color = JarvisCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (simulatedEmails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No direct email telemetry in system buffer. Say: 'Send email to Stark' to trigger a dispatch.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            simulatedEmails.forEach { email ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSlate),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "To: ${email.recipient}",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Subject: ${email.subject}",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = email.body,
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// Tab 3: Notion Database Settings & Sync Status layout
@Composable
fun NotionTab(
    settings: NotionSettings?,
    onSave: (String, String) -> Unit,
    onTriggerSync: () -> Unit
) {
    var notionKey by remember { mutableStateOf(settings?.apiKey ?: "") }
    var notionDbId by remember { mutableStateOf(settings?.databaseId ?: "") }

    LaunchedEffect(settings) {
        if (settings != null) {
            notionKey = settings.apiKey
            notionDbId = settings.databaseId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JarvisSlate),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "NOTION MAINFRAME CONNECTION INTERFACE",
                    color = JarvisCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Configure third-party credentials locally to seamlessly sync priorities to Notion. Access parameters are compiled and encrypted only inside device sandbox storage.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = notionKey,
                    onValueChange = { notionKey = it },
                    label = { Text("Notion API Token", color = JarvisTextPrimary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("notion_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notionDbId,
                    onValueChange = { notionDbId = it },
                    label = { Text("Notion Database ID", color = JarvisTextPrimary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("notion_db_id_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSave(notionKey, notionDbId) },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        modifier = Modifier.weight(1f).testTag("notion_save_button")
                    ) {
                        Text("SAVE CONFIG", color = JarvisBase, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = onTriggerSync,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSlateLight),
                        border = BorderStroke(1.dp, Color(0x3322D3EE)),
                        modifier = Modifier.weight(1f).testTag("notion_sync_button")
                    ) {
                        Text("SYNC DATA", color = JarvisCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High tech visual ledger terminal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JarvisBase),
            border = BorderStroke(0.6.dp, Color(0x1F94A3B8))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "TRANSMISSION DIAGNOSTICS",
                    color = JarvisTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("API Handshake Authentication", color = JarvisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = if (settings?.isConnected == true) "AUTHORIZED" else "STANDBY",
                        color = if (settings?.isConnected == true) JarvisCyan else JarvisGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Direct Uplink Protocol State", color = JarvisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = if (settings?.isConnected == true) "ONLINE" else "STANDBY",
                        color = if (settings?.isConnected == true) JarvisCyan else JarvisGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Local SQLite Sandbox Guard", color = JarvisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "SECURE",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
