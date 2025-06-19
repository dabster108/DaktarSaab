package com.example.daktarsaab.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.daktarsaab.R
import com.example.daktarsaab.ui.theme.DaktarSaabTheme
import com.example.daktarsaab.viewmodel.ForgotPasswordViewModel
import com.example.daktarsaab.viewmodel.ResetPasswordState
import kotlinx.coroutines.delay

class ForgotPasswordActivity : ComponentActivity() {
    private lateinit var viewModel: ForgotPasswordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.black)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ForgotPasswordViewModel::class.java]

        setContent {
            DaktarSaabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SimpleForgotPasswordScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onComplete = {
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackClick: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    // Basic state
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) } // 1: Email, 2: OTP, 3: Password

    // Observe ViewModel state
    val resetState by viewModel.resetState.observeAsState()

    // Handle state changes
    LaunchedEffect(resetState) {
        when (resetState) {
            is ResetPasswordState.CodeSent -> {
                Toast.makeText(context, "Verification code sent to your email", Toast.LENGTH_SHORT).show()
                currentStep = 2
            }
            is ResetPasswordState.CodeVerified -> {
                Toast.makeText(context, "Code verified successfully", Toast.LENGTH_SHORT).show()
                currentStep = 3
            }
            is ResetPasswordState.Success -> {
                Toast.makeText(context, "Password reset successful!", Toast.LENGTH_SHORT).show()
                delay(1000)
                onComplete()
            }
            is ResetPasswordState.Error -> {
                val errorMessage = (resetState as ResetPasswordState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            else -> { /* Initial or loading state */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar with back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }

        // Title
        Text(
            text = when (currentStep) {
                1 -> "Forgot Password"
                2 -> "Enter Verification Code"
                else -> "Create New Password"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Form content based on current step
        when (currentStep) {
            // Email Entry
            1 -> {
                Text(
                    text = "Enter your email address",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (email.isNotBlank()) {
                            viewModel.sendVerificationCode(email)
                        } else {
                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank()
                ) {
                    Text("Send Verification Code")
                }
            }

            // OTP Verification
            2 -> {
                Text(
                    text = "Enter the 4-digit verification code sent to your email",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        // Only accept digits and limit to 4 characters
                        if (it.all { c -> c.isDigit() } && it.length <= 4) {
                            otp = it
                        }
                    },
                    label = { Text("Verification Code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (otp.length == 4) {
                            viewModel.verifyCode(otp)
                        } else {
                            Toast.makeText(context, "Please enter the complete 4-digit code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = otp.length == 4
                ) {
                    Text("Verify Code")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Didn't receive the code? ",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Resend",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            viewModel.resendVerificationCode()
                        }
                    )
                }
            }

            // New Password
            else -> {
                Text(
                    text = "Create a new password",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // New password field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) R.drawable.baseline_visibility_off_24
                                    else R.drawable.baseline_visibility_24
                                ),
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true
                )

                // Confirm password field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                if (newPassword.isNotBlank() && confirmPassword.isNotBlank() &&
                    newPassword != confirmPassword) {
                    Text(
                        text = "Passwords don't match",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        when {
                            newPassword.length < 6 -> {
                                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            }
                            newPassword != confirmPassword -> {
                                Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                viewModel.resetPassword(newPassword)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank() &&
                            newPassword == confirmPassword && newPassword.length >= 6
                ) {
                    Text("Reset Password")
                }
            }
        }
    }

    // Loading overlay
    if (resetState is ResetPasswordState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
