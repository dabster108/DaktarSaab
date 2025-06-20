package com.example.daktarsaab.view

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.daktarsaab.R
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
        window.statusBarColor = getColor(R.color.black)
        enableEdgeToEdge()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    println("TTS: Language not supported or missing data")
                } else {
                    println("TTS: Initialized successfully")
                }
            } else {
                println("TTS: Initialization failed")
            }
        }

        val userName = intent.getStringExtra("USER_NAME")
        val profileImageUrl = intent.getStringExtra("PROFILE_IMAGE_URL")

        // Read dark mode preference from SharedPreferences
        val prefs = getSharedPreferences("daktar_prefs", MODE_PRIVATE)

        setContent {
            // Read the theme preference directly here.
            // isSystemInDarkTheme() is a composable function, so it should be called within a composable scope.
            val systemTheme = isSystemInDarkTheme()
            val currentGlobalDarkMode = prefs.getBoolean("dark_mode", systemTheme)

            DaktarSaabTheme(darkTheme = currentGlobalDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(
                        groqApiService = groqApiService,
                        textToSpeech = textToSpeech,
                        darkMode = currentGlobalDarkMode, // Use theme from SharedPreferences
                        onToggleDarkMode = { /* Theme is controlled by Dashboard and not changeable here */ }, // Made toggle non-functional
                        userName = userName,
                        profileImageUrl = profileImageUrl
                    )
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

// --- Theme Definition ---
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF73F8E5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFA),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFAFDFA),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDBE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    surfaceTint = Color(0xFF006A60),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF52DBCA),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF73F8E5),
    secondary = Color(0xFFB0CCC6),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFAEC9E5),
    onTertiary = Color(0xFF143349),
    tertiaryContainer = Color(0xFF2C4A61),
    onTertiaryContainer = Color(0xFFCCE5FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE0E3E1),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF899390),
    surfaceTint = Color(0xFF52DBCA),
)

// --- Composable Components ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    groqApiService: GroqApiService,
    textToSpeech: TextToSpeech,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    userName: String?,
    profileImageUrl: String?
) {
    val coroutineScope = rememberCoroutineScope()
    val systemPrompt = """
        You are MedGuide, an AI-powered medical assistant. You can answer health-related questions, provide general medical advice, and help users understand symptoms. Always remind users that your advice does not replace a real doctor's consultation. Be empathetic, concise, and clear. If a question is outside your scope, suggest seeing a healthcare professional.
    """.trimIndent()

    val messages = remember { mutableStateListOf<GroqMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showInitialWelcome by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var isSpeaking by remember { mutableStateOf(false) }

    val doctorBotComposition by rememberLottieComposition(LottieCompositionSpec.Asset("doctorbot.json"))
    val doctorBotProgress by animateLottieCompositionAsState(
        composition = doctorBotComposition,
        iterations = LottieConstants.IterateForever
    )

    // Animation states
    val targetScale = if (showInitialWelcome) 1f else 0.3f
    val targetOffsetXDp = if (showInitialWelcome) 0.dp else (-100).dp
    val targetOffsetYDp = if (showInitialWelcome) 0.dp else (-50).dp

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

    // Function to send a message
    val sendMessage: (String) -> Unit = { messageContent ->
        if (messageContent.isNotBlank() && !isLoading) {
            val userMessage = GroqMessage(role = "user", content = messageContent.trim())
            messages.add(userMessage)
            inputText = ""
            isLoading = true

            if (showInitialWelcome) {
                showInitialWelcome = false
            }

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val apiKey = "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"
                    val currentMessagesForApi = mutableListOf<GroqMessage>()
                    currentMessagesForApi.add(GroqMessage(role = "system", content = systemPrompt))
                    currentMessagesForApi.addAll(messages.filter { it.role == "user" || it.role == "assistant" })

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
                            messages.add(it.copy(content = it.content.replace(Regex("[*_`~]"), "")))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        println("API Error: ${response.code()}, $errorBody")
                        messages.add(GroqMessage(role = "assistant", content = "Sorry, I couldn't process your request. Please try again. Error: ${response.code()}"))
                    }
                } catch (e: Exception) {
                    messages.add(GroqMessage(role = "assistant", content = "An error occurred. Please try again. ${e.localizedMessage}"))
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
                sendMessage(it[0])
            }
        }
    }

    // Initial setup
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(GroqMessage(role = "system", content = systemPrompt))
            messages.add(GroqMessage(role = "assistant", content = "Hello! I'm DaktarSaab Medguide, your AI medical assistant. How can I help you today?"))
        }

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

    // Auto-scroll to newest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val clearCurrentChat = {
        messages.clear()
        messages.add(GroqMessage(role = "system", content = systemPrompt))
        messages.add(GroqMessage(role = "assistant", content = "Hello! I'm MedGuide, your AI medical assistant. How can I help you today?"))
        showInitialWelcome = true
    }

    // TTS functions
    val speakText: (String) -> Unit = { text ->
        if (!textToSpeech.isSpeaking) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "message_utterance")
        }
    }

    val stopSpeaking: () -> Unit = {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
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
                        // Left side with back button


                        Spacer(modifier = Modifier.weight(1f))

                        // Right side with theme toggle and profile
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Theme toggle button
                            IconButton(
                                onClick = onToggleDarkMode,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (darkMode) R.drawable.baseline_light_mode_24
                                        else R.drawable.baseline_dark_mode_24
                                    ),
                                    contentDescription = if (darkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Profile icon and name
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!profileImageUrl.isNullOrBlank()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = profileImageUrl),
                                        contentDescription = "Profile",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                CircleShape
                                            )
                                            .padding(6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = userName ?: "User", // Display dynamic username or "User" if null
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                // Initial Welcome Section
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
                            modifier = Modifier.size(if (showInitialWelcome) 200.dp else 50.dp)
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

                if (!showInitialWelcome) {
                    Spacer(modifier = Modifier.height(56.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Top,
                    state = listState
                ) {
                    items(messages.filter { it.role != "system" }) { message ->
                        MessageBubble(message = message, onSpeakClick = { content ->
                            speakText(content)
                        }, onStopClick = {
                            stopSpeaking()
                        }, isSpeaking = isSpeaking)
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
                        label = { Text("Ask AI") },
                        placeholder = { Text("Type here") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary, // Keep MaterialTheme qualifier
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), // Keep MaterialTheme qualifier
                            cursorColor = MaterialTheme.colorScheme.primary, // Keep MaterialTheme qualifier
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, // Keep MaterialTheme qualifier
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // Keep MaterialTheme qualifier
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // Keep MaterialTheme qualifier
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // Keep MaterialTheme qualifier
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, // Keep MaterialTheme qualifier
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant // Keep MaterialTheme qualifier
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Microphone button
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
                            .background(MaterialTheme.colorScheme.tertiaryContainer), // Keep MaterialTheme qualifier
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer // Keep MaterialTheme qualifier
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Clear button
                    IconButton(
                        onClick = { clearCurrentChat() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer), // Keep MaterialTheme qualifier
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer // Keep MaterialTheme qualifier
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    IconButton(
                        onClick = {
                            sendMessage(inputText)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary), // Keep MaterialTheme qualifier
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, // Updated to AutoMirrored version
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary // Keep MaterialTheme qualifier
                        )
                    }
                }
            }

            // Small header for shrunk welcome
            if (!showInitialWelcome) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = paddingValues.calculateTopPadding() + 16.dp)
                        .scale(0.8f)
                        .offset(x = (-30).dp, y = (-20).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LottieAnimation(
                        composition = doctorBotComposition,
                        progress = { doctorBotProgress },
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DaktarSaab Chatbot",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface // Keep MaterialTheme qualifier
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: GroqMessage, onSpeakClick: (String) -> Unit, onStopClick: () -> Unit, isSpeaking: Boolean) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant // Keep MaterialTheme qualifier
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface // Keep MaterialTheme qualifier

    // Lottie Animation for AI
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

            if (!isUser) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (isSpeaking) {
                            onStopClick()
                        } else {
                            onSpeakClick(message.content)
                        }
                    }) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp, // Updated to AutoMirrored version
                            contentDescription = if (isSpeaking) "Stop Speaking" else "Speak Message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // Keep MaterialTheme qualifier
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = "User",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), // Keep MaterialTheme qualifier
                        CircleShape
                    )
                    .padding(6.dp),
                tint = MaterialTheme.colorScheme.primary // Keep MaterialTheme qualifier
            )
        }
    }
}

// Theme Definition
@Composable
fun DaktarSaabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) { // Renamed from colorScheme to avoid conflict
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme, // Use the local colorScheme variable
        typography = MaterialTheme.typography,
        content = content
    )
}


@Preview(showBackground = true)
@Composable
fun ChatPreview() {
    DaktarSaabTheme {
        ChatScreen(
            groqApiService = object : GroqApiService {
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
                    return SUCCESS
                }

                override fun speak(
                    text: CharSequence?,
                    queueMode: Int,
                    params: Bundle?,
                    utteranceId: String?
                ): Int {
                    return SUCCESS
                }

                override fun setLanguage(locale: Locale?): Int {
                    return LANG_AVAILABLE
                }

                override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener?): Int {
                    return SUCCESS
                }
            },
            darkMode = false,
            onToggleDarkMode = {},
            userName = "Dikshanta Preview",
            profileImageUrl = null // You can put a test image URL here for preview
        )
    }
}