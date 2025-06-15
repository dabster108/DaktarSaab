package com.example.daktarsaab.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.random.Random

class ForgotPasswordViewModel : ViewModel() {
    private val TAG = "ForgotPasswordViewModel"

    // Firebase Auth instance
    private val auth = FirebaseAuth.getInstance()

    // LiveData for password reset state
    private val _resetState = MutableLiveData<ResetPasswordState>()
    val resetState: LiveData<ResetPasswordState> = _resetState

    // Store verification code and email
    private var verificationCode: String = ""
    private var userEmail: String = ""

    // SMTP Configuration - Replace with your actual SMTP settings
    private val smtpHost = "smtp.gmail.com"
    private val smtpPort = "587"
    private val senderEmail = "daktarsaab25@gmail.com" // Replace with your email
    private val senderPassword = "wjyo ezgf vsnn jnud" // Replace with your app password

    // Function to send verification code via email
    fun sendVerificationCode(email: String) {
        if (email.isBlank()) {
            _resetState.value = ResetPasswordState.Error("Email cannot be empty")
            return
        }

        // Set loading state
        _resetState.value = ResetPasswordState.Loading

        // First check if the email exists in our database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Query Firebase database to check if email exists
                val emailExists = checkIfEmailExistsInDatabase(email)

                if (emailExists) {
                    // Email exists, generate verification code and send
                    verificationCode = generateVerificationCode()
                    userEmail = email

                    // Send email with verification code
                    val emailSent = sendEmailWithCode(email, verificationCode)

                    if (emailSent) {
                        _resetState.postValue(ResetPasswordState.CodeSent)
                    } else {
                        _resetState.postValue(ResetPasswordState.Error("Failed to send verification email"))
                    }
                } else {
                    // Email does not exist in our database
                    _resetState.postValue(ResetPasswordState.Error("No account found with this email"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking email or sending verification: ${e.message}", e)
                _resetState.postValue(ResetPasswordState.Error("Error: ${e.message}"))
            }
        }
    }

    // Check if email exists in our Firebase database
    private suspend fun checkIfEmailExistsInDatabase(email: String): Boolean {
        return try {
            // Reference to Firebase database
            val database = FirebaseDatabase.getInstance().reference

            // Get all users and check locally instead of using an index
            // This works around the "Index not defined" error
            val usersRef = database.child("users")
            val dataSnapshot = usersRef.get().await()

            var emailExists = false
            if (dataSnapshot.exists()) {
                // Iterate through all users to find matching email
                for (userSnapshot in dataSnapshot.children) {
                    val userEmail = userSnapshot.child("email").getValue(String::class.java)
                    if (userEmail == email) {
                        emailExists = true
                        break
                    }
                }
            }

            Log.d(TAG, "Email check for $email: exists = $emailExists")
            emailExists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if email exists: ${e.message}", e)
            throw e
        }
    }

    // Function to verify OTP code
    fun verifyCode(code: String) {
        if (code.isBlank()) {
            _resetState.value = ResetPasswordState.Error("Verification code cannot be empty")
            return
        }

        _resetState.value = ResetPasswordState.Loading

        if (code == verificationCode) {
            _resetState.value = ResetPasswordState.CodeVerified
        } else {
            _resetState.value = ResetPasswordState.Error("Invalid verification code")
        }
    }

    // Function to reset password
    fun resetPassword(newPassword: String) {
        if (newPassword.isBlank()) {
            _resetState.value = ResetPasswordState.Error("Password cannot be empty")
            return
        }

        if (newPassword.length < 6) {
            _resetState.value = ResetPasswordState.Error("Password must be at least 6 characters")
            return
        }

        _resetState.value = ResetPasswordState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Send password reset email through Firebase
                auth.sendPasswordResetEmail(userEmail).await()

                // Note: In a real implementation, you'd use a custom auth token or
                // a server-side function to directly reset the password.
                // For simplicity, we're using Firebase's password reset email flow.

                _resetState.postValue(ResetPasswordState.Success)
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting password: ${e.message}", e)
                _resetState.postValue(ResetPasswordState.Error("Error: ${e.message}"))
            }
        }
    }

    // Function to resend verification code
    fun resendVerificationCode() {
        if (userEmail.isBlank()) {
            _resetState.value = ResetPasswordState.Error("Email not specified")
            return
        }

        // Generate a new verification code
        verificationCode = generateVerificationCode()

        viewModelScope.launch(Dispatchers.IO) {
            // Send email with new verification code
            val emailSent = sendEmailWithCode(userEmail, verificationCode)

            if (emailSent) {
                _resetState.postValue(ResetPasswordState.CodeResent)
            } else {
                _resetState.postValue(ResetPasswordState.Error("Failed to resend verification email"))
            }
        }
    }

    // Generate a random 4-digit verification code
    private fun generateVerificationCode(): String {
        return String.format("%04d", Random.nextInt(10000))
    }

    // Send email with verification code
    private fun sendEmailWithCode(recipientEmail: String, code: String): Boolean {
        return try {
            val properties = Properties()
            properties["mail.smtp.host"] = smtpHost
            properties["mail.smtp.port"] = smtpPort
            properties["mail.smtp.auth"] = "true"
            properties["mail.smtp.starttls.enable"] = "true"

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(senderEmail))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
            message.subject = "DaktarSaab Password Reset Verification Code"

            val emailContent = """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                        <h2 style="color: #4285F4;">DaktarSaab Password Reset</h2>
                        <p>We received a request to reset your password. Please use the verification code below:</p>
                        <div style="margin: 20px 0; padding: 10px; background-color: #f8f8f8; border-radius: 5px; text-align: center;">
                            <h1 style="letter-spacing: 5px; font-size: 32px; margin: 0; color: #4285F4;">${code}</h1>
                        </div>
                        <p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>
                        <p>Thank you,<br>The DaktarSaab Team</p>
                    </div>
                </body>
                </html>
            """.trimIndent()

            message.setContent(emailContent, "text/html; charset=utf-8")

            Transport.send(message)
            true
        } catch (e: MessagingException) {
            Log.e(TAG, "Error sending email: ${e.message}", e)
            false
        }
    }
}

// Sealed class for different reset password states
sealed class ResetPasswordState {
    object Loading : ResetPasswordState()
    object CodeSent : ResetPasswordState()
    object CodeResent : ResetPasswordState()
    object CodeVerified : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}
