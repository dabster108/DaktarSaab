
package com.example.daktarsaab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import kotlinx.coroutines.delay

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaktarSaabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ForgotPasswordScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ForgotPasswordScreen() {
    var email by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(1) }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isCheckingEmail by remember { mutableStateOf(false) }

    val titleVisible = remember { mutableStateOf(false) }
    val subtitleVisible = remember { mutableStateOf(false) }
    val formVisible = remember { mutableStateOf(false) }
    val buttonVisible = remember { mutableStateOf(false) }

    val otpBoxes = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    // Email validation check
    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            isCheckingEmail = true
            delay(800) // Simulate network/validation delay
            isCheckingEmail = false
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        titleVisible.value = true
        delay(200)
        subtitleVisible.value = true
        delay(200)
        formVisible.value = true
        delay(200)
        buttonVisible.value = true
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            otpBoxes.forEachIndexed { index, animatable ->
                delay(index * 100L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier
                .size(32.dp)
                .clickable { /* Handle back navigation */ }
                .padding(bottom = 16.dp)
        )

        AnimatedVisibility(
            visible = titleVisible.value,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Text(
                text = when (currentStep) {
                    1 -> "Forgot Password?"
                    2 -> "Verify OTP"
                    else -> "Reset Password"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = subtitleVisible.value,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Text(
                text = when (currentStep) {
                    1 -> "Enter your email to receive a verification code"
                    2 -> "We've sent a code to your email"
                    else -> "Create a new password"
                },
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        AnimatedVisibility(
            visible = formVisible.value,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
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
                                                // Loading state
                                                fadeIn() with fadeOut()
                                            } else {
                                                // Transition between loading and validation states
                                                if (targetState.second) {
                                                    // Valid state
                                                    scaleIn() + fadeIn() with scaleOut() + fadeOut()
                                                } else {
                                                    // Invalid state
                                                    fadeIn() with fadeOut()
                                                }
                                            }
                                        }
                                    ) { (checking, valid) ->
                                        when {
                                            checking -> {
                                                val infiniteTransition = rememberInfiniteTransition()
                                                val rotation by infiniteTransition.animateFloat(
                                                    initialValue = 0f,
                                                    targetValue = 360f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(1000, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Restart
                                                    )
                                                )

                                                CircularProgressIndicator(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .rotate(rotation),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            valid -> {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.baseline_check_circle_24),
                                                    contentDescription = "Valid Email",
                                                    tint = Color.Green,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.baseline_error_24),
                                                    contentDescription = "Invalid Email",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    2 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            otpBoxes.forEachIndexed { index, animatable ->
                                val scale by animateFloatAsState(
                                    targetValue = animatable.value,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .scale(scale)
                                        .border(
                                            width = 1.dp,
                                            color = if (otp.length > index) Color.Blue else Color.Gray,
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (otp.length > index) otp[index].toString() else "",
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }

                        TextField(
                            value = otp,
                            onValueChange = { if (it.length <= 4) otp = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Resend Code",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { /* Resend OTP */ }
                            )
                        }
                    }

                    else -> {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
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
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = buttonVisible.value,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = {
                    when (currentStep) {
                        1 -> currentStep = 2
                        2 -> currentStep = 3
                        else -> { /* Complete password reset */ }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = when (currentStep) {
                    1 -> email.isNotBlank() && isEmailValid && !isCheckingEmail
                    2 -> otp.length == 4
                    else -> newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword == confirmPassword
                }
            ) {
                Text(
                    text = when (currentStep) {
                        1 -> "Send Code"
                        2 -> "Verify"
                        else -> "Reset Password"
                    },
                    fontSize = 16.sp
                )
            }
        }

        if (currentStep == 1) {
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = buttonVisible.value,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Remember your password? ")
                    Text(
                        text = "Log in",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Navigate to login */ }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreenPreview() {
    DaktarSaabTheme {
        ForgotPasswordScreen()
    }
}