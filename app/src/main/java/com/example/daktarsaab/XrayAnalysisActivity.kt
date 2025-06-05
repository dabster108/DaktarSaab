package com.example.daktarsaab

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class XrayAnalysisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            com.example.daktarsaab.ui.theme.DaktarSaabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    XrayAnalysisScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun XrayAnalysisScreen() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State for splash screen
    var showSplashScreen by remember { mutableStateOf(true) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.5f) }
    val density = LocalDensity.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            resultText = ""
            errorMessage = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {
                Log.e("XrayAnalysis", "Error loading image", e)
                errorMessage = "Error loading image"
            }
        }
    }

    LaunchedEffect(key1 = true) {
        // Animate splash screen in
        alpha.animateTo(1f, animationSpec = tween(1500))
        scale.animateTo(1f, animationSpec = tween(1000, delayMillis = 500))
        delay(2000) // Keep splash screen for 2 seconds
        // Animate splash screen out and show main content
        alpha.animateTo(0f, animationSpec = tween(500))
        scale.animateTo(0.5f, animationSpec = tween(500))
        delay(500) // Wait for fade out
        showSplashScreen = false
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !showSplashScreen,
                enter = fadeIn() + slideInVertically { with(density) { -40.dp.roundToPx() } },
                exit = fadeOut() + slideOutVertically { with(density) { -40.dp.roundToPx() } }
            ) {
                TopAppBar(
                    title = { Text("X-Ray Analysis", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            AnimatedVisibility(
                visible = !showSplashScreen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { with(density) { 40.dp.roundToPx() } }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { with(density) { 40.dp.roundToPx() } }),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image selection area
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(min = 200.dp, max = 400.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "Selected X-Ray",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clickable {
                                    imagePickerLauncher.launch("image/*")
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Add X-ray image",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Tap to select an X-Ray image for analysis",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action button (Analyze)
                    Button(
                        onClick = {
                            if (bitmap != null) {
                                isLoading = true
                                errorMessage = null
                                resultText = ""
                                scope.launch {
                                    try {
                                        resultText = analyzeImageWithGemini(context, imageUri!!)
                                    } catch (e: Exception) {
                                        errorMessage = "Analysis failed: ${e.message}"
                                        Log.e("XrayAnalysis", "Error", e)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Please select an image first"
                            }
                        },
                        enabled = bitmap != null && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Analyzing...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Analyze"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze X-Ray Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Disclaimer always visible
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "DISCLAIMER: This analysis is for informational purposes only and should not replace professional medical advice. Always consult with a qualified healthcare provider for diagnosis and treatment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Loading, Error, or Results display
                    when {
                        isLoading -> {
                            XrayLoadingAnimation()
                        }
                        errorMessage != null -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        resultText.isNotEmpty() -> {
                            XrayResultDisplay(resultText)
                        }
                    }
                }
            }

            // Splash Screen Overlay
            AnimatedVisibility(
                visible = showSplashScreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = "DaktarSaab Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(120.dp)
                            .alpha(alpha.value)
                            .scale(scale.value)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "DaktarSaab",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.alpha(alpha.value)
                    )
                    Text(
                        text = "X-Ray Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.alpha(alpha.value)
                    )
                }
            }
        }
    }
}

@Composable
fun XrayLoadingAnimation() {
    // Lottie animation composition
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("xrayanimate.json")
    )

    // Progress for the animation (looping forever)
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f,
        restartOnPlay = true
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lottie animation
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(250.dp)
                .padding(8.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analyzing X-Ray Image",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Processing your X-ray with advanced AI analysis...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Rest of the code remains the same (XrayResultDisplay, DiagnosisSection, parseResultText, analyzeImageWithGemini, etc.)
@Composable
fun XrayResultDisplay(resultText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Text(
                text = "X-Ray Analysis Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Parse the resultText to identify sections and display them
            val sections = parseResultText(resultText)

            sections.forEach { (title, content) ->
                when {
                    title.contains("PRIMARY FINDINGS", ignoreCase = true) -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.Default.MedicalServices,
                            sectionColor = Color(0xFFE0F2F1),
                            sectionTextColor = Color(0xFF004D40)
                        )
                    }
                    title.contains("DIFFERENTIAL DIAGNOSES", ignoreCase = true) -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.AutoMirrored.Filled.List,
                            sectionColor = Color(0xFFFFECB3),
                            sectionTextColor = Color(0xFF7A5800)
                        )
                    }
                    title.contains("DETAILED OBSERVATIONS", ignoreCase = true) -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.Default.Visibility,
                            sectionColor = Color(0xFFE3F2FD),
                            sectionTextColor = Color(0xFF0D47A1)
                        )
                    }
                    title.contains("CONFIDENCE LEVEL", ignoreCase = true) -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.Default.CheckCircle,
                            sectionColor = Color(0xFFE8F5E9),
                            sectionTextColor = Color(0xFF1B5E20)
                        )
                    }
                    title.contains("RECOMMENDATIONS", ignoreCase = true) -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.Default.Healing,
                            sectionColor = Color(0xFFE1F5FE),
                            sectionTextColor = Color(0xFF01579B)
                        )
                    }
                    title.contains("X-RAY TYPE", ignoreCase = true) -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.Default.Category,
                            sectionColor = Color(0xFFF3E5F5),
                            sectionTextColor = Color(0xFF4A148C)
                        )
                    }
                    else -> {
                        DiagnosisSection(
                            titleLine = title,
                            content = content,
                            sectionIcon = Icons.Default.Info,
                            sectionColor = Color(0xFFEDE7F6),
                            sectionTextColor = Color(0xFF4527A0)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DiagnosisSection(
    titleLine: String,
    content: String,
    sectionIcon: ImageVector,
    sectionColor: Color,
    sectionTextColor: Color
) {
    val title = titleLine.replaceFirst("\\d+\\.\\s".toRegex(), "")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = sectionColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = sectionIcon,
                    contentDescription = title,
                    tint = sectionTextColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = sectionTextColor
                )
            }

            val formattedContent = formatContentWithBullets(content)
            Text(
                text = formattedContent,
                style = MaterialTheme.typography.bodyMedium,
                color = sectionTextColor.copy(alpha = 0.9f)
            )
        }
    }
}

fun formatContentWithBullets(content: String): String {
    // If content already has bullet points, return as is
    if (content.contains("• ") || content.contains("* ") || content.contains("- ")) {
        return content
    }

    // Otherwise format each line with a bullet point
    return content.split("\n").joinToString("\n") { line ->
        if (line.isNotBlank()) "• $line" else line
    }
}

fun parseResultText(resultText: String): List<Pair<String, String>> {
    val sections = mutableListOf<Pair<String, String>>()
    val lines = resultText.split("\n")

    var currentTitle = ""
    var currentContent = StringBuilder()

    for (line in lines) {
        val trimmedLine = line.trim()

        // Skip empty lines
        if (trimmedLine.isBlank()) continue

        // Check if this is a section header (case-insensitive, with or without leading numbers/dots, and optional colon)
        if (trimmedLine.matches(Regex("(?i)\\d*\\.?\\s*(PRIMARY FINDINGS|DIFFERENTIAL DIAGNOSES|DETAILED OBSERVATIONS|CONFIDENCE LEVEL|RECOMMENDATIONS|X-RAY TYPE).*:?"))
            || trimmedLine.matches(Regex("(?i)(PRIMARY FINDINGS|DIFFERENTIAL DIAGNOSES|DETAILED OBSERVATIONS|CONFIDENCE LEVEL|RECOMMENDATIONS|X-RAY TYPE).*:?"))
        ) {
            // Save previous section if it exists
            if (currentTitle.isNotEmpty()) {
                sections.add(Pair(currentTitle, currentContent.toString().trim()))
                currentContent = StringBuilder()
            }

            currentTitle = trimmedLine
        } else {
            // This is content for the current section
            if (currentTitle.isNotEmpty()) {
                if (currentContent.isNotEmpty()) currentContent.append("\n")
                currentContent.append(trimmedLine)
            } else {
                // This is content without a preceding section header,
                // treat it as part of a generic "Analysis Results" if no sections are yet created.
                if (sections.isEmpty()) {
                    currentTitle = "Analysis Results"
                    currentContent.append(trimmedLine)
                } else {
                    // If there are already sections and this content is not under a new header,
                    // append it to the last section's content.
                    val lastIndex = sections.lastIndex
                    if (lastIndex >= 0) {
                        val lastSection = sections[lastIndex]
                        sections[lastIndex] = Pair(lastSection.first, lastSection.second + "\n" + trimmedLine)
                    }
                }
            }
        }
    }

    // Add the last section if it hasn't been added yet
    if (currentTitle.isNotEmpty() && currentContent.isNotEmpty()) {
        sections.add(Pair(currentTitle, currentContent.toString().trim()))
    }

    return sections
}


suspend fun analyzeImageWithGemini(context: android.content.Context, uri: Uri): String {
    // API key for Gemini
    val apiKey = "AIzaSyAJbi5Pyl3sXWFZD2PtSDlygl-nNIIPJPg" // **IMPORTANT: Replace with your actual Gemini API Key**

    try {
        // Convert image to Base64
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val imageBytes = inputStream?.readBytes() ?: throw Exception("Could not read image")
        inputStream.close()

        // Convert to base64
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // Create the JSON request for Gemini API
        val jsonObject = JSONObject()

        // Create contents array
        val contents = JSONArray()
        val content = JSONObject()

        // Create parts array
        val parts = JSONArray()

        // Add text part
        val textPart = JSONObject()
        textPart.put("text", "Act as a senior radiologist. Analyze this medical image thoroughly and provide:\n\n1. PRIMARY FINDINGS: Key abnormalities detected\n\n2. DIFFERENTIAL DIAGNOSES: Possible conditions\n\n3. DETAILED OBSERVATIONS: Specific observations\n\n4. CONFIDENCE LEVEL: Your confidence in the findings\n\n5. RECOMMENDATIONS: Next steps for the patient\n\n6. X-RAY TYPE: Identify what type of X-ray this is (CHEST, ABDOMINAL, DENTAL, SKULL, SPINAL, BONE, MAMMOGRAM, PELVIC, etc.)\n\nUse professional medical terminology. Highlight urgent findings.")
        parts.put(textPart)

        // Add image part
        val imagePart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Image)
        imagePart.put("inlineData", inlineData)
        parts.put(imagePart)

        // Add parts to content
        content.put("parts", parts)
        contents.put(content)

        // Add contents to main request
        jsonObject.put("contents", contents)

        // Add generation config to reduce hallucinations
        val generationConfig = JSONObject()
        generationConfig.put("temperature", 0.2)
        generationConfig.put("topP", 0.8)
        generationConfig.put("topK", 40)
        jsonObject.put("generationConfig", generationConfig)

        // Convert to string
        val jsonBody = jsonObject.toString()

        Log.d("XrayAnalysis", "Request JSON: ${jsonBody.take(500)}...")

        // Prepare the API request with OkHttp
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        // Define the API URL for Gemini
        val url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        // Execute the request
        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e("XrayAnalysis", "API Error: ${response.code} - $errorBody")
                throw Exception("API request failed with code ${response.code}: ${errorBody.take(100)}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            Log.d("XrayAnalysis", "API Response: ${responseBody.take(500)}...")

            // Parse the JSON response
            val jsonResponse = JSONObject(responseBody)

            if (!jsonResponse.has("candidates") || jsonResponse.getJSONArray("candidates").length() == 0) {
                throw Exception("No candidates in response")
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)

            if (!firstCandidate.has("content")) {
                throw Exception("No content in candidate")
            }

            val content = firstCandidate.getJSONObject("content")

            if (!content.has("parts") || content.getJSONArray("parts").length() == 0) {
                throw Exception("No parts in content")
            }

            val parts = content.getJSONArray("parts")
            val firstPart = parts.getJSONObject(0)

            if (!firstPart.has("text")) {
                throw Exception("No text in part")
            }

            // Get the text content and remove markdown bolding characters
            val geminiResponseText = firstPart.getString("text")
            return@withContext geminiResponseText.replace("**", "").replace("***", "")
        }
    } catch (e: Exception) {
        Log.e("XrayAnalysis", "Error analyzing image", e)
        throw Exception("Failed to analyze image: ${e.message}")
    }
}