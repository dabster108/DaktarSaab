package com.example.daktarsaab

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager // Import for default notification sound
import android.net.Uri // Import for sound URI
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        // NOTIFICATION_ID is not strictly needed here as we use reminderId.hashCode() for notifications
        // const val NOTIFICATION_ID = "reminder_notification_id"
        const val NOTIFICATION_CHANNEL_ID = "reminder_channel"
        const val EXTRA_REMINDER_TEXT = "extra_reminder_text"
        const val EXTRA_REMINDER_ID = "extra_reminder_id" // Used to make PendingIntent unique
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderText = intent.getStringExtra(EXTRA_REMINDER_TEXT) ?: "Reminder"
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: System.currentTimeMillis().toString()

        // Debug: Show a Toast to confirm broadcast is received
        android.widget.Toast.makeText(context, "Reminder Triggered: $reminderText", android.widget.Toast.LENGTH_LONG).show()
        println("ReminderBroadcastReceiver: onReceive called for: $reminderText")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android Oreo and above
        // The check for SDK_INT >= O is implicitly handled by NotificationChannel constructor availability
        // and the system ignoring it on older versions.
        // However, explicit check is good practice if targeting pre-O with different logic.
        // For this case, as minSdk is likely >= 26 (Oreo), this check might be flagged as unnecessary by linters.
        // If minSdk is indeed >=26, the if condition can be removed. Assuming it might not be for broader compatibility.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Reminder Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                description = "Channel for reminder notifications"
                // Set sound for the channel - this is important for Android O+
                val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, null) // null for default attributes
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app when notification is tapped
        val contentIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // Ensure each PendingIntent is unique by using the reminderId (or a unique request code)
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(), // Unique request code
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setContentTitle("Reminder")
            .setContentText(reminderText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Set sound for pre-Oreo devices (Oreo and above use channel sound)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        // Use a unique ID for each notification if you want to show multiple,
        // or a fixed one if you want to update the same notification.
        // Using reminderId.hashCode() ensures each reminder has its own notification.
        notificationManager.notify(reminderId.hashCode(), notification)
        println("ReminderBroadcastReceiver: Displaying notification for: $reminderText")
    }
}

