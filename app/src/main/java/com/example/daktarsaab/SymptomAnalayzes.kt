package com.example.daktarsaab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daktarsaab.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SymptomAnalayzes : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimplifiedSymptomAnalyzer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifiedSymptomAnalyzer() {
    var step by remember { mutableStateOf(1) }
    var bodyPart by remember { mutableStateOf("") }
    var mcqAnswers by remember { mutableStateOf(listOf("", "", "")) }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(true) }
    val commonBodyParts = listOf("Head", "Stomach", "Chest", "Back", "Leg", "Arm", "Throat", "Eye", "Ear", "Tooth")

    val mcqQuestions = listOf(
        "How severe is the pain?" to listOf("Mild", "Moderate", "Severe"),
        "How long have you had this pain?" to listOf("<1 day", "1-3 days", ">3 days"),
        "Is the pain constant or does it come and go?" to listOf("Constant", "Comes and goes", "Not sure")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Symptom Analyzer (Simple)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                1 -> {
                    Text(
                        "Where is it paining?",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = bodyPart,
                        onValueChange = {
                            bodyPart = it
                            showSuggestions = it.isEmpty()
                        },
                        label = { Text("Type or choose a body part") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    AnimatedVisibility(
                        visible = showSuggestions && bodyPart.isEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        // Use Row instead of FlowRow for compatibility if FlowRow is experimental or unavailable
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        ) {
                            commonBodyParts.forEach { part ->
                                AssistChip(
                                    onClick = {
                                        bodyPart = part
                                        showSuggestions = false
                                    },
                                    label = { Text(part) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { if (bodyPart.isNotBlank()) step = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = bodyPart.isNotBlank()
                    ) {
                        Text("Next", fontSize = 16.sp)
                    }
                }
                2 -> {
                    Text(
                        "How would you like to describe your symptoms?",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { step = 3 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Type Manually") }
                        Button(
                            onClick = { step = 4 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Answer Questions") }
                    }
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }
                3 -> { // Manual typing
                    var manualText by remember { mutableStateOf("") }
                    Text(
                        "Describe your symptoms in your own words:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        label = { Text("Type your symptoms here...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { if (manualText.isNotBlank()) step = 5 },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = manualText.isNotBlank()
                    ) { Text("Analyze", fontSize = 16.sp) }
                    OutlinedButton(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }
                4 -> { // MCQ
                    Text(
                        "Answer the following questions:",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    mcqQuestions.forEachIndexed { idx, (question, options) ->
                        Text(
                            question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        options.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = mcqAnswers[idx] == option,
                                    onClick = {
                                        mcqAnswers = mcqAnswers.toMutableList().also { it[idx] = option }
                                    }
                                )
                                Text(option)
                            }
                        }
                    }
                    Button(
                        onClick = { if (mcqAnswers.all { it.isNotBlank() }) step = 6 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = mcqAnswers.all { it.isNotBlank() }
                    ) {
                        Text("Next", fontSize = 16.sp)
                    }
                    OutlinedButton(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }
                5 -> { // Analyze for manual
                    Button(
                        onClick = {
                            isLoading = true
                            responseText = ""
                            val manualText = ""
                            val combinedInput = "Pain Location: $bodyPart\nManual Description: $manualText"
                            CoroutineScope(Dispatchers.IO).launch {
                                val result = getDiagnosisFromAPI(combinedInput)
                                withContext(Dispatchers.Main) {
                                    responseText = result
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("Analyze", fontSize = 16.sp)
                    }
                    OutlinedButton(
                        onClick = { step = 3 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Analyzing...",
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else if (responseText.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Analysis Result:",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                DiagnosisResult(responseText)
                            }
                        }
                    }
                }
                6 -> { // Analyze for MCQ
                    Button(
                        onClick = {
                            isLoading = true
                            responseText = ""
                            val combinedInput = "Pain Location: $bodyPart\n" +
                                mcqQuestions.mapIndexed { idx, (q, _) -> "${q} ${mcqAnswers[idx]}" }.joinToString("\n")
                            CoroutineScope(Dispatchers.IO).launch {
                                val result = getDiagnosisFromAPI(combinedInput)
                                withContext(Dispatchers.Main) {
                                    responseText = result
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("Analyze", fontSize = 16.sp)
                    }
                    OutlinedButton(
                        onClick = { step = 4 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Analyzing...",
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else if (responseText.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Analysis Result:",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                DiagnosisResult(responseText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosisResult(response: String) {
    val sections = remember(response) {
        // Splitting by numbered sections to be more robust
        // This assumes the model correctly outputs "1. Diagnosis:", "2. Possible Diseases:", etc.
        response.split("\n(?=\\d+\\.\\s)".toRegex()).filter { it.isNotBlank() }
            .map { it.trim() } // Trim whitespace from each section
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Slightly increased spacing
    ) {
        if (sections.isEmpty() || response.contains("Error", ignoreCase = true)) {
            Text(
                "Could not generate a proper diagnosis. Please try again with more details, or check your internet connection and API key.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            // Display the raw response for debugging if it's an error
            if (response.contains("Error", ignoreCase = true)) {
                Text(
                    "Raw Response/Error: $response",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            sections.forEach { section ->
                val lines = section.lines()
                if (lines.isNotEmpty()) {
                    // Extract title (e.g., "1. Diagnosis", "2. Possible Diseases")
                    val titleLine = lines.first()
                    val content = lines.drop(1).joinToString("\n").trim()

                    Text(
                        titleLine.replaceFirst("\\d+\\.\\s".toRegex(), ""), // Remove "1. ", "2. " etc. from title
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary // Use a different color for titles
                    )

                    // Format bullet points
                    val bulletPoints = content.split("• ").filter { it.isNotBlank() }
                    if (bulletPoints.size > 1) { // If there are actual bullet points
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            bulletPoints.forEach { point ->
                                if (point.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(
                                            point.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else { // If not bullet points (e.g., for single-line Diagnosis)
                        Text(
                            content,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

fun getDiagnosisFromAPI(symptomDetails: String): String {
    val apiKey = "AIzaSyA-qrsMc14uUweevDBS0enVYcfh1eRQHfg" // Ensure this API key is correct and active!
    val urlString =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

    // **** IMPROVED PROMPT HERE ****
    val jsonInput = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": "As a highly experienced medical AI, based on the following symptoms, provide a concise and structured diagnosis report. Adhere strictly to the requested format below, providing content for each section. If a section is not applicable, state 'None'.\n\nSymptoms: $symptomDetails\n\nREPORT:\n1. Diagnosis: [One concise sentence or phrase for primary diagnosis]\n2. Possible Diseases: \n• [Disease 1]\n• [Disease 2]\n• [Disease 3] (list 3-5, or fewer if less are relevant)\n3. Treatment: \n• [Treatment advice 1]\n• [Treatment advice 2]\n• [Treatment advice 3] (list 3-5, or fewer if less are relevant)\n4. Medicine: \n• [Medicine 1 (Class/Type if specific name not appropriate, e.g., 'Painkillers')]\n• [Medicine 2]\n• [Medicine 3] (list 3-5, or fewer if less are relevant)\n\nMaintain a professional, helpful, and non-prescriptive tone. Emphasize that this is AI-generated and not a substitute for professional medical advice."
                }
              ]
            }
          ]
        }
    """.trimIndent()

    return try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        connection.outputStream.use { os ->
            os.write(jsonInput.toByteArray())
            os.flush()
        }

        val responseCode = connection.responseCode
        val response = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val errorResponse = connection.errorStream.bufferedReader().readText()
            println("API Error Response Code: $responseCode")
            println("API Error Response Body: $errorResponse")
            "API Error: $responseCode - $errorResponse" // Return the actual API error
        }

        // --- Parsing logic (remains mostly the same, but now with a better prompt) ---
        val textKey = "\"text\": \""
        val startIndex = response.indexOf(textKey)
        if (startIndex == -1) {
            println("Full API Response (no 'text' field): $response")
            return "Diagnosis Report:\n\n1. Diagnosis: API response format error or no 'text' field found.\n2. Details:\n• Check the API's actual response structure.\n• Response: ${response.take(200)}..."
        }

        val textValueStart = startIndex + textKey.length
        val textValueEnd = response.indexOf("\"", textValueStart)

        if (textValueEnd == -1) {
            println("Full API Response ('text' field not closed): $response")
            return "Diagnosis Report:\n\n1. Diagnosis: API response format error - 'text' field not properly closed.\n2. Details:\n• Response: ${response.take(200)}..."
        }

        response
            .substring(textValueStart, textValueEnd)
            .replace("\\n", "\n") // Replace escaped newlines
            .replace("\\\"", "\"") // Replace escaped quotes
            .replace("\\*", "") // Remove any remaining markdown asterisks
            .replace("### ", "") // Remove potential markdown headers
            .trim() // Trim leading/trailing whitespace

    } catch (e: Exception) {
        e.printStackTrace() // Log the actual exception for debugging
        "Diagnosis Report:\n\n1. Diagnosis: Connection or Runtime Error\n2. Details:\n• ${e.localizedMessage ?: "Unknown network/runtime error"}\n3. Treatment:\n• Check internet connection.\n• Verify API key and URL.\n• If error persists, restart the app or device."
    }
}

