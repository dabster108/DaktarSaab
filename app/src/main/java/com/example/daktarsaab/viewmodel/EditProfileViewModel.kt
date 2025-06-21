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
                // Get user data first
                val user = try {
                    userRepository.getUserByEmail(email)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get user data", e)
                    onError("Failed to verify user credentials")
                    return@launch
                }

                if (user == null) {
                    onError("User not found")
                    return@launch
                }

                if (user.password != currentPassword) {
                    onError("Current password is incorrect")
                    return@launch
                }

                // Handle image upload if needed
                var newProfileImageUrl = profileImageUrl
                if (selectedImageUri != null) {
                    try {
                        newProfileImageUrl = CloudinaryManager.uploadImage(context, selectedImageUri!!)
                        UserDataManager.updateProfileImage(newProfileImageUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to upload image to Cloudinary", e)
                        onError("Failed to upload profile image: ${e.message}")
                        return@launch
                    }
                }

                // Create updated user model
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

                // Update user in database
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
                Log.e(TAG, "Error during update process", e)
                onError(e.message ?: "An error occurred")
            }
        }
    }
}