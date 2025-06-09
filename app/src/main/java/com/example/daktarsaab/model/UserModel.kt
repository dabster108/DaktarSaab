package com.example.daktarsaab.model

data class UserModel(
    var userId: String = "",
    var fullName: String = "",
    var email: String = "",
    var password: String = "" // Consider if you really need to store the password here if using Firebase Auth
) {
    // You can add additional methods or properties if needed
}
