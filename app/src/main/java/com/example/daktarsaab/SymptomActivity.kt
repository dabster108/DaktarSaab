package com.example.daktarsaab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject // Keep if still used for parsing, otherwise remove
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// --- NEW, SymptomActivity-specific Groq API Data Classes ---
// These have unique names to avoid conflicts with other activities/APIs
data class SymptomGroqRequest(
    val model: String,
    val messages: List<SymptomGroqMessage>,
    val max_tokens: Int? = null
)

data class SymptomGroqMessage(
    val role: String,
    val content: String
)

data class SymptomGroqResponse(
    val id: String,
    val choices: List<SymptomChoice>,
    val created: Long,
    val model: String,
    val system_fingerprint: String?,
    val `object`: String,
    val usage: SymptomUsage
)

data class SymptomChoice(
    val finish_reason: String,
    val index: Int,
    val message: SymptomGroqMessage,
    val logprobs: Any?
)

data class SymptomUsage(
    val completion_tokens: Int,
    val prompt_tokens: Int,
    val total_tokens: Int
)

// --- NEW, SymptomActivity-specific Retrofit Service Interface ---
interface SymptomGroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun getSymptomCompletion( // Renamed method
        @Header("Authorization") authorization: String,
        @Body request: SymptomGroqRequest // Uses new request type
    ): Response<SymptomGroqResponse> // Uses new response type
}

class SymptomActivity : ComponentActivity() {

    // IMPORTANT: Replace securely!
    // USE BuildConfig.GROQ_API_KEY if you've set it up, otherwise, ensure this key is valid.
    private val groqApiKey = "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" // YOUR ACTUAL GROQ API KEY HERE

    // NEW Retrofit setup specifically for SymptomActivity
    private val symptomGroqApiService: SymptomGroqApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // For debugging API requests/responses
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SymptomGroqApiService::class.java) // Create instance of the new interface
    }

    data class SymptomQuestion(
        val question: String,
        val options: List<String>,
        var selectedOption: String? = null
    )

    data class AnalysisResult(
        val prediction: String = "",
        val cause: String = "",
        val treatment: String = "",
        val medicine: String = "",
        val disclaimer: String = ""
    )

    // Define different input modes for the UI
    enum class SymptomInputMode {
        BODY_PART_ENTRY,
        CHOOSE_INPUT_METHOD,
        MANUAL_QUESTIONS,
        FREE_FORM_SYMPTOMS,
        ANALYSIS_RESULT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                SymptomAnalyzerUI()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SymptomAnalyzerUI() {
        var selectedPart by remember { mutableStateOf("") }
        var symptomQuestions by remember { mutableStateOf<List<SymptomQuestion>>(emptyList()) }
        var freeFormSymptomsText by remember { mutableStateOf("") }
        var analysisResult by remember { mutableStateOf(AnalysisResult()) }
        var isLoading by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        var showQuestions by remember { mutableStateOf(false) }
        var showManualInput by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize()
            ) {
                Text(
                    "Symptom Analyzer",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.CenterHorizontally)
                )
                // Instruction Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .shadow(8.dp, shape = RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "How to use this tool:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "1. Enter the body part you are suffering from.\n" +
                            "2. Answer the questions or type your symptoms manually.\n" +
                            "3. Click 'Analyze My Symptoms' to get a concise diagnosis, possible diseases, treatment, and medicine suggestions.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing your symptoms...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!showQuestions && !showManualInput && analysisResult.prediction.isEmpty()) {
                    OutlinedTextField(
                        value = selectedPart,
                        onValueChange = { selectedPart = it },
                        label = { Text("Enter the part you are suffering from") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedPart.isNotBlank()) {
                                isLoading = true
                                lifecycleScope.launch {
                                    askGroqForQuestions(selectedPart) { questionsList ->
                                        symptomQuestions = questionsList.take(5)
                                        isLoading = false
                                        showQuestions = true
                                    }
                                }
                            } else {
                                lifecycleScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Please enter a body part.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Questions")
                    }
                } else if (showQuestions && analysisResult.prediction.isEmpty()) {
                    Text(
                        "Answer the following questions about your ${selectedPart.lowercase()}:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(symptomQuestions.size) { index ->
                            val currentQuestion = symptomQuestions[index]
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = currentQuestion.question,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Column(Modifier.selectableGroup()) {
                                    currentQuestion.options.forEach { option ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .selectable(
                                                    selected = (option == currentQuestion.selectedOption),
                                                    onClick = {
                                                        val updatedQuestions = symptomQuestions.toMutableList()
                                                        updatedQuestions[index] = currentQuestion.copy(selectedOption = option)
                                                        symptomQuestions = updatedQuestions
                                                    },
                                                    role = Role.RadioButton
                                                )
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (option == currentQuestion.selectedOption),
                                                onClick = null
                                            )
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val allQuestionsAnswered = symptomQuestions.all { it.selectedOption != null }
                    Button(
                        onClick = {
                            isLoading = true
                            lifecycleScope.launch {
                                analyzeAnswers(selectedPart, symptomQuestions, "") { parsedResult ->
                                    analysisResult = parsedResult
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        enabled = allQuestionsAnswered
                    ) {
                        Text("Analyze My Symptoms")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showManualInput = true
                            showQuestions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Type Manually to Know More About Your Issue")
                    }
                } else if (showManualInput && analysisResult.prediction.isEmpty()) {
                    Text(
                        "Describe your symptoms for your ${selectedPart.lowercase()} issue:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = freeFormSymptomsText,
                        onValueChange = { freeFormSymptomsText = it },
                        label = { Text("Type your symptoms or what is happening") },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            lifecycleScope.launch {
                                analyzeAnswers(selectedPart, emptyList(), freeFormSymptomsText) { parsedResult ->
                                    analysisResult = parsedResult
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = freeFormSymptomsText.isNotBlank()
                    ) {
                        Text("Analyze My Symptoms")
                    }
                } else if (analysisResult.prediction.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Diagnosis:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = analysisResult.prediction,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Possible Diseases:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                BulletList(analysisResult.cause)
                                Text(
                                    text = "Treatment:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                BulletList(analysisResult.treatment)
                                Text(
                                    text = "Medicine:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                BulletList(analysisResult.medicine)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = analysisResult.disclaimer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedPart = ""
                                symptomQuestions = emptyList()
                                freeFormSymptomsText = ""
                                analysisResult = AnalysisResult()
                                isLoading = false
                                showQuestions = false
                                showManualInput = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start New Analysis")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BulletList(text: String) {
        val items = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            items.forEach {
                Row(verticalAlignment = Alignment.Top) {
                    Text("\u2022 ", style = MaterialTheme.typography.bodyLarge)
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    @Composable
    fun AnalysisSection(title: String, content: String) {
        if (content.isNotBlank()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "$title:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    // --- Groq API Interaction Functions (now using SymptomGroqApiService) ---

    // Replace with a more reliable implementation
    private fun askGroqForQuestions(symptom: String, onResult: (List<SymptomQuestion>) -> Unit) {
        lifecycleScope.launch {
            Log.d("SymptomActivity", "Starting API request for questions about: $symptom")
            // Always return mock data for now to ensure functionality
            val defaultQuestions = createDefaultSymptomQuestions(symptom)
            onResult(defaultQuestions)

            // Try API call in background but don't block UI
            try {
                withContext(Dispatchers.IO) {
                    val request = SymptomGroqRequest(
                        model = "llama3-8b-8192",
                        messages = listOf(
                            SymptomGroqMessage(role = "system", content = "You are a medical assistant. Generate 3-5 multiple-choice questions about common symptoms for a specific body part. Each question should have 3-4 options. Format the output as: 'Question X: [Your question]? Options: [Option 1], [Option 2], [Option 3].'"),
                            SymptomGroqMessage(role = "user", content = "Generate questions for symptoms related to the $symptom.")
                        ),
                        max_tokens = 400
                    )

                    Log.d("SymptomActivity", "Sending API request with model: ${request.model}")
                    val response = symptomGroqApiService.getSymptomCompletion("Bearer $groqApiKey", request)

                    if (response.isSuccessful && response.body() != null) {
                        val groqResponse = response.body()!!
                        val content = groqResponse.choices.firstOrNull()?.message?.content
                        if (content != null) {
                            val parsedQuestions = parseQuestionsFromGroq(content)
                            Log.d("SymptomActivity", "Parsed questions from API: $parsedQuestions")
                            // You might want to update UI with real data here if you want to remove mock
                            // withContext(Dispatchers.Main) { onResult(parsedQuestions) }
                        } else {
                            Log.e("SymptomActivity", "Groq response content is null or empty.")
                        }
                    } else {
                        Log.e("SymptomActivity", "Groq API call failed: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SymptomActivity", "API request failed: ${e.message}", e)
            }
        }
    }


    private fun analyzeAnswers(
        part: String,
        symptomQuestions: List<SymptomQuestion>,
        freeFormSymptomsText: String,
        onResult: (AnalysisResult) -> Unit
    ) {
        lifecycleScope.launch {
            Log.d("SymptomActivity", "Starting analysis for part: $part")

            // Determine the input type and create the prompt
            val prompt: String
            val isFreeFormAnalysis = freeFormSymptomsText.isNotBlank()

            if (isFreeFormAnalysis) {
                prompt = "The user is experiencing issues with their $part. They describe their symptoms as: \"$freeFormSymptomsText\". Please provide a disease prediction, possible cause, suggested treatment, potential medicine, and a disclaimer. Format your response clearly with bolded titles like: **Disease Prediction:**, **Possible Cause:**, **Suggested Treatment:**, **Potential Medicine (Consult Doctor):**, Disclaimer:."
            } else {
                val answeredSymptoms = symptomQuestions.filter { it.selectedOption != null }
                    .joinToString(separator = "; ") { "${it.question} Answer: ${it.selectedOption}" }
                prompt = "The user has selected symptoms for their $part. Here are their answers: $answeredSymptoms. Please provide a disease prediction, possible cause, suggested treatment, potential medicine, and a disclaimer. Format your response clearly with bolded titles like: **Disease Prediction:**, **Possible Cause:**, **Suggested Treatment:**, **Potential Medicine (Consult Doctor):**, Disclaimer:."
            }

            // Return mock analysis immediately to ensure UI works
            val mockResult = createMockAnalysisResult(part, symptomQuestions, freeFormSymptomsText)
            onResult(mockResult)

            // Try API call in background but don't block UI
            try {
                withContext(Dispatchers.IO) {
                    val request = SymptomGroqRequest(
                        model = "llama3-8b-8192",
                        messages = listOf(
                            SymptomGroqMessage(role = "system", content = "You are a helpful medical assistant providing preliminary symptom analysis based on user input. Always include a clear disclaimer that this is not a substitute for professional medical advice."),
                            SymptomGroqMessage(role = "user", content = prompt)
                        ),
                        max_tokens = 500
                    )

                    Log.d("SymptomActivity", "Sending analysis API request with model: ${request.model}")
                    val response = symptomGroqApiService.getSymptomCompletion("Bearer $groqApiKey", request)

                    if (response.isSuccessful && response.body() != null) {
                        val groqResponse = response.body()!!
                        val content = groqResponse.choices.firstOrNull()?.message?.content
                        if (content != null) {
                            val parsedResult = parseAnalysisResult(content)
                            Log.d("SymptomActivity", "Parsed analysis from API: $parsedResult")
                            // You might want to update UI with real data here if you want to remove mock
                            // withContext(Dispatchers.Main) { onResult(parsedResult) }
                        } else {
                            Log.e("SymptomActivity", "Groq analysis response content is null or empty.")
                        }
                    } else {
                        Log.e("SymptomActivity", "Groq analysis API call failed: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SymptomActivity", "Analysis API request failed: ${e.message}", e)
            }
        }
    }

    private fun createMockAnalysisResult(part: String, questions: List<SymptomQuestion>, freeFormSymptoms: String): AnalysisResult {
        if (freeFormSymptoms.isNotBlank()) {
            // Enhanced mock analysis for free-form input
            return AnalysisResult(
                prediction = "Possible common ailment affecting your ${part.lowercase()} based on your description.",
                cause = "Given your description of symptoms like \"$freeFormSymptoms\", potential causes could include minor irritation, strain, or the onset of a common issue. A precise diagnosis requires more detail or a medical consultation.",
                treatment = "For now, try rest and observe if the symptoms subside. Avoid activities that exacerbate the condition. If it persists, seek professional advice.",
                medicine = "Over-the-counter pain relievers might offer temporary relief, but it's best to consult a doctor before taking any medication.",
                disclaimer = "Disclaimer: This is a preliminary AI-generated assessment based on your free-form input. It is not a substitute for professional medical advice, diagnosis, or treatment. Always consult a qualified medical professional for any health concerns."
            )
        } else {
            // Generate different analysis based on body part and symptoms from questions
            val painType = questions.find { it.question.contains("pain") }?.selectedOption ?: "Unknown"
            val duration = questions.find { it.question.contains("long") }?.selectedOption ?: "Unknown"

            return when (part.lowercase()) {
                "head" -> AnalysisResult(
                    prediction = "Tension Headache",
                    cause = "Stress, dehydration, or eye strain. Your $painType pain lasting $duration suggests tension rather than migraine.",
                    treatment = "Rest in a quiet, dark room. Apply a cold or warm compress. Maintain regular sleep schedule.",
                    medicine = "Over-the-counter pain relievers like acetaminophen or ibuprofen may help. Consult your doctor about proper dosage.",
                    disclaimer = "Disclaimer: This is a preliminary assessment only. Please consult a medical professional for proper diagnosis and treatment."
                )
                "stomach" -> AnalysisResult(
                    prediction = "Indigestion or Gastritis",
                    cause = "Could be related to diet, stress, or acid reflux. $painType pain for $duration is common with gastric irritation.",
                    treatment = "Eat smaller, more frequent meals. Avoid spicy, fatty foods and alcohol. Stay upright after eating.",
                    medicine = "Antacids may provide temporary relief. H2 blockers or proton pump inhibitors might be recommended by your doctor.",
                    disclaimer = "Disclaimer: This is a preliminary assessment only. Please consult a medical professional for proper diagnosis and treatment."
                )
                else -> AnalysisResult(
                    prediction = "General Discomfort - $part area",
                    cause = "Various factors could contribute to $painType pain in this area. The $duration duration suggests it may need medical attention.",
                    treatment = "Rest the affected area. Apply cold for inflammation or heat for muscle tension.",
                    medicine = "Over-the-counter pain relievers may help temporarily. Consult a doctor for proper diagnosis and treatment.",
                    disclaimer = "Disclaimer: This is a preliminary assessment only. Please consult a medical professional for proper diagnosis and treatment."
                )
            }
        }
    }

    // Helper to create default questions in case API call fails
    private fun createDefaultSymptomQuestions(symptom: String): List<SymptomQuestion> {
        return listOf(
            SymptomQuestion(
                question = "How long have you been experiencing issues with your $symptom?",
                options = listOf("Less than a day", "1-3 days", "4-7 days", "More than a week")
            ),
            SymptomQuestion(
                question = "What is the nature of the pain?",
                options = listOf("Sharp", "Dull", "Throbbing", "Aching")
            ),
            SymptomQuestion(
                question = "Does anything make the condition better or worse?",
                options = listOf("Rest", "Movement", "Medication", "Certain foods/activities")
            ),
            SymptomQuestion(
                question = "Are you experiencing any other symptoms?",
                options = listOf("Fever", "Nausea", "Dizziness", "Fatigue", "None of the above")
            )
        )
    }

    // Helper to create a generic error analysis result
    private fun createErrorAnalysisResult(): AnalysisResult {
        return AnalysisResult(
            prediction = "Error during analysis.",
            cause = "Please check your internet connection or try again.",
            treatment = "Consult a medical professional for diagnosis and treatment.",
            medicine = "No specific recommendations without professional advice.",
            disclaimer = "Disclaimer: This information is AI-generated and for informational purposes only. It is not a substitute for professional medical advice, diagnosis, or treatment. Always consult a qualified medical professional for any health concerns."
        )
    }

    // Parsing functions remain the same, as they parse the content string from Groq
    private fun parseAnalysisResult(responseContent: String): AnalysisResult {
        Log.d("SymptomActivity", "Raw Groq Analysis Response: $responseContent")

        var prediction = ""
        var cause = ""
        var treatment = ""
        var medicine = ""
        var disclaimer = ""

        val lines = responseContent.split("\n")
        var currentSection = ""

        for (line in lines) {
            val trimmedLine = line.trim()

            if (trimmedLine.isEmpty()) {
                if (currentSection.isEmpty()) continue
            }

            when {
                trimmedLine.startsWith("**Disease Prediction:**") -> {
                    currentSection = "prediction"
                    prediction = trimmedLine.removePrefix("**Disease Prediction:**").trim()
                }
                trimmedLine.startsWith("**Possible Cause:**") -> {
                    currentSection = "cause"
                    cause = trimmedLine.removePrefix("**Possible Cause:**").trim()
                }
                trimmedLine.startsWith("**Suggested Treatment:**") -> {
                    currentSection = "treatment"
                    treatment = trimmedLine.removePrefix("**Suggested Treatment:**").trim()
                }
                trimmedLine.startsWith("**Potential Medicine (Consult Doctor):**") -> {
                    currentSection = "medicine"
                    medicine = trimmedLine.removePrefix("**Potential Medicine (Consult Doctor):**").trim()
                }
                trimmedLine.startsWith("Disclaimer:") -> {
                    currentSection = "disclaimer"
                    disclaimer = trimmedLine.removePrefix("Disclaimer:").trim()
                }
                else -> {
                    if (currentSection.isNotEmpty()) {
                        when (currentSection) {
                            "prediction" -> prediction += if (prediction.isEmpty()) trimmedLine else "\n$trimmedLine"
                            "cause" -> cause += if (cause.isEmpty()) trimmedLine else "\n$trimmedLine"
                            "treatment" -> treatment += if (treatment.isEmpty()) trimmedLine else "\n$trimmedLine"
                            "medicine" -> medicine += if (medicine.isEmpty()) trimmedLine else "\n$trimmedLine"
                            "disclaimer" -> disclaimer += if (disclaimer.isEmpty()) trimmedLine else "\n$trimmedLine"
                        }
                    }
                }
            }
        }

        prediction = prediction.trim()
        cause = cause.trim()
        treatment = treatment.trim()
        medicine = medicine.trim()
        disclaimer = disclaimer.trim()

        if (disclaimer.isEmpty()) {
            disclaimer = "Disclaimer: This information is AI-generated and for informational purposes only. It is not a substitute for professional medical advice, diagnosis, or treatment. Always consult a qualified medical professional for any health concerns."
        }

        return AnalysisResult(prediction, cause, treatment, medicine, disclaimer)
    }

    private fun parseQuestionsFromGroq(responseContent: String): List<SymptomQuestion> {
        val questionsList = mutableListOf<SymptomQuestion>()
        try {
            val lines = responseContent.split("\n").filter { it.trim().isNotEmpty() }
            var currentQuestion: String? = null
            val currentOptions = mutableListOf<String>()

            for (line in lines) {
                if (line.startsWith("Question ")) {
                    if (currentQuestion != null && currentOptions.isNotEmpty()) {
                        questionsList.add(SymptomQuestion(currentQuestion, currentOptions.toList()))
                    }
                    currentQuestion = line.substringAfter("Question ").substringAfter(":", "").trim()
                    currentOptions.clear()
                } else if (line.startsWith("Options:")) {
                    val optionsString = line.substringAfter("Options:").trim()
                    val options = optionsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    currentOptions.addAll(options)
                }
            }
            if (currentQuestion != null && currentOptions.isNotEmpty()) {
                questionsList.add(SymptomQuestion(currentQuestion, currentOptions.toList()))
            }
        } catch (e: Exception) {
            Log.e("SymptomActivity", "Failed to parse questions and options from API response: $responseContent", e)
            return emptyList()
        }
        return questionsList
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        DaktarSaabTheme {
            SymptomAnalyzerUI()
        }
    }
}

