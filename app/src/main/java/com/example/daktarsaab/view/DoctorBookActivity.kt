package com.example.daktarsaab.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.R
import com.example.daktarsaab.view.ui.theme.DaktarSaabTheme
import androidx.lifecycle.LifecycleOwner

class DoctorBookActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaktarSaabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DoctorBookingContainer()
                }
            }
        }
    }
}

data class DoctorCategory(
    val title: String,
    val iconRes: Int,
    val description: String
)

@Composable
fun DoctorBookingContainer() {
    val PrimaryBlue = Color(0xFF4285F4)
    val LightBlueBackground = Color(0xFFE3F2FD)
    val TextDark = Color(0xFF212121)
    val TextLight = Color(0xFF616161)
    val CardBackground = Color.White
    val OutlineColor = Color(0xFFBBDEFB)

    var showBookingGrid by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<DoctorCategory?>(null) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalContext.current as LifecycleOwner

    DisposableEffect(showBookingGrid, selectedCategory, activity, lifecycleOwner) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    selectedCategory != null -> selectedCategory = null
                    showBookingGrid -> showBookingGrid = false
                    else -> {
                        isEnabled = false
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }
                }
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBlueBackground)
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = showBookingGrid,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(600)) { fullWidth -> if (targetState) fullWidth else -fullWidth } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(600)) { fullWidth -> if (targetState) -fullWidth else fullWidth } + fadeOut(animationSpec = tween(300)))
            }, label = "Screen Transition"
        ) { targetShowGrid ->
            if (targetShowGrid) {
                DoctorBookingGridScreen(
                    primaryBlue = PrimaryBlue,
                    cardBackground = CardBackground,
                    textDark = TextDark,
                    textLight = TextLight,
                    outlineColor = OutlineColor,
                    onBackToWelcome = { showBookingGrid = false },
                    onCategorySelected = { category -> selectedCategory = category }
                )
            } else {
                WelcomeScreen(
                    onStartBookingClick = { showBookingGrid = true },
                    primaryBlue = PrimaryBlue,
                    cardBackground = CardBackground,
                    textDark = TextDark
                )
            }
        }

        // Show small popup dialog for details
        selectedCategory?.let { category ->
            AlertDialog(
                onDismissRequest = { selectedCategory = null },
                containerColor = Color.White,
                title = {
                    Text(
                        "Book appointment for ${category.title}",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = category.iconRes),
                            contentDescription = category.title,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            category.description,
                            color = TextDark,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        println("Booking for ${category.title}!")
                        selectedCategory = null
                    }) { Text("Book Now") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { selectedCategory = null }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onStartBookingClick: () -> Unit,
    primaryBlue: Color,
    cardBackground: Color,
    textDark: Color
) {
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Doctor Booking",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue,
            textAlign = TextAlign.Left,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = contentVisible, enter = fadeIn(animationSpec = tween(600))) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset("doctoractivity.json"))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier
                        .weight(0.4f)
                        .height(140.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = "Welcome to Daktar Saab!\nDoctor Booking!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textDark,
                    textAlign = TextAlign.Start,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .weight(0.6f)
                        .padding(start = 4.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> -fullHeight / 2 }, animationSpec = tween(500, 100)) + fadeIn()
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "How It Works:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. Tap 'Start Booking' below to browse categories.\n" +
                                "2. Select a specialty to learn more.\n" +
                                "3. Proceed to see available doctors and book your appointment!",
                        fontSize = 14.sp,
                        color = textDark,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 2 }, animationSpec = tween(500, 200)) + fadeIn()
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Specialties Available:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ENT, Neurology, Dermatology, Pulmonology, Cardiology, Orthopedics & more.",
                        fontSize = 14.sp,
                        color = textDark,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        AnimatedVisibility(visible = contentVisible, enter = fadeIn(tween(600, 300))) {
            Button(
                onClick = onStartBookingClick,
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(6.dp)
            ) {
                Text("Start Booking", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun DoctorBookingGridScreen(
    primaryBlue: Color,
    cardBackground: Color,
    textDark: Color,
    textLight: Color,
    outlineColor: Color,
    onBackToWelcome: () -> Unit,
    onCategorySelected: (DoctorCategory) -> Unit
) {
    val categories = remember {
        listOf(
            DoctorCategory("ENT", R.drawable.ent, "Specializes in conditions of the ear, nose, and throat."),
            DoctorCategory("Neuro", R.drawable.head, "Focuses on disorders of the nervous system."),
            DoctorCategory("Skin", R.drawable.skin, "Deals with diseases of the skin, hair, and nails."),
            DoctorCategory("Lungs", R.drawable.lungs, "Specializes in diseases of the respiratory tract."),
            DoctorCategory("Heart", R.drawable.heart, "Focuses on disorders of the heart and blood vessels."),
            DoctorCategory("Ortho", R.drawable.bone, "Specializes in conditions affecting the musculoskeletal system.")
        )
    }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalContext.current as LifecycleOwner

    DisposableEffect(activity, lifecycleOwner) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBackToWelcome()
        }
        activity?.onBackPressedDispatcher?.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Book an Appointment",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories) { category ->
                DoctorCategoryCard(
                    category = category,
                    modifier = Modifier
                        .size(120.dp) // Bigger circle
                        .clickable { onCategorySelected(category) },
                    primaryBlue = primaryBlue,
                    cardBackground = cardBackground,
                    textDark = textDark,
                    outlineColor = outlineColor,
                    iconSize = 64.dp, // Bigger icon
                    textSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun DoctorCategoryCard(
    category: DoctorCategory,
    modifier: Modifier = Modifier,
    cardBackground: Color,
    textDark: Color,
    outlineColor: Color,
    primaryBlue: Color,
    iconSize: Dp = 64.dp,
    textSize: TextUnit = 18.sp
) {
    Card(
        modifier = modifier
            .clip(CircleShape)
            .background(cardBackground)
            .animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(2.dp, outlineColor)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = category.iconRes),
                contentDescription = category.title,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(10.dp))
            Text(
                category.title,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                color = textDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewDoctorBooking() {
    DaktarSaabTheme { DoctorBookingContainer() }
}