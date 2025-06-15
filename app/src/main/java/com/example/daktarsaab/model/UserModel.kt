package com.example.daktarsaab.model

data class UserModel(
    var userId: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var phoneNumber: String = "",
    var active: String = "",
    var email: String = "",
    var password: String = "",  // Added password field
    val f: String = ""  // Required field as per your existing model
) {
    // You can add additional methods or properties if needed
}
