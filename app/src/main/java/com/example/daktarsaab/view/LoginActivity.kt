package com.example.daktarsaab.view
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.R
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.delay

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isSplashShown = false // Ensures splash shows on every fresh activity creation

        setContent {
            // Move isSystemInDarkTheme() and theme state inside the composable scope
            val isDarkTheme = isSystemInDarkTheme()
            var darkMode by rememberSaveable { mutableStateOf(isDarkTheme) }

            DaktarSaabTheme(darkTheme = darkMode) {
                var showSplash by remember { mutableStateOf(!isSplashShown) }

                LaunchedEffect(showSplash) {
                    if (showSplash) {
                        // Changed duration to 5 seconds
                        delay(5000)
                        showSplash = false
                        sharedPreferences.edit().putBoolean("isSplashShown", true).apply()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        val context = LocalContext.current
                        LoginScreen(
                            onForgotPasswordClick = {
                                val intent = Intent(context, ForgotPasswordActivity::class.java)
                                context.startActivity(intent)
                            },
                            onSignupClick = {
                                val intent = Intent(context, SignupActivity::class.java)
                                context.startActivity(intent)
                            },
                            darkMode = darkMode,
                            onToggleDarkMode = { darkMode = !darkMode }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    // Lottie animation setup
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("loading.json"))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever // Loop the Lottie animation
    )

    // Text animation states
    val welcomeText = "Welcome to"
    val appNameLine1 = "Daktar"
    val appNameLine2 = "Saab"

    var welcomeVisible by remember { mutableStateOf(false) }
    var appName1Visible by remember { mutableStateOf(false) }
    var appName2Visible by remember { mutableStateOf(false) }

    // Animation parameters for text
    val textAnimationDuration = 700 // milliseconds
    val textDropOffset = (-30).dp // How far the text drops from

    // LaunchedEffect to trigger text animations sequentially
    LaunchedEffect(Unit) {
        delay(400) // Initial delay for text animations to start after Lottie is visible
        welcomeVisible = true
        delay(300) // Stagger for "Daktar"
        appName1Visible = true
        delay(250) // Stagger for "Saab"
        appName2Visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Explicitly White background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Center the content vertically in the column
        ) {
            // 1. Lottie Animation - Size Increased
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(280.dp) // Increased size for Lottie animation
            )

            // 2. "Much Space" between Lottie and Text
            Spacer(modifier = Modifier.height(40.dp)) // Adjustable space

            // 3. Animated Text Elements
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Animated "Welcome to"
                AnimatedVisibility(
                    visible = welcomeVisible,
                    enter = slideInVertically(
                        initialOffsetY = { textDropOffset.roundToPx() },
                        animationSpec = tween(durationMillis = textAnimationDuration, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(durationMillis = textAnimationDuration))
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

                // Animated "Daktar"
                AnimatedVisibility(
                    visible = appName1Visible,
                    enter = slideInVertically(
                        initialOffsetY = { textDropOffset.roundToPx() },
                        animationSpec = tween(durationMillis = textAnimationDuration, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(durationMillis = textAnimationDuration))
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

                // Animated "Saab"
                AnimatedVisibility(
                    visible = appName2Visible,
                    enter = slideInVertically(
                        initialOffsetY = { textDropOffset.roundToPx() },
                        animationSpec = tween(durationMillis = textAnimationDuration, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(durationMillis = textAnimationDuration))
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

private fun Dp.roundToPx(): Int {return this.value.toInt()}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onForgotPasswordClick: () -> Unit,
    onSignupClick: () -> Unit,
    darkMode: Boolean = isSystemInDarkTheme(),
    onToggleDarkMode: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var isCheckingEmail by remember { mutableStateOf(false) }
    var isPasswordLocked by remember { mutableStateOf(false) }
    var lastPasswordEdit by remember { mutableStateOf(0L) }

    val animatedFullText = "Welcome back! Please enter your details."
    val defaultText = "Welcome to Daktar Sab App"
    var displayedText by remember { mutableStateOf("") }
    var isAnimating by remember { mutableStateOf(true) }

    val topVisible = remember { mutableStateOf(false) }
    val bottomVisible = remember { mutableStateOf(false) }

    val doctorAnimation by rememberLottieComposition(LottieCompositionSpec.Asset("doctorlogin.json"))
    val animationProgress by animateLottieCompositionAsState(
        doctorAnimation,
        iterations = LottieConstants.IterateForever
    )

    val isEmailValid = remember(email) {
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            isCheckingEmail = true
            delay(800)
            isCheckingEmail = false
        }
    }

    LaunchedEffect(password) {
        if (password.isNotBlank()) {
            isPasswordLocked = false
            lastPasswordEdit = System.currentTimeMillis()
            delay(1000)
            if (System.currentTimeMillis() - lastPasswordEdit >= 900) {
                isPasswordLocked = true
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        topVisible.value = true
        delay(300)
        bottomVisible.value = true

        for (i in animatedFullText.indices) {
            displayedText = animatedFullText.substring(0, i + 1)
            delay(50)
        }
        delay(1000)

        for (i in animatedFullText.length downTo 0) {
            displayedText = animatedFullText.substring(0, i)
            delay(30)
        }

        displayedText = defaultText
        isAnimating = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dark/Light mode toggle in the top right with padding
        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (darkMode) R.drawable.baseline_light_mode_24
                         else R.drawable.baseline_dark_mode_24
                ),
                contentDescription = if (darkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = topVisible.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -200 }),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LottieAnimation(
                        composition = doctorAnimation,
                        progress = { animationProgress },
                        modifier = Modifier
                            .size(250.dp)
                            .padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Log in ✨",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displayedText,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = bottomVisible.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 200 }),
                exit = fadeOut()
            ) {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_email_24),
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (email.isNotBlank()) {
                                AnimatedContent(
                                    targetState = isCheckingEmail to isEmailValid,
                                    transitionSpec = {
                                        if (targetState.first) {
                                            fadeIn() with fadeOut()
                                        } else {
                                            if (targetState.second) {
                                                scaleIn() + fadeIn() with scaleOut() + fadeOut()
                                            } else {
                                                fadeIn() with fadeOut()
                                            }
                                        }
                                    }
                                ) { (checking, valid) ->
                                    when {
                                        checking -> LoadingCircle()
                                        valid -> BlueCheckmark()
                                        else -> {}
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible)
                                        R.drawable.baseline_visibility_off_24
                                    else
                                        R.drawable.baseline_visibility_24
                                ),
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                modifier = Modifier
                                    .clickable { passwordVisible = !passwordVisible }
                                    .padding(8.dp)
                            )
                        },
                        leadingIcon = {
                            val lockScale = animateFloatAsState(
                                targetValue = if (isPasswordLocked && password.isNotBlank()) 1.2f else 1f,
                                animationSpec = tween(durationMillis = 300),
                                label = "lockScale"
                            )

                            Icon(
                                painter = painterResource(
                                    id = if (isPasswordLocked && password.isNotBlank())
                                        R.drawable.baseline_lock_24
                                    else
                                        R.drawable.baseline_lock_open_24
                                ),
                                contentDescription = null,
                                tint = if (isPasswordLocked && password.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.scale(lockScale.value)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Updated row for "Remember me" and "Forgot password"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                "Remember me",
                                fontSize = 12.sp,  // Smaller text size
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = "Forgot password?",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onForgotPasswordClick() }
                                .padding(8.dp)  // Add padding for better touch target
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { /* TODO: Implement login logic */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() && !isCheckingEmail && isEmailValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Log In", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = " Or log in with ",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { /* TODO: Handle Google sign-in */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img1),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Don't have an account?")
                        TextButton(onClick = onSignupClick) {
                            Text(
                                text = "Sign up",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCircle() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_indicator_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_indicator_rotation"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BlueCheckmark() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_check_circle_24),
            contentDescription = "Valid Email",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Splash Screen Preview")
@Composable
fun SplashScreenPreview() {
    DaktarSaabTheme {
        SplashScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen Preview")
@Composable
fun LoginScreenPreview() {
    DaktarSaabTheme {
        LoginScreen(
            onForgotPasswordClick = {},
            onSignupClick = {}
        )
    }
}