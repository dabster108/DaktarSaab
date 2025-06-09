package com.example.daktarsaab.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.daktarsaab.di.FirebaseModule
//import com.example.daktarsaab.repository.UserRepository
//import com.example.daktarsaab.repository.UserRepositoryImp
//import com.google.firebase.auth.AuthResult
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//class AuthViewModel : ViewModel() {
//
//    private val userRepository: UserRepository = UserRepositoryImp(
//        FirebaseModule.firebaseAuthInstance,
//        FirebaseModule.firebaseDatabaseInstance
//    )
//
//    private val _signupState = MutableStateFlow<Result<AuthResult>?>(null)
//    val signupState: StateFlow<Result<AuthResult>?> = _signupState.asStateFlow()
//
//    private val _loginState = MutableStateFlow<Result<AuthResult>?>(null)
//    val loginState: StateFlow<Result<AuthResult>?> = _loginState.asStateFlow()
//
//    private val _userDataSaveState = MutableStateFlow<Result<Boolean>?>(null)
//    val userDataSaveState: StateFlow<Result<Boolean>?> = _userDataSaveState.asStateFlow()
//
//    fun signupUser(email: String, password: String, fullName: String) {
//        viewModelScope.launch {
//            userRepository.signupUser(email, password, fullName).collect { result ->
//                _signupState.value = result
//                if (result.isSuccess) {
//                    val userId = result.getOrNull()?.user?.uid
//                    if (userId != null) {
//                        saveUserData(userId, fullName, email)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun saveUserData(userId: String, fullName: String, email: String) {
//        viewModelScope.launch {
//            userRepository.saveUserData(userId, fullName, email).collect { result ->
//                _userDataSaveState.value = result
//            }
//        }
//    }
//
//    fun loginUser(email: String, password: String) {
//        viewModelScope.launch {
//            userRepository.loginUser(email, password).collect { result ->
//                _loginState.value = result
//            }
//        }
//    }
//}
