package com.example.daktarsaab.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daktarsaab.R
import com.example.daktarsaab.view.ui.theme.DaktarSaabTheme
import com.airbnb.lottie.compose.*
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner // Import LifecycleOwner

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

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalContext.current as LifecycleOwner // Get LifecycleOwner

    // Handle back button press
    DisposableEffect(showBookingGrid, activity, lifecycleOwner) { // Add lifecycleOwner to keys
        val callback = object : OnBackPressedCallback(true /* enabled */) {
            override fun handleOnBackPressed() {
                if (showBookingGrid) {
                    showBookingGrid = false // Go back to WelcomeScreen
                } else {
                    // If on WelcomeScreen, let the default back behavior (exit app) happen
                    // To avoid infinite loop, disable callback temporarily and then call onBackPressed
                    isEnabled = false // Disable this callback to allow default behavior
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
        // Corrected: Pass lifecycleOwner and callback directly
        activity?.onBackPressedDispatcher?.addCallback(lifecycleOwner, callback)

        onDispose {
            callback.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBlueBackground)
            .padding(16.dp)
    ) {
        if (showBookingGrid) {
            DoctorBookingGridScreen(
                primaryBlue = PrimaryBlue,
                cardBackground = CardBackground,
                textDark = TextDark,
                textLight = TextLight,
                outlineColor = OutlineColor,
                onBackToWelcome = { showBookingGrid = false }
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
}

@Composable
fun WelcomeScreen(
    onStartBookingClick: () -> Unit,
    primaryBlue: Color,
    cardBackground: Color,
    textDark: Color
) {
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Doctor Booking",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlue,
            textAlign = TextAlign.Left,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 600))
        ) {
            Row(
                modifier = Modifier
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
            enter = slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(durationMillis = 500, delayMillis = 100)) + fadeIn()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 500, delayMillis = 200)) + fadeIn()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            Button(
                onClick = onStartBookingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "Start Booking",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun DoctorBookingGridScreen(
    primaryBlue: Color,
    cardBackground: Color,
    textDark: Color,
    textLight: Color,
    outlineColor: Color,
    onBackToWelcome: () -> Unit
) {
    val categories = listOf(
        DoctorCategory(
            "ENT",
            R.drawable.ent,
            "Specializes in conditions of the ear, nose, and throat (Otolaryngology)."
        ),
        DoctorCategory(
            "Neuro",
            R.drawable.head,
            "Focuses on disorders of the nervous system, including the brain, spinal code, and nerves (Neurology)."
        ),
        DoctorCategory(
            "Skin",
            R.drawable.skin,
            "Deals with diseases of the skin, hair, and nails (Dermatology)."
        ),
        DoctorCategory(
            "Lungs",
            R.drawable.lungs,
            "Specializes in diseases of the respiratory tract, including the lungs and bronchial tubes (Pulmonology)."
        ),
        DoctorCategory(
            "Heart",
            R.drawable.heart,
            "Focuses on disorders of the heart and blood vessels (Cardiology)."
        ),
        DoctorCategory(
            "Ortho",
            R.drawable.bone,
            "Specializes in conditions affecting the musculoskeletal system, including bones, joints, ligaments, tendons, and muscles (Orthopedics)."
        )
    )

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalContext.current as LifecycleOwner // Get LifecycleOwner

    // Handle back button specifically for this screen
    DisposableEffect(activity, lifecycleOwner) { // Add lifecycleOwner to keys
        val callback = object : OnBackPressedCallback(true /* enabled */) {
            override fun handleOnBackPressed() {
                onBackToWelcome()
            }
        }
        // Corrected: Pass lifecycleOwner and callback directly
        activity?.onBackPressedDispatcher?.addCallback(lifecycleOwner, callback)

        onDispose {
            callback.remove()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            // Spacer to push content down from the top
            Spacer(modifier = Modifier.height(24.dp)) // Added spacing here

            Text(
                text = "Book an Appointment",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        items(categories.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                rowItems.forEach { category ->
                    DoctorItem(
                        category = category,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        primaryBlue = primaryBlue,
                        cardBackground = cardBackground,
                        textDark = textDark,
                        textLight = textLight,
                        outlineColor = outlineColor
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                }
            }
        }
    }
}

@Composable
fun DoctorItem(
    category: DoctorCategory,
    modifier: Modifier = Modifier,
    cardBackground: Color,
    textDark: Color,
    textLight: Color,
    outlineColor: Color,
    primaryBlue: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, outlineColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .clip(CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = category.iconRes),
                contentDescription = category.title,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textDark,
                textAlign = TextAlign.Center
            )

            AnimatedVisibility(visible = expanded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = category.description,
                        fontSize = 12.sp,
                        color = textLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp)) // Small spacer before the icon button
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = primaryBlue // Use your primary color for the icon
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewDoctorBooking() {
    DaktarSaabTheme {
        DoctorBookingContainer()
    }
}