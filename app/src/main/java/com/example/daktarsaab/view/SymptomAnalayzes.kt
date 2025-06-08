package com.example.daktarsaab.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    EnhancedSymptomAnalyzer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EnhancedSymptomAnalyzer() {
    var currentStep by remember { mutableStateOf(1) }
    var bodyPart by remember { mutableStateOf("") }
    var manualDescription by remember { mutableStateOf("") }
    var mcqAnswers by remember { mutableStateOf(listOf("", "", "")) }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Animation states
    val density = LocalDensity.current
    val animatedProgress = remember { Animatable(0f) }

    // Common body parts with icons
    val bodyPartOptions = listOf(
        "Head" to Icons.Outlined.Face,
        "Stomach" to Icons.Outlined.Fastfood,
        "Chest" to Icons.Outlined.Favorite,
        "Back" to Icons.Outlined.Person,
        "Leg" to Icons.Outlined.DirectionsWalk,
        "Arm" to Icons.Outlined.PanTool,
        "Throat" to Icons.Outlined.RecordVoiceOver,
        "Eye" to Icons.Outlined.Visibility,
        "Ear" to Icons.Outlined.Hearing,
        "Tooth" to Icons.Outlined.EmojiEmotions
    )

    val mcqQuestions = listOf(
        "How severe is the pain?" to listOf("Mild", "Moderate", "Severe"),
        "How long have you had this pain?" to listOf("<1 day", "1-3 days", ">3 days"),
        "Is the pain constant or does it come and go?" to listOf("Constant", "Comes and goes", "Not sure")
    )

    LaunchedEffect(currentStep) {
        animatedProgress.animateTo(
            targetValue = currentStep.toFloat() / 6f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            when (currentStep) {
                                1 -> "Select Body Part"
                                2 -> "Choose Input Method"
                                3 -> "Describe Symptoms"
                                4 -> "Answer Questions"
                                5, 6 -> "Analysis Results"
                                else -> "Symptom Analyzer"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (currentStep > 1) {
                            IconButton(onClick = {
                                currentStep = when (currentStep) {
                                    3, 4 -> 2
                                    5 -> 3
                                    6 -> 4
                                    else -> currentStep - 1
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Progress indicator
                LinearProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                val direction = if (targetState > initialState)
                    AnimatedContentTransitionScope.SlideDirection.Left
                else
                    AnimatedContentTransitionScope.SlideDirection.Right

                slideIntoContainer(direction) with
                        slideOutOfContainer(direction)
            },
            label = "screen_transition"
        ) { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (step) {
                    1 -> {
                        BodyPartSelectionScreen(
                            bodyPart = bodyPart,
                            onBodyPartChange = { bodyPart = it },
                            bodyPartOptions = bodyPartOptions,
                            onNext = { currentStep = 2 }
                        )
                    }
                    2 -> {
                        InputMethodSelectionScreen(
                            onManualSelected = { currentStep = 3 },
                            onQuestionsSelected = { currentStep = 4 }
                        )
                    }
                    3 -> {
                        ManualDescriptionScreen(
                            description = manualDescription,
                            onDescriptionChange = { manualDescription = it },
                            onAnalyze = { currentStep = 5 }
                        )
                    }
                    4 -> {
                        MCQQuestionsScreen(
                            questions = mcqQuestions,
                            answers = mcqAnswers,
                            onAnswerSelected = { idx, answer ->
                                mcqAnswers = mcqAnswers.toMutableList().apply { this[idx] = answer }
                            },
                            onNext = { currentStep = 6 }
                        )
                    }
                    5 -> {
                        AnalysisScreen(
                            bodyPart = bodyPart,
                            description = manualDescription,
                            isLoading = isLoading,
                            setLoading = { isLoading = it },
                            responseText = responseText,
                            setResponseText = { responseText = it },
                            onBackToStart = {
                                currentStep = 1
                                bodyPart = ""
                                manualDescription = ""
                                responseText = ""
                            }
                        )
                    }
                    6 -> {
                        AnalysisScreen(
                            bodyPart = bodyPart,
                            description = mcqQuestions.mapIndexed { idx, (q, _) -> "$q ${mcqAnswers[idx]}" }.joinToString("\n"),
                            isLoading = isLoading,
                            setLoading = { isLoading = it },
                            responseText = responseText,
                            setResponseText = { responseText = it },
                            onBackToStart = {
                                currentStep = 1
                                bodyPart = ""
                                mcqAnswers = listOf("", "", "")
                                responseText = ""
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BodyPartSelectionScreen(
    bodyPart: String,
    onBodyPartChange: (String) -> Unit,
    bodyPartOptions: List<Pair<String, ImageVector>>,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Where are you experiencing discomfort?",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Select or type the body part where you're feeling pain",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom search box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            OutlinedTextField(
                value = bodyPart,
                onValueChange = onBodyPartChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("Type body part...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Body part options in a grid
        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bodyPartOptions) { (part, icon) ->
                BodyPartOption(
                    name = part,
                    icon = icon,
                    isSelected = bodyPart == part,
                    onClick = { onBodyPartChange(part) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom button
        Button(
            onClick = onNext,
            enabled = bodyPart.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun BodyPartOption(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val elevation = if (isSelected) 4.dp else 1.dp

    // Pulsating animation for selected item
    val infiniteTransition = rememberInfiniteTransition(label = "selection")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .padding(4.dp)
            .scale(if (isSelected) scale else 1f)
            .shadow(elevation, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            modifier = Modifier
                .size(32.dp)
                .padding(bottom = 8.dp),
            tint = contentColor
        )
        Text(
            text = name,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InputMethodSelectionScreen(
    onManualSelected: () -> Unit,
    onQuestionsSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "How would you like to describe your symptoms?",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Manual input option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .clickable(onClick = onManualSelected),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Type Manually",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Describe your symptoms in your own words",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Questions option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .clickable(onClick = onQuestionsSelected),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Answer Questions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select from predefined options to describe your pain",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ManualDescriptionScreen(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Describe your symptoms",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Provide details about your pain, when it started, what makes it better or worse",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = {
                    Text("Example: I've been having a sharp pain in my head for 2 days, especially when I move suddenly...")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onAnalyze,
            enabled = description.length >= 10,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "Analyze Symptoms",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Search, contentDescription = null)
        }
    }
}

@Composable
fun MCQQuestionsScreen(
    questions: List<Pair<String, List<String>>>,
    answers: List<String>,
    onAnswerSelected: (Int, String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Answer these questions",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        questions.forEachIndexed { idx, (question, options) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    options.forEach { option ->
                        val isSelected = answers[idx] == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onAnswerSelected(idx, option) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAnswerSelected(idx, option) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = answers.all { it.isNotBlank() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "Get Analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Search, contentDescription = null)
        }
    }
}

@Composable
fun AnalysisScreen(
    bodyPart: String,
    description: String,
    isLoading: Boolean,
    setLoading: (Boolean) -> Unit,
    responseText: String,
    setResponseText: (String) -> Unit,
    onBackToStart: () -> Unit
) {
    // Trigger API call when entering this screen
    LaunchedEffect(bodyPart, description) {
        if (responseText.isBlank() && !isLoading) {
            setLoading(true)
            CoroutineScope(Dispatchers.IO).launch {
                val combinedInput = "Pain Location: $bodyPart\nDescription: $description"
                val result = getDiagnosisFromAPI(combinedInput)
                withContext(Dispatchers.Main) {
                    setResponseText(result)
                    setLoading(false)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            LoadingAnimation()
        } else if (responseText.isNotBlank()) {
            EnhancedDiagnosisResult(responseText)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackToStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Start New Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "This analysis is AI-generated and not a substitute for professional medical advice. Always consult a healthcare provider for serious symptoms.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun LoadingAnimation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsating animation
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Analyzing your symptoms...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Our AI is processing your information to provide personalized insights",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EnhancedDiagnosisResult(response: String) {
    val sections = remember(response) {
        response.split("\n(?=\\d+\\.\\s)".toRegex())
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Your Diagnosis Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (sections.isEmpty() || response.contains("Error", ignoreCase = true)) {
            ErrorResultCard(response)
        } else {
            sections.forEachIndexed { index, section ->
                val sectionColor = when (index) {
                    0 -> MaterialTheme.colorScheme.primaryContainer // Diagnosis
                    1 -> MaterialTheme.colorScheme.errorContainer // Possible Diseases
                    2 -> MaterialTheme.colorScheme.secondaryContainer // Treatment
                    3 -> MaterialTheme.colorScheme.tertiaryContainer // Medicine
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val sectionTextColor = when (index) {
                    0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    1 -> MaterialTheme.colorScheme.onErrorContainer
                    2 -> MaterialTheme.colorScheme.onSecondaryContainer
                    3 -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                val sectionIcon = when (index) {
                    0 -> Icons.Default.MedicalServices // Diagnosis
                    1 -> Icons.Default.Error // Possible Diseases
                    2 -> Icons.Default.Healing // Treatment
                    3 -> Icons.Default.Medication // Medicine
                    else -> Icons.Default.Info
                }

                DiagnosisSection(
                    section = section,
                    sectionColor = sectionColor,
                    sectionTextColor = sectionTextColor,
                    sectionIcon = sectionIcon
                )
            }
        }
    }
}

@Composable
fun DiagnosisSection(
    section: String,
    sectionColor: Color,
    sectionTextColor: Color,
    sectionIcon: ImageVector
) {
    val lines = section.lines()
    if (lines.isEmpty()) return

    val titleLine = lines.first()
    val content = lines.drop(1).joinToString("\n").trim()
    val title = titleLine.replaceFirst("\\d+\\.\\s".toRegex(), "")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = sectionColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    sectionIcon,
                    contentDescription = null,
                    tint = sectionTextColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = sectionTextColor
                )
            }

            Divider(
                color = sectionTextColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val bulletPoints = content.split("• ").filter { it.isNotBlank() }
            if (bulletPoints.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bulletPoints.forEach { point ->
                        if (point.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    "• ",
                                    color = sectionTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    point.trim(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = sectionTextColor,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = sectionTextColor,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun ErrorResultCard(errorResponse: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    "Error Processing Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                "Could not generate a proper diagnosis. Please try again with more details, or check your internet connection.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                lineHeight = 22.sp
            )

            if (errorResponse.contains("Error", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Details: ${errorResponse.take(200)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun getDiagnosisFromAPI(symptomDetails: String): String {
    val apiKey = "AIzaSyDvf30lZo9MpS5-AQDUSbC_t04HvmMGJfQ"
    val urlString =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

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
            "API Error: $responseCode - $errorResponse"
        }

        val textPattern = "\"text\":\\s*\"((?:\\\\.|[^\\\"])*?)\"".toRegex()
        val matchResult = textPattern.find(response)

        if (matchResult != null) {
            matchResult.groupValues[1]
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\*", "")
                .replace("### ", "")
                .trim()
        } else {
            "1. Diagnosis: API Response Parsing Error\n\n2. Possible Diseases:\n• Connection issue\n• API format change\n• Invalid response structure\n\n3. Treatment:\n• Try again with different symptoms\n• Check internet connection\n• Verify API key is valid\n\n4. Medicine:\n• None needed for technical errors"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "1. Diagnosis: System Error\n\n2. Possible Diseases:\n• Network connectivity issue\n• API service unavailable\n• Client-side exception\n\n3. Treatment:\n• Check your internet connection\n• Try again later\n• Restart the application\n\n4. Medicine:\n• None needed for technical errors"
    }
}