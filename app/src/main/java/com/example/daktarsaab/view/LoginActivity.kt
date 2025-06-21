package com.example.daktarsaab.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.R
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.example.daktarsaab.viewmodel.LoginState
import com.example.daktarsaab.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import kotlinx.coroutines.delay

class LoginActivity : ComponentActivity() {
    private lateinit var viewModel: LoginViewModel
    private val TAG = "LoginActivity"
    private val SPLASH_SHOWN_KEY = "splash_shown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the LoginViewModel
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        // Set the context for notifications
        viewModel.setContext(this)

        // Set status bar color to match the theme
        window.statusBarColor = getColor(R.color.black)

        // Get the application-wide shared preferences
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Check if app is launched fresh from the launcher
        val isFromLauncher = intent.action == Intent.ACTION_MAIN &&
                intent.categories?.contains(Intent.CATEGORY_LAUNCHER) == true

        // Reset splash preference when app is launched from launcher
        if (isFromLauncher) {
            sharedPreferences.edit().remove(SPLASH_SHOWN_KEY).apply()
        }

        // Check if splash has been shown this session
        val isSplashShown = sharedPreferences.getBoolean(SPLASH_SHOWN_KEY, false)

        // Check if we're coming from signup or other activity
        val isComingFromSignup = intent.getBooleanExtra("FROM_SIGNUP", false)

        // Check if we're resuming after a sign-in attempt
        viewModel.checkLoggedInUser()

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(false) }
            val isSystemDark = isSystemInDarkTheme()

            // Use system preference on first launch
            LaunchedEffect(Unit) {
                darkMode = isSystemDark
            }

            DaktarSaabTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Theme toggle button
                        IconButton(
                            onClick = { darkMode = !darkMode },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (darkMode) R.drawable.baseline_dark_mode_24
                                    else R.drawable.baseline_light_mode_24
                                ),
                                contentDescription = if (darkMode) "Dark mode" else "Light mode",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Main content in a Column without scroll
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Lottie animation with reduced size
                            val doctorAnimation by rememberLottieComposition(
                                LottieCompositionSpec.Asset("doctorlogin.json")
                            )
                            val animationProgress by animateLottieCompositionAsState(
                                composition = doctorAnimation,
                                iterations = LottieConstants.IterateForever
                            )

                            LottieAnimation(
                                composition = doctorAnimation,
                                progress = { animationProgress },
                                modifier = Modifier
                                    .size(180.dp)
                                    .padding(bottom = 16.dp)
                            )

                            // Debug logging for login states
                            val context = LocalContext.current
                            val loginState by viewModel.loginState.observeAsState()

                            LaunchedEffect(loginState) {
                                when (loginState) {
                                    is LoginState.Loading -> {
                                        Log.d(TAG, "Login state: Loading")
                                    }
                                    is LoginState.Success -> {
                                        Log.d(TAG, "Login state: Success")
                                        val user = (loginState as LoginState.Success).user
                                        Log.d(TAG, "Logged in user: ${user.email}, ID: ${user.userId}")

                                        // Show success notification and navigate
                                        Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(context, DashboardActivity::class.java).apply {
                                            putExtra("FROM_LOGIN", true)
                                            putExtra("USER_ID", user.userId)
                                        }
                                        context.startActivity(intent)
                                        finish()
                                    }
                                    is LoginState.Error -> {
                                        val errorMsg = (loginState as LoginState.Error).message
                                        if (errorMsg.isNotEmpty()) {
                                            Log.d(TAG, "Login state: Error - $errorMsg")
                                        }
                                    }
                                    null -> {
                                        Log.d(TAG, "Login state: Initial null state")
                                    }
                                }
                            }

                            var email by remember { mutableStateOf("") }
                            var password by remember { mutableStateOf("") }
                            var passwordVisible by remember { mutableStateOf(false) }
                            var rememberMe by remember { mutableStateOf(true) }
                            var isCheckingEmail by remember { mutableStateOf(false) }
                            var isPasswordLocked by remember { mutableStateOf(false) }
                            var lastPasswordEdit by remember { mutableStateOf(0L) }

                            // Define isEmailValid here to fix the unresolved reference
                            val isEmailValid = remember(email) {
                                Patterns.EMAIL_ADDRESS.matcher(email).matches()
                            }

                            // State to track login status
                            var isLoggingIn by remember { mutableStateOf(false) }
                            var errorMessage by remember { mutableStateOf<String?>(null) }

                            // Observe login state
                            if (viewModel != null) {
                                val loginState by viewModel.loginState.observeAsState()

                                LaunchedEffect(loginState) {
                                    when (loginState) {
                                        is LoginState.Loading -> {
                                            isLoggingIn = true
                                            errorMessage = null
                                        }
                                        is LoginState.Success -> {
                                            isLoggingIn = false
                                            errorMessage = null
                                            // Navigation is handled in the Activity
                                        }
                                        is LoginState.Error -> {
                                            isLoggingIn = false
                                            errorMessage = (loginState as LoginState.Error).message
                                            // Show Toast for error message
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                        }
                                        null -> {
                                            isLoggingIn = false
                                            errorMessage = null
                                        }
                                    }
                                }
                            }

                            val animatedFullText = "Welcome back! Please enter your details."
                            val defaultText = "Welcome to Daktar Sab App"
                            var displayedText by remember { mutableStateOf("") }
                            var isAnimating by remember { mutableStateOf(true) }

                            val topVisible = remember { mutableStateOf(false) }
                            val bottomVisible = remember { mutableStateOf(false) }

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

                            AnimatedVisibility(
                                visible = topVisible.value,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { -200 }),
                                exit = fadeOut()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
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
                                        onValueChange = {
                                            email = it
                                            // Clear error message when user types
                                            if (errorMessage?.contains("not registered", ignoreCase = true) == true) {
                                                errorMessage = null
                                            }
                                        },
                                        label = { Text("Email") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        isError = errorMessage?.contains("not registered", ignoreCase = true) == true,
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.baseline_email_24),
                                                contentDescription = null
                                            )
                                        },
                                        trailingIcon = {
                                            if (email.isNotBlank()) {
                                                @OptIn(ExperimentalAnimationApi::class)
                                                AnimatedContent(
                                                    targetState = Pair(isCheckingEmail, isEmailValid),
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
                                                ) { state ->
                                                    val (checking, valid) = state
                                                    when {
                                                        checking -> LoadingCircle()
                                                        valid -> BlueCheckmark()
                                                        else -> {}
                                                    }
                                                }
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorScheme.primary,
                                            unfocusedBorderColor = colorScheme.outline,
                                            focusedLabelColor = colorScheme.primary,
                                            errorBorderColor = colorScheme.error,
                                            errorLabelColor = colorScheme.error
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = {
                                            password = it
                                            // Clear error message when user types
                                            if (errorMessage?.contains("Password incorrect", ignoreCase = true) == true) {
                                                errorMessage = null
                                            }
                                        },
                                        label = { Text("Password") },
                                        modifier = Modifier.fillMaxWidth(),
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        isError = errorMessage?.contains("Password incorrect", ignoreCase = true) == true,
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
                                                    colorScheme.primary
                                                else
                                                    colorScheme.onSurface,
                                                modifier = Modifier.scale(lockScale.value)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorScheme.primary,
                                            unfocusedBorderColor = colorScheme.outline,
                                            focusedLabelColor = colorScheme.primary,
                                            errorBorderColor = colorScheme.error,
                                            errorLabelColor = colorScheme.error
                                        )
                                    )

                                    // Error messages section - only show here after password field
                                    if (errorMessage != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        when {
                                            errorMessage?.contains("Password incorrect", ignoreCase = true) == true -> {
                                                Text(
                                                    text = "Incorrect password!!",
                                                    color = colorScheme.error,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                            errorMessage?.contains("not registered", ignoreCase = true) == true -> {
                                                Text(
                                                    text = "User not registered. Click signup",
                                                    color = colorScheme.error,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Row for "Remember me" and "Forgot password" - moved up closer to password field
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),  // Reduced padding to move up
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
                                                    checkedColor = colorScheme.primary
                                                )
                                            )
                                            Text(
                                                "Remember me",
                                                fontSize = 12.sp,  // Smaller text size
                                                color = colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }

                                        Text(
                                            text = "Forgot password?",
                                            color = colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
                                                    startActivity(intent)
                                                }
                                                .padding(8.dp)  // Add padding for better touch target
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (viewModel != null) {
                                                viewModel.loginUser(email, password)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        enabled = !isLoggingIn && email.isNotBlank() && password.isNotBlank() && !isCheckingEmail && isEmailValid,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary,
                                            contentColor = colorScheme.onPrimary,
                                            disabledContainerColor = colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        if (isLoggingIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Log In", fontSize = 16.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Or login with section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Divider(
                                            modifier = Modifier.weight(1f),
                                            color = colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = " Or log in with ",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                                        )
                                        Divider(
                                            modifier = Modifier.weight(1f),
                                            color = colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Google sign in button
                                    OutlinedButton(
                                        onClick = {
                                            // Define onGoogleSignInClick handler
                                            // TODO: Implement Google Sign-In
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestEmail()
                                                .build()
                                            val googleSignInClient = GoogleSignIn.getClient(this@LoginActivity, gso)
                                            // Launch sign-in intent
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        border = ButtonDefaults.outlinedButtonBorder,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = colorScheme.onSurface
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img1),
                                                contentDescription = "Google Logo",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .padding(4.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sign in with Google")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))  // Reduced spacing before sign up text

                                    // Keep only this "Don't have an account" section
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),  // Added padding to ensure visibility
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Don't have an account? ",
                                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Sign up",
                                            color = colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                                                startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            }
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
            .background(colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
            strokeWidth = 2.dp,
            color = colorScheme.primary
        )
    }
}

@Composable
private fun BlueCheckmark() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_check_circle_24),
            contentDescription = "Valid Email",
            tint = colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun LoginScreenPreview() {
    DaktarSaabTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Preview content
                    Text(
                        text = "Log in ✨",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}