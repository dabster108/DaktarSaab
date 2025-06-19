package com.example.daktarsaab.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if we're coming back from another activity
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action != null && intent.action == Intent.ACTION_MAIN) {
            // If we're not the root activity and this is a launcher intent, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("SplashActivity", "onCreate: Starting splash screen")

        window.statusBarColor = getColor(android.R.color.black)

        setContent {
            DaktarSaabTheme {
                SplashScreen(onSplashComplete = {
                    Log.d("SplashActivity", "Splash animation complete, navigating to LoginActivity")
                    try {
                        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Log.e("SplashActivity", "Error starting LoginActivity", e)
                    }
                })
            }
        }
    }

    override fun onBackPressed() {
        // Prevent going back from splash screen
        return
    }
}

@Composable
private fun SplashScreen(onSplashComplete: () -> Unit) {
    // Lottie animation setup
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("loading.json"))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    // Text animation states
    val welcomeText = "Welcome to"
    val appNameLine1 = "Daktar"
    val appNameLine2 = "Saab"

    var welcomeVisible by remember { mutableStateOf(false) }
    var appName1Visible by remember { mutableStateOf(false) }
    var appName2Visible by remember { mutableStateOf(false) }

    // Trigger navigation after splash duration
    LaunchedEffect(Unit) {
        delay(400) // Initial delay
        welcomeVisible = true
        delay(300)
        appName1Visible = true
        delay(250)
        appName2Visible = true

        // Total splash duration
        delay(4050) // Remaining time to make total 5 seconds
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lottie Animation
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(280.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Animated Text Elements
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // "Welcome to" text
                AnimatedVisibility(
                    visible = welcomeVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
                ) {
                    Text(
                        text = welcomeText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // "Daktar" text
                AnimatedVisibility(
                    visible = appName1Visible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
                ) {
                    Text(
                        text = appNameLine1,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // "Saab" text
                AnimatedVisibility(
                    visible = appName2Visible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
                ) {
                    Text(
                        text = appNameLine2,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
