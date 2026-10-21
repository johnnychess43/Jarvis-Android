package com.jarvis.android

import android.Manifest
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val Background = Color(0xFF07111D)
private val Panel = Color(0xFF101F30)
private val Cyan = Color(0xFF43D9F5)
private val Muted = Color(0xFF9BB2C8)
private val White = Color(0xFFEAF7FF)

data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var onSpeechResult: ((String) -> Unit)? = null
    private var onSpeechError: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            JarvisScreen(
                onSpeak = { speak(it) },
                onPhoneCommand = { executePhoneCommand(it) },
                onListen = { result, error -> startListening(result, error) },
                onStopListening = { speechRecognizer?.cancel() }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-${System.currentTimeMillis()}")
    }

    private fun startListening(result: (String) -> Unit, error: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            result("PERMISSION_REQUIRED")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            error()
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(errorCode: Int) { error() }
                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (spoken.isNotBlank()) result(spoken) else error()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
            }
            recognizer.startListening(intent)
        }
    }

    private fun executePhoneCommand(raw: String): String? {
        val command = raw.trim()
        val lower = command.lowercase(Locale.getDefault())
        val prefs = getSharedPreferences("jarvis_plugin_state", MODE_PRIVATE)

        fun launch(intent: Intent, success: String): String {
            return try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                success
            } catch (_: Exception) {
                "I couldn't open that on this phone. The app may not be installed or the action may not be supported."
            }
        }

        // Android-native adaptations of the uploaded desktop plugins.
        // Features requiring desktop UI automation or a connected AI model are
        // explicitly kept as guided/manual workflows rather than faked.
        if (lower == "hey jarvis" || lower == "wake up jarvis" || lower == "wake up") {
            return "I'm here. Always-on wake-word listening is not enabled; tap the microphone button to speak a command."
        }
        if (lower.contains("water") || lower.contains("drink water") ||
            lower.contains("hydration") || lower.contains("maji")) {
            val current = prefs.getInt("water_ml", 0)
            val goal = prefs.getInt("water_goal_ml", 2000)
            when {
                lower.contains("how much") || lower.contains("status") || lower.contains("today") ->
                    return "Today's water log: $current ml of $goal ml. Say 'I drank 250 ml of water' to add a drink."
                lower.contains("goal") || lower.contains("target") -> {
                    val amount = Regex("(\\d{2,5})").find(lower)?.value?.toIntOrNull()
                    if (amount != null && amount in 250..10000) {
                        prefs.edit().putInt("water_goal_ml", amount).apply()
                        return "Daily water goal saved as $amount ml."
                    }
                    return "Tell me a daily water goal in millilitres, for example: set water goal to 2500 ml."
                }
                lower.contains("stop") || lower.contains("reminder") || lower.contains("remind") ->
                    return "Hydration tracking is available, but scheduled background notifications are not wired yet. Your water log is saved on this phone."
                else -> {
                    val amount = Regex("(\\d{2,5})\\s*(ml|millilit|millilitre|milliliter)").find(lower)
                        ?.groupValues?.get(1)?.toIntOrNull() ?: 250
                    val updated = (current + amount).coerceAtMost(100000)
                    prefs.edit().putInt("water_ml", updated).apply()
                    return "Logged $amount ml. Today's total is $updated ml of $goal ml."
                }
            }
        }

        if (lower.contains("quiz me") || lower.startsWith("test me") || lower.contains("start a quiz")) {
            return "Quiz plugin is on the roadmap. I can prepare and mark quizzes once the AI provider is connected; for now try a topic like 'quiz me on biology' after AI setup."
        }
        if (lower.contains("explain this document") || lower.contains("review this document") ||
            lower.contains("what am i signing") || lower.contains("review this contract")) {
            return "Document Review is planned. Paste the document text here once AI is connected; JARVIS should explain clauses and uncertainties, not replace professional legal advice."
        }
        if (lower.contains("calories") || lower.contains("nutritional value") || lower.contains("how many calories")) {
            return launch(Intent("android.media.action.IMAGE_CAPTURE"), "Opening the camera. Food recognition and calorie estimation need the AI vision connection, so I won't guess nutrition from a photo yet.")
        }
        if (lower.contains("youtube") && (lower.contains("pause") || lower.contains("resume") ||
            lower.contains("skip forward") || lower.contains("rewind") || lower.contains("captions") ||
            lower.contains("volume") || lower.contains("fullscreen") || lower.contains("next video"))) {
            return "YouTube controls plugin noted. Android does not let this starter send desktop keyboard shortcuts to another app. Open YouTube and use its player controls, or ask me to open/search YouTube."
        }
        if (lower.contains("search youtube for ") || lower.contains("find on youtube ")) {
            val query = command.substringAfter("for ", command.substringAfter("find on YouTube ", "")).trim()
            val uri = android.net.Uri.parse("https://www.youtube.com/results?search_query=" + android.net.Uri.encode(query))
            return launch(Intent(Intent.ACTION_VIEW, uri), "Searching YouTube for $query.")
        }
        if (lower.contains("message on discord") || lower.contains("send a discord message") ||
            lower.contains("discord message")) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, command.substringAfter("tell him", command.substringAfter("say", command)))
            }
            return launch(Intent.createChooser(share, "Choose where to share this draft"), "Opening the Android share sheet. Review the message and recipient before sending.")
        }
        if (lower.contains("take over this conversation") || lower.contains("chat takeover")) {
            return "Chat Takeover is kept draft-only for safety: I won't monitor private chats or auto-send replies in the background. You can ask me to draft a reply, then review and send it yourself."
        }
        if (lower.contains("notification") || lower.contains("notifications")) {
            return launch(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName),
                "Opening JARVIS notification settings. Reading other apps' notifications requires a separate user-granted Notification Access service, which is not enabled in this starter.")
        }
        if (lower.contains("record my screen") || lower.contains("screen recording") || lower.contains("start recording my screen")) {
            return "Screen Recorder plugin is not wired yet. On Android, start Screen Record from Quick Settings; Android requires a system consent prompt before an app can capture the screen."
        }
        if (lower.contains("screenshot") || lower.contains("annotate my screen") || lower.contains("circle the button")) {
            return "Screenshot & Annotate is planned. Android requires screen-capture consent and an AI vision connection to identify and circle controls; neither is enabled yet."
        }
        if (lower.contains("game update") || lower.contains("update my game") || lower.contains("update game")) {
            val uri = android.net.Uri.parse("https://play.google.com/store/search?q=" + android.net.Uri.encode(command) + "&c=apps")
            return launch(Intent(Intent.ACTION_VIEW, uri), "Opening Play Store search. Steam/Epic desktop game updater scripts cannot run as Android plugins.")
        }
        if (lower.contains("create an excel") || lower.contains("make a spreadsheet") ||
            lower.contains("create a spreadsheet") || lower.contains("excel table")) {
            val csv = "Item,Amount\\nExample 1,100\\nExample 2,200\\nTotal,=SUM(B2:B3)"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "JARVIS spreadsheet draft")
                putExtra(Intent.EXTRA_TEXT, csv)
            }
            return launch(Intent.createChooser(share, "Share spreadsheet CSV draft"), "Prepared a sample CSV draft to share. Full Excel workbook generation and charts need a file-export workflow.")
        }

        if (lower.startsWith("open ")) {
            val target = lower.removePrefix("open ").trim()
            val settingsIntent = when {
                target.contains("wi-fi") || target.contains("wifi") || target.contains("internet") ->
                    Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                target.contains("bluetooth") -> Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                target.contains("settings") -> Intent(android.provider.Settings.ACTION_SETTINGS)
                target.contains("camera") -> Intent("android.media.action.IMAGE_CAPTURE")
                target.contains("youtube") -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                target.contains("maps") || target.contains("google maps") ->
                    packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                target.contains("chrome") -> packageManager.getLaunchIntentForPackage("com.android.chrome")
                target.contains("phone") || target.contains("dialer") -> Intent(Intent.ACTION_DIAL)
                target.contains("messages") || target.contains("sms") -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
                else -> null
            }
            if (settingsIntent != null) {
                return launch(settingsIntent, "Opening $target.")
            }
            return "I don't have a launcher shortcut for '$target' yet. Try YouTube, Maps, Chrome, Camera, Wi-Fi settings, Bluetooth settings, or phone settings."
        }

        if (lower.startsWith("call ")) {
            val contactOrNumber = command.substringAfter(" ", "").trim()
            if (contactOrNumber.any { it.isDigit() } && contactOrNumber.count { it.isDigit() } >= 3) {
                val uri = android.net.Uri.parse("tel:${android.net.Uri.encode(contactOrNumber)}")
                return launch(Intent(Intent.ACTION_DIAL, uri), "Opening the dialler for $contactOrNumber. Review the number and tap call.")
            }
            return launch(Intent(Intent.ACTION_DIAL), "Opening your dialler. Choose $contactOrNumber from your contacts to place the call.")
        }

        if (lower.startsWith("text ") || lower.startsWith("send a message") || lower.startsWith("send message")) {
            val body = if (lower.startsWith("text ")) command.substringAfter(" ").trim() else ""
            val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")).apply {
                if (body.isNotBlank()) putExtra("sms_body", body)
            }
            return launch(intent, "Opening your messaging app. Review the recipient and message before sending.")
        }

        if (lower.contains("set an alarm") || lower.startsWith("set alarm")) {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
            return launch(intent, "Opening the clock app so you can set your alarm.")
        }

        if (lower.contains("take a photo") || lower.contains("open camera")) {
            return launch(Intent("android.media.action.IMAGE_CAPTURE"), "Opening the camera.")
        }

        if (lower.contains("search the web for ")) {
            val query = command.substringAfter("search the web for ", "").trim()
            if (query.isNotBlank()) {
                val uri = android.net.Uri.parse("https://www.google.com/search?q=" + android.net.Uri.encode(query))
                return launch(Intent(Intent.ACTION_VIEW, uri), "Searching the web for $query.")
            }
        }

        if (lower.contains("open wi-fi settings") || lower.contains("open wifi settings")) {
            return launch(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), "Opening Wi-Fi settings.")
        }
        if (lower.contains("open settings")) {
            return launch(Intent(android.provider.Settings.ACTION_SETTINGS), "Opening phone settings.")
        }

        return null
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisScreen(
    onSpeak: (String) -> Unit,
    onPhoneCommand: (String) -> String?,
    onListen: ((String) -> Unit, () -> Unit) -> Unit,
    onStopListening: () -> Unit
) {
    val messages = remember {
        mutableStateListOf(ChatMessage("JARVIS online. Chat or tap the microphone to speak. AI connection is not configured yet.", false))
    }
    var input by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var showPermissionHelp by remember { mutableStateOf(false) }
    var showPlugins by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            listening = true
            onListen({ spoken ->
                listening = false
                if (spoken != "PERMISSION_REQUIRED") {
                    input = spoken
                } else showPermissionHelp = true
            }, { listening = false })
        } else showPermissionHelp = true
    }

    fun sendMessage() {
        val question = input.trim()
        if (question.isEmpty()) return
        messages.add(ChatMessage(question, true))
        input = ""
        val answer = onPhoneCommand(question) ?: localDemoReply(question)
        messages.add(ChatMessage(answer, false))
        onSpeak(answer)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, background = Background, surface = Panel,
        onBackground = White, onSurface = White
    )) {
        Column(
            modifier = Modifier.fillMaxSize().background(Background).padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(Cyan.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("J", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("J.A.R.V.I.S.", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("PERSONAL AI • ANDROID", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
                TextButton(onClick = { showPlugins = true }) {
                    Text("PLUGINS", color = Cyan, fontSize = 10.sp)
                }
                Surface(color = Color(0xFF12372F), shape = RoundedCornerShape(20.dp)) {
                    Text("● ONLINE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF72E5B2), fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Surface(
                color = Panel, shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).background(Cyan.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(66.dp).background(Cyan.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Text("◉", color = Cyan, fontSize = 38.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(if (listening) "LISTENING…" else "READY WHEN YOU ARE", color = Cyan, fontSize = 11.sp, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Your assistant, in your pocket.", color = Muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("CONVERSATION", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (message.fromUser) Color(0xFF153D51) else Panel,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(if (message.fromUser) "YOU" else "JARVIS", color = if (message.fromUser) Cyan else Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(message.text, color = White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (listening) {
                        onStopListening()
                        listening = false
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            listening = true
                            onListen({ spoken ->
                                listening = false
                                if (spoken == "PERMISSION_REQUIRED") showPermissionHelp = true else input = spoken
                            }, { listening = false })
                        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) {
                    Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Voice input", tint = Cyan)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    decorationBox = { inner ->
                        Box {
                            if (input.isEmpty()) Text("Ask JARVIS anything…", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                IconButton(onClick = { sendMessage() }) {
                    Icon(Icons.Default.Send, contentDescription = "Send message", tint = Cyan)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("Voice replies • Basic phone commands • AI provider not connected", color = Muted, fontSize = 10.sp)
            }
        }
    }
    if (showPlugins) {
        val pluginExamples = listOf(
            "Water Tracker" to "I drank 250 ml of water",
            "Calorie Counter" to "How many calories is this?",
            "Document Review" to "Explain this document",
            "Excel Writer" to "Create a spreadsheet",
            "Quiz" to "Quiz me on biology",
            "YouTube Controller" to "Search YouTube for physics notes",
            "Discord Messenger" to "Send a Discord message saying I'll be late",
            "Chat Takeover" to "Take over this conversation",
            "Notification Reader" to "Check my notifications",
            "Screen Recorder" to "Start recording my screen",
            "Screenshot & Annotate" to "Circle the button on my screen",
            "Wake Word" to "Hey JARVIS",
            "Game Updater" to "Update my game"
        )
        AlertDialog(
            onDismissRequest = { showPlugins = false },
            title = { Text("JARVIS Plugin Deck") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pluginExamples) { item ->
                        Surface(
                            color = Panel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(item.first, color = Cyan, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = {
                                    input = item.second
                                    showPlugins = false
                                }) { Text("Try: ${item.second}", color = White) }
                            }
                        }
                    }
                    item {
                        Text(
                            "These are Android adaptations, not direct execution of the uploaded Windows/Python scripts. Some need AI, screen-capture consent, notification access, or more implementation.",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlugins = false }) { Text("Close") }
            }
        )
    }
    if (showPermissionHelp) {
        AlertDialog(
            onDismissRequest = { showPermissionHelp = false },
            title = { Text("Microphone access") },
            text = { Text("Allow microphone access to use voice input. Speech recognition also requires a compatible Android recognition service.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionHelp = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionHelp = false }) { Text("Cancel") }
            }
        )
    }
}

internal fun localDemoReply(question: String): String {
    val q = question.lowercase(Locale.getDefault())
    return when {
        q.contains("hello") || q.contains("hi jarvis") -> "Hello. I'm online and ready. Connect an AI provider to enable full conversational intelligence."
        q.contains("time") -> "I can help with time-related tasks once the assistant tools are connected. For now, please check your device clock."
        q.contains("who are you") -> "I'm JARVIS, your Android assistant starter. My voice interface works locally; advanced AI features are still being built."
        q.contains("remember") -> "Persistent memory is planned. This starter currently keeps conversation messages only while the app is open."
        else -> "I received your message. I'm currently in demo mode, so I don't have a live AI model connected yet. The next milestone is a secure AI provider connection."
    }
}
