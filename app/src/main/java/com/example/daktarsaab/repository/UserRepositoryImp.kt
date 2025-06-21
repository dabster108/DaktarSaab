package com.example.daktarsaab.repository

import com.example.daktarsaab.model.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class UserRepositoryImp : UserRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef: DatabaseReference = database.getReference("users")

    override suspend fun createUser(user: UserModel): Boolean {
        return try {
            val userRef = if (user.userId.isNotEmpty()) {
                usersRef.child(user.userId)
            } else {
                usersRef.push().apply {
                    user.userId = key ?: ""
                }
            }
            userRef.setValue(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUserById(userId: String): UserModel? {
        return suspendCoroutine { continuation ->
            usersRef.child(userId).get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(UserModel::class.java)
                continuation.resume(user)
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    override suspend fun getUserByEmail(email: String): UserModel? {
        return suspendCoroutine { continuation ->
            usersRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(UserModel::class.java)
                        if (user != null && user.email == email) {
                            continuation.resume(user)
                            return@addOnSuccessListener
                        }
                    }
                }
                continuation.resume(null)
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    override suspend fun updateUser(user: UserModel): Boolean {
        return try {
            usersRef.child(user.userId).setValue(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteUser(userId: String): Boolean {
        return try {
            usersRef.child(userId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllUsers(): Flow<List<UserModel>> = callbackFlow {
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull {
                    it.getValue(UserModel::class.java)
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.addValueEventListener(valueEventListener)
        awaitClose {
            usersRef.removeEventListener(valueEventListener)
        }
    }
}