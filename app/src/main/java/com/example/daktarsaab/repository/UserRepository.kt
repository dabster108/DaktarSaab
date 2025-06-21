package com.example.daktarsaab.repository

import com.example.daktarsaab.model.UserModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun createUser(user: UserModel): Boolean
    suspend fun getUserById(userId: String): UserModel?
    suspend fun getUserByEmail(email: String): UserModel?
    suspend fun updateUser(user: UserModel): Boolean
    suspend fun deleteUser(userId: String): Boolean
    suspend fun getAllUsers(): Flow<List<UserModel>>
}