package com.example.daktarsaab.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserDataManager {
    private val _userProfileImageUrl = MutableStateFlow<String>("")
    val userProfileImageUrl: StateFlow<String> = _userProfileImageUrl.asStateFlow()

    fun updateProfileImage(newImageUrl: String) {
        _userProfileImageUrl.value = newImageUrl
    }
}
