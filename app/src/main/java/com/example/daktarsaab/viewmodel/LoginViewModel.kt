package com.example.daktarsaab.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daktarsaab.model.UserModel
import com.example.daktarsaab.repository.UserRepository
import com.example.daktarsaab.repository.UserRepositoryImp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val TAG = "LoginViewModel"

    // Firebase authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // User repository for checking if user exists in database
    private val userRepository: UserRepository = UserRepositoryImp()

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

        // SIMPLIFIED APPROACH: ONLY check database, skip Firebase Auth completely
        viewModelScope.launch {
            try {
                // Check if this user exists in our database by email
                val user = userRepository.getUserByEmail(email)

                if (user != null) {
                    // User exists in database, check if password matches
                    if (user.password == password) {
                        // Database password matches! Login successful
                        _loginState.value = LoginState.Success(user)
                        Log.d(TAG, "Login successful using database credentials")

                        // Try to silently sign in to Firebase Auth in the background for
                        // features that might need Firebase Auth, but we don't care if it fails
                        try {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Firebase Auth sign-in successful after database auth")
                                }
                                .addOnFailureListener {
                                    // Ignore failures - we're using DB auth only
                                }
                        } catch (e: Exception) {
                            // Ignore completely - we don't care about Firebase Auth
                        }
                    } else {
                        // Password doesn't match the one in database
                        _loginState.value = LoginState.Error("Incorrect password")
                        Log.d(TAG, "Login failed: incorrect password")
                    }
                } else {
                    // User not found in database
                    _loginState.value = LoginState.Error("User not registered. Please sign up")
                    Log.d(TAG, "Login failed: user not found in database")
                }
            } catch (e: Exception) {
                // Error with database lookup
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
                Log.e(TAG, "Error during database authentication", e)
            }
        }
    }

    // Function to check if user is already logged in
    fun checkLoggedInUser() {
        // We're not auto-redirecting users anymore - always show login screen
        // Just clear any existing login state to ensure we're starting fresh
        _loginState.value = LoginState.Error("") // Empty error state instead of null

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