package com.example.daktarsaab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.google.gson.annotations.SerializedName
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
import androidx.compose.ui.res.painterResource // Import for painterResource

// --- Groq API Data Classes ---

// Request Data Classes
data class GroqChatCompletionRequest(
    val model: String,
    val messages: List<GroqMessage>
)

data class GroqMessage(
    val role: String,
    val content: String
)

// Response Data Classes
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
    val logprobs: Any? // Can be null
)

data class Usage(
    val completion_tokens: Int,
    val prompt_tokens: Int,
    val total_tokens: Int
)

// Retrofit Service Interface
interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatCompletionRequest
    ): Response<GroqChatCompletionResponse>
}

// --- ChatbotActivity ---
class ChatbotActivity : ComponentActivity() {

    // Initialize Retrofit service here, using lazy initialization for efficiency
    private val groqApiService: GroqApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies for debugging
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/") // Base URL for the Groq API
            .client(client) // Custom OkHttpClient with logging
            .addConverterFactory(GsonConverterFactory.create()) // Converter for JSON serialization/deserialization
            .build()
            .create(GroqApiService::class.java) // Create the Retrofit service instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge() // Enables edge-to-edge display for a modern look
        setContent {
            DaktarSaabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        modifier = Modifier.padding(innerPadding),
                        groqApiService = groqApiService
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier, groqApiService: GroqApiService) {
    val coroutineScope = rememberCoroutineScope()

    // State for the current chat messages (in-memory only)
    // Chat history will be cleared when the app is closed or activity is recreated
    val messages = remember { mutableStateListOf<GroqMessage>() }
    // State to hold the current text in the input field
    var inputText by remember { mutableStateOf("") }
    // State to indicate if an API call is in progress
    var isLoading by remember { mutableStateOf(false) }

    // Add an initial greeting message from the AI when the screen is first composed
    LaunchedEffect(Unit) {
        messages.add(GroqMessage(role = "assistant", content = "Hello! How can I help you today?"))
    }

    // Function to clear current chat messages (in-memory only)
    val clearCurrentChat: () -> Unit = {
        messages.clear()
        messages.add(GroqMessage(role = "assistant", content = "Hello! How can I help you today?")) // Re-add initial message
        Log.d("ChatScreen", "Current chat cleared (in-memory).")
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar with Clear Chat button
        TopAppBar(
            title = { Text("DaktarSaab Chatbot") },
            actions = {
                IconButton(onClick = { clearCurrentChat() }) { // Clear chat button
                    Icon(Icons.Default.Clear, contentDescription = "Clear Chat")
                }
            }
        )

        // Chat messages display area (scrollable)
        LazyColumn(
            modifier = Modifier
                .weight(1f) // Takes up all available vertical space
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            reverseLayout = true, // New messages appear at the bottom
            verticalArrangement = Arrangement.Bottom // Align content to the bottom
        ) {
            items(messages.reversed()) { message -> // Display messages from current session
                MessageBubble(message = message)
            }
        }

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        // Input field and send button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Ask me anything") }, // Updated label
                placeholder = { Text("What can I help with?") }, // Updated placeholder
                modifier = Modifier.weight(1f), // Takes up most of the row space
                shape = RoundedCornerShape(24.dp), // Rounded corners for a modern look
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        val userMessageContent = inputText.trim()
                        val userMessage = GroqMessage(role = "user", content = userMessageContent)
                        messages.add(userMessage) // Add user message to in-memory list
                        inputText = "" // Clear input field immediately
                        isLoading = true // Show loading indicator

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val apiKey = BuildConfig.GROQ_API_KEY
                                val request = GroqChatCompletionRequest(
                                    model = "llama3-8b-8192", // Using a suitable Groq model
                                    messages = messages.toList() // Send all current messages as context
                                )

                                val response = groqApiService.getChatCompletion(
                                    authorization = "Bearer $apiKey",
                                    request = request
                                )

                                if (response.isSuccessful) {
                                    val chatResponse = response.body()
                                    val assistantMessageContent = chatResponse?.choices?.firstOrNull()?.message?.content
                                    // Remove markdown formatting (basic removal for *, **)
                                    val cleanedMessage = assistantMessageContent?.replace(Regex("[*_]"), "")?.trim()
                                    val assistantMessage = GroqMessage(role = "assistant", content = cleanedMessage ?: "No response from AI.")
                                    messages.add(assistantMessage) // Add AI response to in-memory list
                                    Log.d("GroqChatbot", "Groq API Response: $cleanedMessage")
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    val errorMessage = GroqMessage(role = "assistant", content = "Error: ${response.code()} - ${errorBody}")
                                    messages.add(errorMessage) // Add error message to in-memory list
                                    Log.e("GroqChatbot", "Groq API Error: ${response.code()} - $errorBody")
                                }
                            } catch (e: Exception) {
                                val errorMessage = GroqMessage(role = "assistant", content = "Network error: ${e.message}")
                                messages.add(errorMessage) // Add network error message to in-memory list
                                Log.e("GroqChatbot", "Error making Groq API request", e)
                            } finally {
                                isLoading = false // Hide loading indicator
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50) // Circular button
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
fun MessageBubble(message: GroqMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(), // Smooth animation for size changes
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom // Align content to bottom if bubbles are different heights
    ) {
        // Chatbot icon for assistant messages
        if (!isUser) {
            Icon(
                // Changed from Icons.Default.SmartToy to painterResource for custom drawable
                painter = painterResource(id = R.drawable.baseline_personal_injury_24),
                contentDescription = "Chatbot Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(4.dp)
                    .align(Alignment.Bottom) // Align icon to bottom of bubble
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier
                .widthIn(max = 300.dp) // Limit bubble width
                .align(Alignment.Bottom), // Align text to bottom of bubble
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(10.dp)
            )
        }

        // User icon for user messages
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Person, // Using Person for user
                contentDescription = "User Icon",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .padding(4.dp)
                    .align(Alignment.Bottom) // Align icon to bottom of bubble
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ChatbotPreview() {
    DaktarSaabTheme {
        // For preview, we can't make real network calls.
        // Provide a dummy implementation of ChatScreen for visual preview.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("DaktarSaab Chatbot") },
                    actions = {
                        IconButton(onClick = { /* Preview: Do nothing */ }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Chat")
                        }
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    items(listOf(
                        GroqMessage(role = "assistant", content = "Hello! How can I help you today?"),
                        GroqMessage(role = "user", content = "Tell me about AI."),
                        GroqMessage(role = "assistant", content = "Artificial intelligence (AI) is a rapidly evolving field of computer science that aims to create machines capable of intelligent behavior. It involves developing systems that can perform tasks that typically require human intelligence, such as learning, problem-solving, decision-making, perception, and language understanding."),
                        GroqMessage(role = "user", content = "That's interesting! What are some applications of AI?")
                    ).reversed()) { message ->
                        MessageBubble(message = message)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = "Preview message",
                        onValueChange = { /* Do nothing */ },
                        label = { Text("Ask me anything") },
                        placeholder = { Text("What can I help with?") },
                        modifier = Modifier.weight(1f),
                        enabled = false, // Disable input in preview
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = { /* Do nothing */ },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    }
}
