package com.example.daktarsaab.repository
//
//import com.example.daktarsaab.model.UserModel
//import com.google.firebase.auth.AuthResult
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.FirebaseDatabase
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.tasks.await
//
//class UserRepositoryImp(
//    private val auth: FirebaseAuth,
//    private val database: FirebaseDatabase
//) : UserRepository {
//
//    override suspend fun signupUser(email: String, password: String, fullName: String): Flow<Result<AuthResult>> = flow {
//        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
//        emit(Result.success(authResult))
//    }.catch {
//        emit(Result.failure(it))
//    }
//
//    override suspend fun saveUserData(userId: String, fullName: String, email: String): Flow<Result<Boolean>> = flow {
//        val userModel = UserModel(userId, fullName, email)
//        database.getReference("users").child(userId).setValue(userModel).await()
//        emit(Result.success(true))
//    }.catch {
//        emit(Result.failure(it))
//    }
//
//    override suspend fun loginUser(email: String, password: String): Flow<Result<AuthResult>> = flow {
//        val authResult = auth.signInWithEmailAndPassword(email, password).await()
//        emit(Result.success(authResult))
//    }.catch {
//        emit(Result.failure(it))
//    }
//}
