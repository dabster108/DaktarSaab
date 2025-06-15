package com.example.daktarsaab.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*
import com.example.daktarsaab.R
import com.example.daktarsaab.viewmodel.SignupState
import com.example.daktarsaab.viewmodel.SignupViewModel

class SignupActivity : ComponentActivity() {

    private lateinit var viewModel: SignupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[SignupViewModel::class.java]

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
                    val signupState by viewModel.signupState.observeAsState()

                    when (signupState) {
                        is SignupState.VerificationSent,
                        is SignupState.VerificationPending,
                        is SignupState.CheckingVerification -> {
                            EmailVerificationScreen(
                                email = (signupState as? SignupState.VerificationSent)?.user?.email ?: "",
                                onCheckVerification = { viewModel.checkEmailVerification() },
                                verificationState = signupState,
                                darkMode = darkMode
                            )
                        }
                        is SignupState.VerificationComplete -> {
                            VerificationCompleteScreen(
                                onContinue = {
                                    // Navigate to login screen
                                    val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                darkMode = darkMode
                            )
                        }
                        else -> {
                            SignupScreen(
                                onLoginClick = {
                                    val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                darkMode = darkMode,
                                onToggleDarkMode = { darkMode = !darkMode },
                                viewModel = viewModel,
                                onSignupSuccess = {
                                    // This callback is not used anymore since we're showing verification screen
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onLoginClick: () -> Unit,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    viewModel: SignupViewModel,
    onSignupSuccess: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isCheckingEmail by remember { mutableStateOf(false) }

    // State to track the signup button loading
    var isSigningUp by remember { mutableStateOf(false) }

    // Observe the signup state from ViewModel
    val signupState by viewModel.signupState.observeAsState()
    val context = LocalContext.current

    // Handle signup state changes
    LaunchedEffect(signupState) {
        when (signupState) {
            is SignupState.Loading -> {
                isSigningUp = true
            }
            is SignupState.Success -> {
                isSigningUp = false
                Toast.makeText(context, "Signup successful!", Toast.LENGTH_SHORT).show()
                onSignupSuccess()
            }
            is SignupState.Error -> {
                isSigningUp = false
                val errorMessage = (signupState as SignupState.Error).message
                Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
            else -> {
                // Initial state, do nothing
            }
        }
    }

    val isEmailValid = remember(email) {
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageUploaded by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        isImageUploaded = uri != null
    }

    val transition = updateTransition(isImageUploaded, label = "imageUploadTransition")
    val borderSize by transition.animateDp(label = "borderSize") { uploaded ->
        if (uploaded) 2.dp else 1.dp
    }
    val borderColor by transition.animateColor(label = "borderColor") { uploaded ->
        if (uploaded) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
    }

    val visible = remember { mutableStateOf(false) }
    val fieldsVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible.value = true
        delay(300)
        fieldsVisible.value = true
    }

    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            isCheckingEmail = true
            delay(800)
            isCheckingEmail = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(id = if (darkMode) R.drawable.baseline_dark_mode_24 else R.drawable.baseline_light_mode_24),
                contentDescription = if (darkMode) "Dark mode" else "Light mode",
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )
        }
    }

    CompositionLocalProvider(
        LocalContentColor provides (if (darkMode) Color.White else Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Add Lottie animation at the top
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset("signupdoctor.json"))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Sign Up ✨",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(borderSize, borderColor, CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageUploaded && selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(selectedImageUri)
                                        .build()
                                ),
                                contentDescription = "Profile Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                                contentDescription = "Add Profile Image",
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Please upload your photo",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = fieldsVisible.value,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = null
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = null
                    )

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
                                        else -> Spacer(Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconToggleButton(
                                checked = passwordVisible,
                                onCheckedChange = { passwordVisible = it }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (passwordVisible)
                                            R.drawable.baseline_visibility_off_24
                                        else
                                            R.drawable.baseline_visibility_24
                                    ),
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_lock_24),
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconToggleButton(
                                checked = confirmPasswordVisible,
                                onCheckedChange = { confirmPasswordVisible = it }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (confirmPasswordVisible)
                                            R.drawable.baseline_visibility_off_24
                                        else
                                            R.drawable.baseline_visibility_24
                                    ),
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_lock_24),
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = fieldsVisible.value,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = {
                        // Validate fields
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Call ViewModel to register user
                        viewModel.registerUser(firstName, lastName, email, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !isSigningUp &&
                             firstName.isNotBlank() &&
                             lastName.isNotBlank() &&
                             email.isNotBlank() &&
                             password.isNotBlank() &&
                             confirmPassword.isNotBlank() &&
                             password == confirmPassword
                ) {
                    if (isSigningUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign Up", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = fieldsVisible.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account? ")
                    Text(
                        text = "Log in",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onLoginClick)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCircle() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        )
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

@Composable
fun EmailVerificationScreen(
    email: String,
    onCheckVerification: () -> Unit,
    verificationState: SignupState?,
    darkMode: Boolean
) {
    val isChecking = verificationState is SignupState.CheckingVerification
    val emailToShow = if (email.isBlank()) "your email" else email

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Email verification animation
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("email_verification.json"))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We've sent a verification email to $emailToShow",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please check your inbox and click on the verification link to complete your registration.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCheckVerification,
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("I've Verified My Email", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Didn't receive the email? Check your spam folder or click the button to check if your email has been verified.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun VerificationCompleteScreen(
    onContinue: () -> Unit,
    darkMode: Boolean
) {
    // Auto redirect after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onContinue()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("email_success.json"))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            isPlaying = true,
            speed = 0.7f,
            restartOnPlay = false
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Email Verified Successfully!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your email has been verified. You can now log in to your account.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Redirecting to login page...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignupScreenPreview() {
    DaktarSaabTheme(darkTheme = false) {
        SignupScreen(
            onLoginClick = {}, darkMode = false, onToggleDarkMode = {},
            viewModel = TODO(),
            onSignupSuccess = TODO()
        )
    }
}