package com.example.daktarsaab.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryManager {
    companion object {
        private const val TAG = "CloudinaryManager"
        private var isInitialized = false

        // Initialize Cloudinary with your cloud credentials
        fun init(context: Context) {
            if (!isInitialized) {
                try {
                    val config = mapOf(
                        "cloud_name" to "dr99twhg2",
                        "api_key" to "764794831832454",
                        "api_secret" to "nLLG7-iFUIJRAc0BIxQZnzzy30A",
                        "secure" to true
                    )
                    MediaManager.init(context, config)
                    isInitialized = true
                    Log.d(TAG, "Cloudinary initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing Cloudinary", e)
                    throw e
                }
            }
        }

        // Upload image to Cloudinary and return the URL
        suspend fun uploadImage(context: Context, imageUri: Uri): String = suspendCancellableCoroutine { continuation ->
            try {
                // Make sure Cloudinary is initialized
                if (!isInitialized) {
                    init(context)
                }

                val requestId = MediaManager.get().upload(imageUri)
                    .option("folder", "daktarsaab_users")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d(TAG, "Starting upload to Cloudinary")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100) / totalBytes
                            Log.d(TAG, "Upload progress: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                            val secureUrl = resultData?.get("secure_url") as? String
                            if (secureUrl != null) {
                                Log.d(TAG, "Upload successful: $secureUrl")
                                continuation.resume(secureUrl)
                            } else {
                                continuation.resumeWithException(Exception("Failed to get secure URL from Cloudinary"))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e(TAG, "Upload error: ${error.description}")
                            continuation.resumeWithException(Exception("Upload failed: ${error.description}"))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.d(TAG, "Upload rescheduled: ${error.description}")
                        }
                    }).dispatch()

                continuation.invokeOnCancellation {
                    MediaManager.get().cancelRequest(requestId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during upload", e)
                continuation.resumeWithException(e)
            }
        }
    }
}
