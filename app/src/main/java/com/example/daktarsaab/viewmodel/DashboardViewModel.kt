package com.example.daktarsaab.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daktarsaab.model.UserModel
import com.example.daktarsaab.repository.UserRepository
import com.example.daktarsaab.repository.UserRepositoryImp
import com.example.daktarsaab.utils.UserDataManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val TAG = "DashboardViewModel"

    private val userRepository: UserRepository = UserRepositoryImp()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val _userData = MutableLiveData<UserModel?>()
    val userData: LiveData<UserModel?> = _userData

    private val _userProfileImageUrl = MutableLiveData<String?>()
    val userProfileImageUrl: LiveData<String?> = _userProfileImageUrl

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Log.d(TAG, "Initializing DashboardViewModel - attempting to fetch current user data")
        fetchUserData() // Initial fetch with current auth user

        // Observe profile image changes from UserDataManager
        viewModelScope.launch {
            UserDataManager.userProfileImageUrl.collectLatest { newImageUrl ->
                _userProfileImageUrl.value = newImageUrl
            }
        }
    }

    fun fetchUserData(forcedUserId: String? = null) {
        val targetUid: String?
        if (forcedUserId != null) {
            Log.d(TAG, "fetchUserData called with forcedUserId: $forcedUserId")
            targetUid = forcedUserId
        } else {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "Error: User not authenticated and no forcedUserId provided.")
                _error.value = "User not authenticated"
                _isLoading.value = false
                return
            }
            targetUid = currentUser.uid
            Log.d(TAG, "fetchUserData using currentUser.uid: $targetUid, email: ${currentUser.email}")
        }

        if (targetUid == null) {
            Log.e(TAG, "Error: targetUid is null.")
            _error.value = "User ID not available for fetching data."
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        // Try fetching by UID first
        fetchUserByUid(targetUid)
    }

    private fun fetchUserByUid(uid: String) {
        Log.d(TAG, "Attempting to fetch user data by UID: $uid")
        viewModelScope.launch {
            val userRef = database.getReference("users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "User data found by UID: $uid")
                        processUserData(snapshot)
                    } else {
                        Log.d(TAG, "User data not found by UID ($uid), trying email lookup as fallback.")
                        auth.currentUser?.email?.let { email ->
                            if (auth.currentUser?.uid == uid) { // Only fallback to email if the original UID matches current user
                                fetchUserByEmail(email)
                            } else {
                                Log.w(TAG, "UID mismatch ($uid vs ${auth.currentUser?.uid}), not falling back to email for forced UID.")
                                _isLoading.value = false
                                _error.value = "User data not found for specified ID."
                            }
                        } ?: run {
                            _isLoading.value = false
                            _error.value = "User data not found and email not available for fallback."
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching user data by UID ($uid): ${error.message}")
                    _isLoading.value = false
                    _error.value = "Failed to fetch user data (UID): ${error.message}"
                    // Fallback to email if appropriate
                     auth.currentUser?.email?.let { email ->
                        if (auth.currentUser?.uid == uid) {
                            fetchUserByEmail(email)
                        } else {
                             Log.w(TAG, "UID mismatch on cancellation, not falling back to email for forced UID.")
                        }
                    }
                }
            })
        }
    }

    private fun fetchUserByEmail(email: String) {
        Log.d(TAG, "Attempting to fetch user data by email: $email")
        viewModelScope.launch {
            val usersRef = database.getReference("users")
            usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d(TAG, "Email query for '$email' - snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                        if (snapshot.exists()) {
                            // Assuming email is unique, take the first child
                            val userSnapshot = snapshot.children.firstOrNull()
                            if (userSnapshot != null) {
                                Log.d(TAG, "User data found by email: $email, snapshot key: ${userSnapshot.key}")
                                processUserData(userSnapshot)
                            } else {
                                Log.e(TAG, "User data not found by email ($email) despite snapshot existing (empty children).")
                                _isLoading.value = false
                                _error.value = "User data not found (email)."
                            }
                        } else {
                            Log.e(TAG, "User data not found by email ($email). Snapshot does not exist.")
                            _isLoading.value = false
                            _error.value = "User data not found (email)."
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error fetching user data by email ($email): ${error.message}")
                        _isLoading.value = false
                        _error.value = "Failed to fetch user data (email): ${error.message}"
                    }
                })
        }
    }

    private fun processUserData(snapshot: DataSnapshot) {
        try {
            val user = snapshot.getValue(UserModel::class.java)
            _userData.value = user
            val userMap = snapshot.value as? Map<*, *>
            Log.d(TAG, "Raw user data from Firebase for snapshot key ${snapshot.key}: $userMap")
            val imageUrlFromSnapshot = userMap?.get("imageUrl") as? String
            Log.d(TAG, "Image URL from snapshot directly: $imageUrlFromSnapshot")
            _userProfileImageUrl.value = imageUrlFromSnapshot // Prefer direct map access for imageUrl
            Log.d(TAG, "Processed user: ${user?.firstName} ${user?.lastName}, Final image URL set to: ${_userProfileImageUrl.value}")
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error processing user data for snapshot key ${snapshot.key}", e)
            _error.value = "Error processing user data: ${e.message}"
            _isLoading.value = false
        }
    }

    fun refreshUserData() {
        Log.d(TAG, "Manually refreshing user data using current auth user.")
        fetchUserData() // This will use auth.currentUser.uid by default
    }
}
