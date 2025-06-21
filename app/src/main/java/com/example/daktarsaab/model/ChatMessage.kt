package com.example.daktarsaab.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val role: String = "user", // "user" or "assistant"
    val isUser: Boolean = true,
    val timestamp: Timestamp = Timestamp.now(),
    val conversationId: String = ""
)
