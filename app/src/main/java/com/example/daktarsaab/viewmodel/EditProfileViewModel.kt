package com.example.daktarsaab.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daktarsaab.model.UserModel
import com.example.daktarsaab.repository.UserRepository
import com.example.daktarsaab.repository.UserRepositoryImp
import com.example.daktarsaab.utils.CloudinaryManager
import com.example.daktarsaab.utils.UserDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {
    private val TAG = "EditProfileViewModel"

    // User data
    var userId: String = ""
    var email: String = ""
    var profileImageUrl: String = ""
    private var selectedImageUri: Uri? = null
    var initialFirstName: String = ""
    var initialLastName: String = ""

    private val _firstName = MutableStateFlow("")
    val firstName = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName = _lastName.asStateFlow()

    private val userRepository: UserRepository = UserRepositoryImp()

    fun onFirstNameChange(newName: String) {
        _firstName.value = newName
    }

    fun onLastNameChange(newName: String) {
        _lastName.value = newName
    }

    fun initialize(userId: String, email: String, profileImageUrl: String) {
        this.userId = userId
        this.email = email
        this.profileImageUrl = profileImageUrl
        // Update UserDataManager with current profile image
        UserDataManager.updateProfileImage(profileImageUrl)
        fetchUserData()
    }

    private fun fetchUserData() {
        if(userId.isEmpty()){
            return
        }
        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(userId)
                user?.let {
                    _firstName.value = it.firstName
                    _lastName.value = it.lastName
                    initialFirstName = it.firstName
                    initialLastName = it.lastName
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data", e)
            }
        }
    }

    fun setSelectedImageUri(uri: Uri) {
        selectedImageUri = uri
    }

    fun updateProfile(
        currentPassword: String,
        newFirstName: String,
        newLastName: String,
        newEmail: String,
        newPassword: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // First verify the current password using database authentication
                val user = userRepository.getUserByEmail(email)
                if (user == null) {
                    onError("User not found")
                    return@launch
                }

                if (user.password != currentPassword) {
                    onError("Current password is incorrect")
                    return@launch
                }

                // User is authenticated, proceed with updates
                try {
                    // 1. Upload new profile image if selected
                    var newProfileImageUrl = profileImageUrl
                    if (selectedImageUri != null) {
                        try {
                            newProfileImageUrl = CloudinaryManager.uploadImage(context, selectedImageUri!!)
                            // Update UserDataManager with new profile image URL
                            UserDataManager.updateProfileImage(newProfileImageUrl)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to upload image to Cloudinary", e)
                            onError("Failed to upload profile image: ${e.message}")
                            return@launch
                        }
                    }

                    // 2. Create updated user model
                    val updatedUser = UserModel(
                        userId = userId,
                        email = if (newEmail.isNotEmpty()) newEmail else email,
                        password = if (newPassword.isNotEmpty()) newPassword else currentPassword,
                        firstName = newFirstName,
                        lastName = newLastName,
                        imageUrl = newProfileImageUrl,
                        phoneNumber = user.phoneNumber,
                        active = user.active,
                        f = user.f
                    )

                    // 3. Update user in database
                    val success = userRepository.updateUser(updatedUser)
                    if (success) {
                        // Update local state
                        initialFirstName = newFirstName
                        initialLastName = newLastName
                        if (newEmail.isNotEmpty()) email = newEmail
                        if (newProfileImageUrl != profileImageUrl) profileImageUrl = newProfileImageUrl

                        onSuccess()
                    } else {
                        onError("Failed to update profile")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Database error: ${e.message}", e)
                    onError("Failed to update profile: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during update process", e)
                onError(e.message ?: "An error occurred")
            }
        }
    }
}