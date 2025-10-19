package com.rentals.eliterentals

import android.content.Context
import androidx.work.*

object SyncScheduler {

    fun scheduleSync(context: Context, jwt: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("jwt", jwt)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWorker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
