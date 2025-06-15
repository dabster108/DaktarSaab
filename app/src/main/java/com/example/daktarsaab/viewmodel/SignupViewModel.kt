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

class SignupViewModel : ViewModel() {
    private val TAG = "SignupViewModel"

    // Firebase authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // User repository implementation
    private val userRepository: UserRepository = UserRepositoryImpl()

    // LiveData for UI state management
    private val _signupState = MutableLiveData<SignupState>()
    val signupState: LiveData<SignupState> = _signupState

    // Function to register a new user with email and password
    fun registerUser(firstName: String, lastName: String, email: String, password: String) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            _signupState.value = SignupState.Error("All fields are required")
            return
        }

        _signupState.value = SignupState.Loading

        viewModelScope.launch {
            try {
                // Create user with Firebase Authentication
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Get the Firebase user ID
                            val userId = auth.currentUser?.uid ?: ""

                            // Create user model with proper field mapping
                            val user = UserModel(
                                userId = userId,
                                firstName = firstName,
                                lastName = lastName,
                                phoneNumber = "", // Empty for now
                                active = "true",
                                email = email,
                                f = firstName // Set f field to firstName for compatibility
                            )

                            // Save user to Firebase database
                            viewModelScope.launch {
                                val result = userRepository.createUser(user)
                                if (result) {
                                    _signupState.value = SignupState.Success(user)
                                    Log.d(TAG, "User created successfully: $userId")
                                } else {
                                    _signupState.value = SignupState.Error("Failed to save user data")
                                    Log.e(TAG, "Failed to save user data")
                                }
                            }
                        } else {
                            // Authentication failed
                            val exception = task.exception
                            _signupState.value = SignupState.Error(exception?.message ?: "Authentication failed")
                            Log.e(TAG, "Authentication failed", exception)
                        }
                    }
            } catch (e: Exception) {
                _signupState.value = SignupState.Error(e.message ?: "An unknown error occurred")
                Log.e(TAG, "Error registering user", e)
            }
        }
    }
}

// Sealed class to represent different states of the signup process
sealed class SignupState {
    object Loading : SignupState()
    data class Success(val user: UserModel) : SignupState()
    data class Error(val message: String) : SignupState()
}
