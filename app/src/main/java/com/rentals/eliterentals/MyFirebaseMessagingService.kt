package com.rentals.eliterentals

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "📩 Push received: ${remoteMessage.data}")

        val notification = remoteMessage.notification
        val data = remoteMessage.data

        val title = notification?.title ?: data["title"] ?: "Elite Rentals"
        val message = notification?.body ?: data["body"] ?: "You have a new notification"
        val type = data["type"] ?: "default"

        // 🔹 Handle message based on type
        sendNotification(title, message, type, data)
    }

    private fun sendNotification(title: String, message: String, type: String, data: Map<String, String>) {
        val channelId = "elite_rentals_notifications"
        val notificationId = Random.nextInt()

        // 🔹 Choose destination activity
        val targetIntent = when (type) {
            "message" -> Intent(this, MessagesActivity::class.java)
            "announcement" -> Intent(this, AnnouncementsActivity::class.java)
            "rent_due" -> Intent(this, TenantDashboardActivity::class.java)
            "new_task" -> Intent(this, CaretakerTrackMaintenanceActivity::class.java)
            "escalation" -> Intent(this, MainPmActivity::class.java)
            else -> {
                val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
                when (prefs.getString("role", "Tenant")) {
                    "Caretaker" -> Intent(this, CaretakerTrackMaintenanceActivity::class.java)
                    "PropertyManager" -> Intent(this, MainPmActivity::class.java)
                    else -> Intent(this, TenantDashboardActivity::class.java)
                }
            }
        }

        // Pass message details
        for ((key, value) in data) {
            targetIntent.putExtra(key, value)
        }
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            targetIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🔹 Notification style
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        // 🔹 Create notification channel for Android O+
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Elite Rentals Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notifications for messages and announcements"
            }
            manager.createNotificationChannel(channel)
        }

        manager.notify(notificationId, builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔄 New FCM token: $token")
        SharedPrefs.saveFcmToken(this, token)
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val userId = prefs.getInt("userId", -1)
        val jwtToken = prefs.getString("jwt", null)

        if (userId == -1 || jwtToken.isNullOrBlank()) {
            Log.w("FCM", "⚠️ Skipping token sync — user not logged in or JWT missing")
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)
        val request = FcmTokenRequest(token)

        api.updateFcmToken("Bearer $jwtToken", userId, request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "✅ Token updated on server.")
                    } else {
                        Log.e("FCM", "❌ Token update failed: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("FCM", "❌ Retrofit call failed: ${t.message}")
                }
            })
    }
}
