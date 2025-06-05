package com.example.daktarsaab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp // For baseline_speaker_phone_24
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.HashMap
import java.util.Locale

// --- Data Classes ---
data class GroqChatCompletionRequest(
    val model: String,
    val messages: List<GroqMessage>
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val system_fingerprint: String?,
    val `object`: String,
    val usage: Usage
)

data class Choice(
    val finish_reason: String,
    val index: Int,
    val message: GroqMessage,
    val logprobs: Any?
)

data class Usage(
    val completion_tokens: Int,
    val prompt_tokens: Int,
    val total_tokens: Int
)

// --- Retrofit Service ---
interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatCompletionRequest
    ): Response<GroqChatCompletionResponse>
}

// --- Chatbot Activity ---
class ChatbotActivity : ComponentActivity() {
    private val groqApiService: GroqApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }

    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US) // Set your desired language
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    println("TTS: Language not supported or missing data")
                } else {
                    println("TTS: Initialized successfully")
                }
            } else {
                println("TTS: Initialization failed")
            }
        }

        setContent {
            DaktarSaabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(groqApiService = groqApiService, textToSpeech = textToSpeech)
                }
            }
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }
}

// --- Composable Components ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier, groqApiService: GroqApiService, textToSpeech: TextToSpeech) {
    val coroutineScope = rememberCoroutineScope()
    val systemPrompt = """
        You are MedGuide, an AI-powered medical assistant. You can answer health-related questions, provide general medical advice, and help users understand symptoms. Always remind users that your advice does not replace a real doctor's consultation. Be empathetic, concise, and clear. If a question is outside your scope, suggest seeing a healthcare professional.
    """.trimIndent()

    val messages = remember { mutableStateListOf<GroqMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showInitialWelcome by remember { mutableStateOf(true) } // Control welcome animation state
    val listState = rememberLazyListState()
    var isSpeaking by remember { mutableStateOf(false) } // State for TTS speaking status

    val doctorBotComposition by rememberLottieComposition(LottieCompositionSpec.Asset("doctorbot.json"))
    val doctorBotProgress by animateLottieCompositionAsState(
        composition = doctorBotComposition,
        iterations = LottieConstants.IterateForever
    )

    // Animation states for shrinking welcome section
    val targetScale = if (showInitialWelcome) 1f else 0.3f
    val targetOffsetXDp = if (showInitialWelcome) 0.dp else (-100).dp // Adjust these values
    val targetOffsetYDp = if (showInitialWelcome) 0.dp else (-50).dp // based on your desired final position

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "welcomeScale"
    )
    val offsetX by animateFloatAsState(
        targetValue = with(LocalDensity.current) { targetOffsetXDp.toPx() },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "welcomeOffsetX"
    )
    val offsetY by animateFloatAsState(
        targetValue = with(LocalDensity.current) { targetOffsetYDp.toPx() },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "welcomeOffsetY"
    )


    // Function to send a message (either typed or spoken)
    val sendMessage: (String) -> Unit = { messageContent ->
        if (messageContent.isNotBlank() && !isLoading) {
            val userMessage = GroqMessage(role = "user", content = messageContent.trim())
            messages.add(userMessage)
            inputText = "" // Clear input text only if it was used for sending
            isLoading = true

            // Trigger welcome animation to shrink on first message
            if (showInitialWelcome) {
                showInitialWelcome = false
            }

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val apiKey = "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"
                    // Use current list including system prompt for API call
                    val currentMessagesForApi = messages.toList()
                    val request = GroqChatCompletionRequest(
                        model = "llama3-8b-8192",
                        messages = currentMessagesForApi
                    )

                    val response = groqApiService.getChatCompletion(
                        "Bearer $apiKey",
                        request
                    )

                    if (response.isSuccessful) {
                        val assistantMessage = response.body()?.choices?.firstOrNull()?.message
                        assistantMessage?.let {
                            // Add assistant's response, stripping common markdown for cleaner display
                            messages.add(it.copy(content = it.content.replace(Regex("[*_`~]"), "")))
                        }
                    } else {
                        messages.add(GroqMessage(role = "assistant", content = "Sorry, I couldn't process your request. Please try again. Error: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    messages.add(GroqMessage(role = "assistant", content = "An error occurred: ${e.localizedMessage}"))
                } finally {
                    isLoading = false
                }
            }
        }
    }


    // Speech-to-Text Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText: ArrayList<String>? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.let {
                // Directly send the spoken text as a user message
                sendMessage(it[0])
            }
        }
    }

    // Trigger initial messages
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(GroqMessage(role = "system", content = systemPrompt))
            messages.add(GroqMessage(role = "assistant", content = "Hello! I'm MedGuide, your AI medical assistant. How can I help you today?"))
        }

        // Set up UtteranceProgressListener for TTS
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    // Auto-scroll to the newest message (at the bottom)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // Scroll to the last item when new messages are added
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val clearCurrentChat = {
        messages.clear()
        messages.add(GroqMessage(role = "system", content = systemPrompt))
        messages.add(GroqMessage(role = "assistant", content = "Hello! I'm MedGuide, your AI medical assistant. How can I help you today?"))
        showInitialWelcome = true // Reset welcome animation state
    }

    // Function to start Text-to-Speech
    val speakText: (String) -> Unit = { text ->
        if (!textToSpeech.isSpeaking) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "message_utterance")
        }
    }

    // Function to stop Text-to-Speech
    val stopSpeaking: () -> Unit = {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false // Manually set to false if stopping
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Using painterResource for baseline_arrow_left_24
                        val ArrowBack = painterResource(R.drawable.baseline_arrow_left_24)
                        Icon(
                            painter = ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        // This is the part that will shrink and move
                        Crossfade(targetState = showInitialWelcome, animationSpec = tween(500), label = "welcomeHeaderCrossfade") { initial ->
                            if (initial) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                            .padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Dikshanta",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                            } else {
                                // Once hidden, you might want a smaller icon or just a title here
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(24.dp) // Smaller size
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MedGuide", // Changed to MedGuide when shrunk
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Initial Welcome Section (conditionally animated)
                AnimatedVisibility(
                    visible = showInitialWelcome,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 300, delayMillis = 100), shrinkTowards = Alignment.Top)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .scale(scale)
                            .offset(x = with(LocalDensity.current) { offsetX.toDp() }, y = with(LocalDensity.current) { offsetY.toDp() }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LottieAnimation(
                            composition = doctorBotComposition,
                            progress = { doctorBotProgress },
                            modifier = Modifier.size(if (showInitialWelcome) 200.dp else 50.dp) // Shrink Lottie
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Welcome to DaktarSaab Medical Chatbot",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Placeholder for the chat when the welcome shrinks
                if (!showInitialWelcome) {
                    Spacer(modifier = Modifier.height(56.dp)) // Adjust based on how much space the shrunk element takes
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    // Removed reverseLayout = true
                    verticalArrangement = Arrangement.Top, // Messages stack from top
                    state = listState
                ) {
                    items(messages.filter { it.role != "system" }) { message -> // Display messages in their original order
                        MessageBubble(message = message, onSpeakClick = { content ->
                            speakText(content)
                        }, onStopClick = {
                            stopSpeaking()
                        }, isSpeaking = isSpeaking) // Pass isSpeaking state
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Ask me anything") },
                        placeholder = { Text("Type your question...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Microphone button for Speech-to-Text
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                            }
                            speechRecognizerLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { clearCurrentChat() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    IconButton(
                        onClick = {
                            sendMessage(inputText) // Use the new sendMessage function
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        enabled = inputText.isNotBlank() && !isLoading // Only enable if there's typed text
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Small header for the shrunk welcome (aligned to top-left)
            // This is a simplified version and needs careful positioning
            // to match the exact targetOffset of the animation.
            if (!showInitialWelcome) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart) // Align to top-start
                        .padding(start = 16.dp, top = paddingValues.calculateTopPadding() + 16.dp) // Adjust padding as needed
                        .scale(0.8f) // Slightly smaller than the main shrunk element
                        .offset(x = (-30).dp, y = (-20).dp), // Fine-tune offset to fit beside the top app bar
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LottieAnimation(
                        composition = doctorBotComposition,
                        progress = { doctorBotProgress },
                        modifier = Modifier.size(40.dp) // Very small Lottie
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MedGuide",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: GroqMessage, onSpeakClick: (String) -> Unit, onStopClick: () -> Unit, isSpeaking: Boolean) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    // Lottie Animation for AI (robot.json)
    val robotComposition by rememberLottieComposition(LottieCompositionSpec.Asset("robot.json"))
    val robotProgress by animateLottieCompositionAsState(
        composition = robotComposition,
        iterations = LottieConstants.IterateForever
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show robot Lottie for assistant messages
        if (!isUser) {
            LottieAnimation(
                composition = robotComposition,
                progress = { robotProgress },
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    modifier = Modifier.padding(16.dp)
                )
            }
            // Add speaker icon for assistant messages
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End, // Align to end of bubble
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Stop button based on speaking state
                    IconButton(onClick = {
                        if (isSpeaking) {
                            onStopClick()
                        } else {
                            onSpeakClick(message.content)
                        }
                    }) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Speaking" else "Speak Message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Show person icon for user messages
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = "User",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .padding(6.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatPreview() {
    DaktarSaabTheme {
        ChatScreen(groqApiService = object : GroqApiService {
            override suspend fun getChatCompletion(p0: String, p1: GroqChatCompletionRequest) =
                Response.success(GroqChatCompletionResponse(
                    id = "test",
                    choices = listOf(Choice(
                        finish_reason = "stop",
                        index = 0,
                        message = GroqMessage("assistant", "Sample response from MedGuide! Always consult a doctor for medical advice."),
                        logprobs = null
                    )),
                    created = 0,
                    model = "test",
                    system_fingerprint = null,
                    `object` = "test",
                    usage = Usage(0, 0, 0)
                ))
        },
            textToSpeech = object : TextToSpeech(
                android.app.Application(), null
            ) {
                override fun shutdown() {}
                override fun stop(): Int {
                    return TextToSpeech.SUCCESS
                }
                override fun speak(
                    text: CharSequence?,
                    queueMode: Int,
                    params: Bundle?,
                    utteranceId: String?
                ): Int {
                    return TextToSpeech.SUCCESS
                }
                override fun setLanguage(locale: Locale?): Int {
                    return TextToSpeech.LANG_AVAILABLE
                }
                override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener?): Int {
                    return TextToSpeech.SUCCESS
                }
            })
    }
}