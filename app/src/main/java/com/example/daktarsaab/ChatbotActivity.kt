package com.example.daktarsaab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState // Added for auto-scroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.ArrowBack // Import for the back arrow icon

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(groqApiService = groqApiService)
                }
            }
        }
    }
}

// --- Composable Components ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier, groqApiService: GroqApiService) {
    val coroutineScope = rememberCoroutineScope()
    val systemPrompt = """
        You are MedGuide, an AI-powered medical assistant. You can answer health-related questions, provide general medical advice, and help users understand symptoms. Always remind users that your advice does not replace a real doctor's consultation. Be empathetic, concise, and clear. If a question is outside your scope, suggest seeing a healthcare professional.
    """.trimIndent()

    val messages = remember { mutableStateListOf<GroqMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showAnimations by remember { mutableStateOf(false) } // State to trigger entry animations
    val listState = rememberLazyListState() // Added for auto-scroll

    // Lottie animation for the fixed top section
    val doctorBotComposition by rememberLottieComposition(LottieCompositionSpec.Asset("doctorbot.json"))
    val doctorBotProgress by animateLottieCompositionAsState(
        composition = doctorBotComposition,
        iterations = LottieConstants.IterateForever // Loop continuously
    )

    // Trigger animations when the screen is first composed
    LaunchedEffect(Unit) {
        showAnimations = true
        if (messages.isEmpty()) {
            // Initialize chat with system prompt and assistant's greeting
            messages.add(GroqMessage(role = "system", content = systemPrompt))
            messages.add(GroqMessage(role = "assistant", content = "Hello! I'm MedGuide, your AI medical assistant. How can I help you today?"))
        }
    }

    // Auto-scroll to the newest message when messages list changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0) // Animate scroll to the top (newest message due to reverseLayout)
        }
    }

    val clearCurrentChat = {
        messages.clear()
        messages.add(GroqMessage(role = "system", content = systemPrompt))
        messages.add(GroqMessage(role = "assistant", content = "Hello! I'm MedGuide, your AI medical assistant. How can I help you today?"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showAnimations,
                        enter = slideInHorizontally(animationSpec = tween(durationMillis = 500)) { initialOffset -> initialOffset } // Slide in from left
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, // Space between elements
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Arrow Icon - No functionality for now
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp)) // Spacer between arrow and profile

                            // Dikshanta profile, now bolded and black color
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f), // Takes remaining space
                                horizontalArrangement = Arrangement.End // Aligns profile to the end
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
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), // Made bold
                                    color = Color.Black // Changed text color to black
                                )
                                Spacer(modifier = Modifier.width(16.dp)) // Padding from edge
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface // Use surface for cleaner look
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fixed Lottie animation section (doctorbot.json) with slide-in from top
            AnimatedVisibility(
                visible = showAnimations,
                enter = slideInVertically(animationSpec = tween(durationMillis = 500, delayMillis = 200)) { fullHeight -> -fullHeight } // Slide in from top, slightly delayed
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp), // Adjusted vertical padding for visual centering
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center // Center vertically in its own column
                ) {
                    LottieAnimation(
                        composition = doctorBotComposition,
                        progress = { doctorBotProgress },
                        modifier = Modifier.size(200.dp) // Maintain a larger size for prominence
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Space between animation and text
                    Text(
                        text = "Welcome to DaktarSaab Medical Chatbot",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold // Make welcome text bold
                    )
                }
            }
            // HorizontalDivider removed as requested

            // Spacer after the fixed section (no divider)
            Spacer(modifier = Modifier.height(8.dp))

            // Chat messages display area - takes remaining space and scrolls
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // This makes it take all available vertical space
                    .padding(horizontal = 8.dp),
                reverseLayout = true, // New messages appear at the bottom
                verticalArrangement = Arrangement.Bottom, // Keeps content aligned to the bottom
                state = listState // Added for auto-scroll
            ) {
                // Filter out the system message from display
                items(messages.reversed().filter { it.role != "system" }) { message ->
                    MessageBubble(message = message)
                }
            }

            // Loading indicator at the bottom, just above input field
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp)) // Small space after loading indicator

            // Input Area
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

                // Clear Chat button moved next to the text field
                IconButton(
                    onClick = { clearCurrentChat() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer), // A different color for clear
                    enabled = !isLoading // Disable clear button while loading
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp)) // Space between clear and send

                // Send button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMessage = GroqMessage(role = "user", content = inputText.trim())
                            messages.add(userMessage)
                            inputText = ""
                            isLoading = true

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val apiKey = "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" // Replaced BuildConfig.GROQ_API_KEY
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
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    enabled = !isLoading // Disable button while loading to prevent multiple sends
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: GroqMessage) {
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

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(16.dp)
            )
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
                    .padding(6.dp), // Adjust padding within the circle
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
        })
    }
}

