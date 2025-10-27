package com.rentals.eliterentals

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    val syncedRequests = mutableListOf<Int>()
    val syncedPayments = mutableListOf<Int>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!NetworkUtils.isNetworkAvailable(applicationContext)) return@withContext Result.retry()

            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.offlineDao()
            val prefs = applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)
            val jwt = inputData.getString("jwt") ?: return@withContext Result.retry()


            if (jwt.isNullOrEmpty()) return@withContext Result.retry()
            val api = RetrofitClient.instance

            val conflictList = mutableListOf<ConflictItem>()

            // ==============================
            // 🔹 Sync Maintenance Requests
            // ==============================
            val pendingRequests = dao.getPendingRequests()
            for (req in pendingRequests) {
                try {
                    val tenantIdBody = req.tenantId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val propertyIdBody = req.propertyId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val descriptionBody = req.description.toRequestBody("text/plain".toMediaTypeOrNull())
                    val categoryBody = req.category.toRequestBody("text/plain".toMediaTypeOrNull())
                    val urgencyBody = req.urgency.toRequestBody("text/plain".toMediaTypeOrNull())

                    val proofPart = req.imageUri?.let { uriStr ->
                        val file = getFileFromUri(applicationContext, Uri.parse(uriStr))
                        file?.let {
                            val mimeType = applicationContext.contentResolver.getType(Uri.parse(uriStr))
                            MultipartBody.Part.createFormData("proof", file.name, file.asRequestBody(mimeType?.toMediaTypeOrNull()))
                        }
                    }

                    val response = api.createMaintenance(
                        token = "Bearer $jwt",
                        tenantId = tenantIdBody,
                        propertyId = propertyIdBody,
                        description = descriptionBody,
                        category = categoryBody,
                        urgency = urgencyBody,
                        proof = proofPart
                    )

                    when {
                        response.isSuccessful -> {
                            req.syncStatus = "synced"
                            dao.updateRequest(req)
                            syncedRequests.add(req.id) // track success
                        }
                        response.code() == 409 -> { /* conflict handling */ }
                        else -> { req.syncStatus = "failed"; dao.updateRequest(req) }
                    }
                    dao.updateRequest(req)

                } catch (e: Exception) {
                    e.printStackTrace()
                    req.syncStatus = "failed"
                    dao.updateRequest(req)
                }
            }

            // ==============================
            // 🔹 Sync Payments
            // ==============================
            val pendingPayments = dao.getPendingPayments()
            for (pay in pendingPayments) {
                try {
                    val tenantIdBody = pay.tenantId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val amountBody = pay.amount.toRequestBody("text/plain".toMediaTypeOrNull())
                    val dateBody = pay.dateIso.toRequestBody("text/plain".toMediaTypeOrNull())

                    val proofPart = pay.fileUri?.let { uriStr ->
                        val file = getFileFromUri(applicationContext, Uri.parse(uriStr))
                        file?.let {
                            val mimeType = applicationContext.contentResolver.getType(Uri.parse(uriStr))
                            MultipartBody.Part.createFormData("proof", pay.fileName ?: file.name, file.asRequestBody(mimeType?.toMediaTypeOrNull()))
                        }
                    }

                    val response = api.createPayment(
                        bearer = "Bearer $jwt",
                        tenantId = tenantIdBody,
                        amount = amountBody,
                        date = dateBody,
                        proof = proofPart
                    )

                    when {
                        response.isSuccessful -> {
                            pay.syncStatus = "synced"
                            dao.updatePayment(pay)
                            syncedPayments.add(pay.id) // track success
                        }
                        response.code() == 409 -> { /* conflict handling */ }
                        else -> { pay.syncStatus = "failed"; dao.updatePayment(pay) }
                    }
                    dao.updatePayment(pay)

                } catch (e: Exception) {
                    e.printStackTrace()
                    pay.syncStatus = "failed"
                    dao.updatePayment(pay)
                }
            }

            // ==============================
            // 🔹 Show Notification for Conflicts
            // ==============================
            if (conflictList.isNotEmpty()) {
                showConflictNotification(conflictList)
            }

            if (syncedRequests.isNotEmpty() || syncedPayments.isNotEmpty()) {
                showSyncSummaryNotification(syncedRequests, syncedPayments)
            }

            // Clear synced entries
            dao.clearSyncedRequests()
            dao.clearSyncedPayments()

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}")
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showSyncSuccessNotification(type: String, id: Int) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = applicationContext.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }

        val channelId = "sync_success_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Sync Success",
                    NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("$type Synced")
            .setContentText("$type #$id has been successfully submitted")
            .setSmallIcon(R.drawable.ic_sync_conflict) // add a suitable drawable
            .setAutoCancel(true)
            .build()

        nm.notify((System.currentTimeMillis() % 10000).toInt(), notification) // unique ID
    }

    private fun showSyncSummaryNotification(syncedRequests: List<Int>, syncedPayments: List<Int>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "sync_summary_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, "Sync Summary", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        val summaryText = buildString {
            if (syncedRequests.isNotEmpty()) append("✅ Maintenance requests: ${syncedRequests.size}\n")
            if (syncedPayments.isNotEmpty()) append("✅ Payments: ${syncedPayments.size}")
        }.trim()

        if (summaryText.isEmpty()) return

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Offline Sync Complete")
            .setContentText("Your pending requests and payments were synced")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setSmallIcon(R.drawable.ic_sync_conflict)
            .setAutoCancel(true)
            .build()

        nm.notify(0, notification)
    }


    private fun showConflictNotification(conflicts: List<ConflictItem>) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = applicationContext.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return // skip notification if no permission
        }

        val channelId = "sync_conflicts_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Sync Conflicts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(channel)
            }
        }

        // Create intent to launch conflict resolution activity
        val intent = Intent(applicationContext, SyncConflictsActivity::class.java).apply {
            putExtra("conflictList", ArrayList(conflicts))
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build and show notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Sync Conflicts Detected")
            .setContentText("${conflicts.size} item(s) require resolution")
            .setSmallIcon(R.drawable.ic_sync_conflict) // make sure this drawable exists
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(1, notification)
    }

}
