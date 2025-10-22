package com.rentals.eliterentals

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random
import retrofit2.Callback

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Elite Rentals"
        val message = remoteMessage.notification?.body ?: "You have a new notification"
        sendNotification(title, message)
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "elite_rentals_notifications"

        // Route based on role
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val role = prefs.getString("role", "Tenant")
        val intent = when (role) {
            "Caretaker" -> Intent(this, CaretakerTrackMaintenanceActivity::class.java)
            "PropertyManager" -> Intent(this, MainPmActivity::class.java)
            else -> Intent(this, TenantDashboardActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Elite Rentals Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(Random.nextInt(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token received: $token")
        SharedPrefs.saveFcmToken(this, token)
        sendTokenToServer(token)
    }


    private fun sendTokenToServer(token: String) {
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        val userId = prefs.getInt("userId", -1)
        val jwtToken = prefs.getString("jwt", null)

        Log.d("FCM", "Preparing to send token. userId=$userId, jwtToken=$jwtToken")

        if (userId != -1 && jwtToken != null) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            val request = FcmTokenRequest(token)

            api.updateFcmToken("Bearer $jwtToken", userId, request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        println("✅ FCM token updated via Retrofit")
                    } else {
                        println("❌ FCM token update failed: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    println("❌ Retrofit call failed: ${t.message}")
                }
            })
        }
    }



}
