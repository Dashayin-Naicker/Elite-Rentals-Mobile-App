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

            val jwt  = inputData.getString("jwt")
            val chat = inputData.getString("chatbot_text")
            val clang = inputData.getString("chatbot_lang")
                ?: applicationContext
                    .getSharedPreferences("app", Context.MODE_PRIVATE)
                    .getString("language", "en")
                ?: "en"

            // If this job was queued just to send a chatbot message, do it and exit.
            if (!jwt.isNullOrEmpty() && !chat.isNullOrEmpty()) {
                val svc = RetrofitClient.instance
                return@withContext try {
                    svc.sendChatbotMessage(
                        bearer = "Bearer $jwt",
                        body = ApiService.ChatbotMessageCreate(
                            messageText = chat,
                            isChatbot = true,
                            language = clang
                        )
                    )
                    Result.success()
                } catch (e: Exception) {
                    Result.retry()
                }
            }

            // If not a chatbot-only run and we don't have a JWT, retry later.
            if (jwt.isNullOrEmpty()) return@withContext Result.retry()

            // ===== Below is your existing sync logic =====
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.offlineDao()
            val api = RetrofitClient.instance
            val conflictList = mutableListOf<ConflictItem>()

            // --- Maintenance ---
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
                        bearer = "Bearer $jwt",
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
                            syncedRequests.add(req.id)
                        }
                        response.code() == 409 -> { /* conflict handling if needed */ }
                        else -> { req.syncStatus = "failed"; dao.updateRequest(req) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    req.syncStatus = "failed"
                    dao.updateRequest(req)
                }
            }

            // --- Payments ---
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
                            syncedPayments.add(pay.id)
                        }
                        response.code() == 409 -> { /* conflict handling if needed */ }
                        else -> { pay.syncStatus = "failed"; dao.updatePayment(pay) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    pay.syncStatus = "failed"
                    dao.updatePayment(pay)
                }
            }

            if (syncedRequests.isNotEmpty() || syncedPayments.isNotEmpty()) {
                showSyncSummaryNotification(syncedRequests, syncedPayments)
            }

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
            inputStream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun showSyncSummaryNotification(syncedRequests: List<Int>, syncedPayments: List<Int>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "sync_summary_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Sync Summary", NotificationManager.IMPORTANCE_HIGH))
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "sync_conflicts_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Sync Conflicts", NotificationManager.IMPORTANCE_HIGH))
        }

        val intent = Intent(applicationContext, SyncConflictsActivity::class.java)
            .putExtra("conflictList", ArrayList(conflicts))

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Sync Conflicts Detected")
            .setContentText("${conflicts.size} item(s) require resolution")
            .setSmallIcon(R.drawable.ic_sync_conflict)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(1, notification)
    }
}
