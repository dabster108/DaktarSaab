package com.example.daktarsaab.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daktarsaab.model.UserModel
import com.example.daktarsaab.repository.UserRepository
import com.example.daktarsaab.repository.UserRepositoryImpl
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val TAG = "LoginViewModel"

    // Firebase authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // User repository for checking if user exists in database
    private val userRepository: UserRepository = UserRepositoryImpl()

    // LiveData for UI state management
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    // Function to authenticate user with email and password
    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Email and password are required")
            return
        }

        _loginState.value = LoginState.Loading

        // MODIFIED APPROACH: Always check database first, then only use Firebase Auth if needed
        viewModelScope.launch {
            try {
                // First check if this user exists in our database by email
                val user = userRepository.getUserByEmail(email)

                if (user != null) {
                    // User exists in database, check if password matches
                    if (user.password == password) {
                        // Database password matches! Login successful
                        _loginState.value = LoginState.Success(user)
                        Log.d(TAG, "Login successful using database credentials")

                        // Optionally try to sign in to Firebase Auth in the background
                        // to keep sessions in sync, but don't block the login process
                        try {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Firebase Auth sign-in successful after database auth")
                                }
                                .addOnFailureListener { e ->
                                    Log.d(TAG, "Firebase Auth sign-in failed after database auth: ${e.message}")
                                    // We don't care if this fails since we're using DB auth
                                }
                        } catch (e: Exception) {
                            // Ignore Firebase Auth errors
                            Log.d(TAG, "Ignoring Firebase Auth error: ${e.message}")
                        }
                    } else {
                        // Password doesn't match database - try Firebase Auth as fallback
                        // This helps users who might have reset password through other means
                        authenticateWithFirebaseAuth(email, password)
                    }
                } else {
                    // User not found in database - try Firebase Auth as fallback
                    authenticateWithFirebaseAuth(email, password)
                }
            } catch (e: Exception) {
                // Error with database lookup - try Firebase Auth as fallback
                Log.e(TAG, "Error looking up user in database: ${e.message}")
                authenticateWithFirebaseAuth(email, password)
            }
        }
    }

    // Helper function to sync Firebase Auth with the database password
    private fun syncWithFirebaseAuth(email: String, password: String, user: UserModel) {
        // First try normal sign-in
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Perfect! Both DB and Auth are in sync
                _loginState.value = LoginState.Success(user)
                Log.d(TAG, "Login successful for user with matching DB and Auth passwords")
            }
            .addOnFailureListener { exception ->
                // If auth fails but DB password matches, we need to update Firebase Auth
                // Two approaches: either create a new account or update existing account

                // Try to create a new auth account with the email/password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        _loginState.value = LoginState.Success(user)
                        Log.d(TAG, "Created new Firebase Auth account to match DB credentials")
                    }
                    .addOnFailureListener { createException ->
                        // If we can't create a new account, the account probably exists
                        // but with a different password. In this case, let the user know
                        // they need to use the custom password reset again
                        _loginState.value = LoginState.Error(
                            "Your database password was reset but Firebase Auth is out of sync. " +
                            "Please use 'Forgot Password' once more to fully reset your account."
                        )
                        Log.e(TAG, "Auth and DB passwords are out of sync", createException)
                    }
            }
    }

    // Original Firebase Auth authentication method
    private fun authenticateWithFirebaseAuth(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Get current user ID
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    // Check if user exists in database
                    viewModelScope.launch {
                        try {
                            val user = userRepository.getUserById(userId)

                            if (user != null) {
                                // User exists in database - LOGIN SUCCESSFUL
                                _loginState.value = LoginState.Success(user)
                                Log.d(TAG, "Login successful for user: $userId")
                            } else {
                                // User authenticated but not found in database
                                _loginState.value = LoginState.Error("User not registered. Please sign up")
                                // Sign out the user from Firebase Auth since they don't exist in the database
                                auth.signOut()
                                Log.e(TAG, "User authenticated but not found in database: $userId")
                            }
                        } catch (e: Exception) {
                            _loginState.value = LoginState.Error("Failed to retrieve user data")
                            Log.e(TAG, "Error retrieving user data", e)
                        }
                    }
                } else {
                    _loginState.value = LoginState.Error("Authentication failed")
                    Log.e(TAG, "User ID is null after successful authentication")
                }
            }
            .addOnFailureListener { exception ->
                // Determine specific error message based on the exception
                val errorMessage = when {
                    exception.message?.contains("no user record", ignoreCase = true) == true ->
                        "User not registered. Please sign up"
                    exception.message?.contains("password is invalid", ignoreCase = true) == true ->
                        "Password incorrect"
                    exception.message?.contains("blocked", ignoreCase = true) == true ->
                        "Too many failed attempts. Try again later."
                    else -> "Login failed: ${exception.message}"
                }

                _loginState.value = LoginState.Error(errorMessage)
                Log.e(TAG, "Login failed", exception)
            }
    }

    // Function to handle Google Sign-In
    fun firebaseAuthWithGoogle(googleSignInAccount: GoogleSignInAccount) {
        _loginState.value = LoginState.Loading

        val credential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                // Sign in success
                val firebaseUser = auth.currentUser
                val userId = firebaseUser?.uid

                if (userId != null) {
                    viewModelScope.launch {
                        try {
                            var user = userRepository.getUserById(userId)

                            if (user == null) {
                                // User doesn't exist in database, create a new one
                                val displayName = firebaseUser.displayName ?: ""
                                val email = firebaseUser.email ?: ""

                                // Split display name into first and last name if possible
                                val nameParts = displayName.split(" ", limit = 2)
                                val firstName = nameParts.getOrNull(0) ?: ""
                                val lastName = nameParts.getOrNull(1) ?: ""

                                user = UserModel(
                                    userId = userId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    email = email,
                                    active = "true",
                                    phoneNumber = firebaseUser.phoneNumber ?: "",
                                    f = firstName  // Set f field to firstName for compatibility
                                )

                                val created = userRepository.createUser(user)
                                if (!created) {
                                    _loginState.value = LoginState.Error("Failed to create user profile")
                                    auth.signOut()
                                    return@launch
                                }
                            }

                            _loginState.value = LoginState.Success(user)
                            Log.d(TAG, "Google sign in successful for user: $userId")
                        } catch (e: Exception) {
                            _loginState.value = LoginState.Error("Failed to process Google sign-in")
                            Log.e(TAG, "Error processing Google sign-in", e)
                            auth.signOut()
                        }
                    }
                } else {
                    _loginState.value = LoginState.Error("Authentication failed")
                    Log.e(TAG, "User ID is null after Google authentication")
                }
            }
            .addOnFailureListener { exception ->
                // If sign in fails, display a message to the user
                val errorMessage = when {
                    exception.message?.contains("invalid token", ignoreCase = true) == true ->
                        "Invalid authentication token. Please try again."
                    exception.message?.contains("network error", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    exception.message?.contains("credential is invalid", ignoreCase = true) == true ->
                        "Invalid credentials. Please try again."
                    else -> "Google sign in failed: ${exception.message}"
                }
                _loginState.value = LoginState.Error(errorMessage)
                Log.e(TAG, "Google sign in failed", exception)
            }
    }

    // Handle Google Sign-In Intent result
    fun handleSignInResult(data: Intent?) {
        try {
            // Make sure data is not null
            if (data == null) {
                _loginState.value = LoginState.Error("Google sign-in failed: No data received")
                Log.e(TAG, "Google sign-in failed: data is null")
                return
            }

            // Use GoogleSignIn API to get the SignInAccount from the Intent
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                // Check if we got a valid token
                if (account.idToken.isNullOrEmpty()) {
                    _loginState.value = LoginState.Error("Google sign-in failed: No ID token received")
                    Log.e(TAG, "Google sign-in failed: ID token is null or empty")
                    return
                }

                // Log success and authenticate with Firebase
                Log.d(TAG, "Google sign-in successful, proceeding to Firebase auth")
                firebaseAuthWithGoogle(account)

            } catch (e: ApiException) {
                // Handle specific API exceptions with detailed error messages
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled"
                    GoogleSignInStatusCodes.NETWORK_ERROR -> "Network error during Google sign-in"
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google sign-in failed (${e.statusCode})"
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google sign-in already in progress"
                    GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Invalid Google account selected"
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Sign-in required but not attempted"
                    else -> "Google sign-in failed with status code: ${e.statusCode}"
                }
                _loginState.value = LoginState.Error(errorMessage)
                Log.e(TAG, "Google sign in failed with status code: ${e.statusCode}", e)
            }
        } catch (e: Exception) {
            // Handle other exceptions
            _loginState.value = LoginState.Error("Google sign in failed: ${e.message}")
            Log.e(TAG, "Google sign in failed with exception", e)
        }
    }

    // Function to check if user is already logged in
    fun checkLoggedInUser() {
        // We're not auto-redirecting users anymore - always show login screen
        // Just clear any existing login state to ensure we're starting fresh
        _loginState.value = null

        // Optional: Sign out any existing user to ensure they need to login again
        auth.signOut()
    }
}

// Sealed class to represent different states of the login process
sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: UserModel) : LoginState()
    data class Error(val message: String) : LoginState()
}
