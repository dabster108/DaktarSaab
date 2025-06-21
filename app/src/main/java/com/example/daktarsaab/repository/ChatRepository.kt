package com.example.daktarsaab.repository

import com.example.daktarsaab.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatCollection = db.collection("chatbotuser")

    suspend fun saveMessage(userId: String, message: ChatMessage) {
        chatCollection
            .document(userId)
            .collection("conversations")
            .document(message.conversationId)
            .collection("messages")
            .document()
            .set(message)
            .await()
    }

    suspend fun getConversations(userId: String): List<String> {
        return chatCollection
            .document(userId)
            .collection("conversations")
            .get()
            .await()
            .documents
            .map { it.id }
    }

    fun getMessagesForConversation(userId: String, conversationId: String): Query {
        return chatCollection
            .document(userId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    suspend fun clearConversation(userId: String, conversationId: String) {
        val messages = chatCollection
            .document(userId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .get()
            .await()

        messages.documents.forEach { doc ->
            doc.reference.delete().await()
        }
    }
}
