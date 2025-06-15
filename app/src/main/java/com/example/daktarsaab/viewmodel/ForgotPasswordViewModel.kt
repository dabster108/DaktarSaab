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
                // Find the user in the database by email
                val database = FirebaseDatabase.getInstance().reference
                val usersRef = database.child("users")
                val dataSnapshot = usersRef.get().await()

                var userId: String? = null

                // Find the user with the matching email
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val email = userSnapshot.child("email").getValue(String::class.java)
                        if (email == userEmail) {
                            userId = userSnapshot.key
                            break
                        }
                    }
                }

                if (userId != null) {
                    // 1. First, update the password in the database
                    val userRef = usersRef.child(userId)
                    userRef.child("password").setValue(newPassword).await()

                    // 2. Next, try to update Firebase Auth
                    // This approach will trigger a new anonymous auth session
                    // to handle the Firebase Auth side of things
                    try {
                        // First sign in anonymously
                        val anonymousAuth = auth.signInAnonymously().await()

                        // Then create a new user with the email/password
                        try {
                            auth.createUserWithEmailAndPassword(userEmail, newPassword).await()
                            Log.d(TAG, "Created new user in Firebase Auth during password reset")
                        } catch (e: Exception) {
                            // If user exists, send a password reset email using Firebase's SMTP
                            auth.sendPasswordResetEmail(userEmail).await()
                            Log.d(TAG, "Sent password reset email via Firebase SMTP")
                        }

                        // Sign out the anonymous user
                        auth.signOut()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error with Firebase Auth operations: ${e.message}")
                        // Continue with flow - we don't want to fail the password reset
                        // just because Firebase Auth operations failed
                    }

                    // 3. Send our own confirmation email with the new password
                    try {
                        val emailContent = """
                            <html>
                            <body style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                    <h2 style="color: #4285F4;">DaktarSaab Password Reset Completed</h2>
                                    <p>Your password has been successfully reset.</p>
                                    <p>Your new password is: <strong>${newPassword}</strong></p>
                                    <p>Please use this password to log in to your account.</p>
                                    <p>We recommend changing your password after logging in for security reasons.</p>
                                    <p>Thank you,<br>The DaktarSaab Team</p>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()

                        sendCustomEmail(userEmail, "DaktarSaab Password Reset Completed", emailContent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send password confirmation email: ${e.message}", e)
                        // Continue anyway, as the reset process has completed
                    }

                    // Password successfully updated
                    _resetState.postValue(ResetPasswordState.Success)
                    Log.d(TAG, "Password reset successful for user ID: $userId")
                } else {
                    // User not found - shouldn't happen since we checked earlier
                    _resetState.postValue(ResetPasswordState.Error("User not found"))
                    Log.e(TAG, "User not found when trying to reset password for email: $userEmail")
                }
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

    // Function to send custom email message
    private fun sendCustomEmail(recipientEmail: String, subject: String, htmlContent: String): Boolean {
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
            message.subject = subject
            message.setContent(htmlContent, "text/html; charset=utf-8")

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
