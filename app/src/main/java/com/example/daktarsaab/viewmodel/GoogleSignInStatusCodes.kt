package com.example.daktarsaab.viewmodel

/**
 * Common status codes for Google Sign-In operations
 * These match the constants from com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
 */
object GoogleSignInStatusCodes {
    const val SUCCESS = 0
    const val SIGN_IN_CANCELLED = 12501
    const val SIGN_IN_CURRENTLY_IN_PROGRESS = 12502
    const val SIGN_IN_FAILED = 12500
    const val NETWORK_ERROR = 7
    const val INVALID_ACCOUNT = 5
    const val SIGN_IN_REQUIRED = 4
    const val INTERNAL_ERROR = 8
    const val TIMEOUT = 15
    const val DEVELOPER_ERROR = 10
}
