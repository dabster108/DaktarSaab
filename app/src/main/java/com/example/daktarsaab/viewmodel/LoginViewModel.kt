package com.example.daktarsaab.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daktarsaab.model.UserModel
import com.example.daktarsaab.repository.UserRepository
import com.example.daktarsaab.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
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

    // Function to check if user is already logged in
    fun checkLoggedInUser() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val user = userRepository.getUserById(currentUser.uid)

                    if (user != null) {
                        _loginState.value = LoginState.Success(user)
                    } else {
                        // User is authenticated but not in database
                        auth.signOut()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking logged in user", e)
                }
            }
        }
    }
}

// Sealed class to represent different states of the login process
sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: UserModel) : LoginState()
    data class Error(val message: String) : LoginState()
}
