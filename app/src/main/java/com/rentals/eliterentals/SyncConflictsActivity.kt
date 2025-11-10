package com.rentals.eliterentals

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SyncConflictsActivity : AppCompatActivity() {

    private lateinit var tvConflictInfo: TextView
    private lateinit var btnKeepLocal: Button
    private lateinit var btnKeepServer: Button

    private var conflictQueue = mutableListOf<ConflictItem>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_conflicts)

        tvConflictInfo = findViewById(R.id.tvConflictInfo)
        btnKeepLocal = findViewById(R.id.btnKeepLocal)
        btnKeepServer = findViewById(R.id.btnKeepServer)

        conflictQueue = intent.getParcelableArrayListExtra("conflictList") ?: mutableListOf()

        if (conflictQueue.isEmpty()) {
            finish() // Nothing to resolve
            return
        }

        showCurrentConflict()

        btnKeepLocal.setOnClickListener { resolveCurrentConflict(true) }
        btnKeepServer.setOnClickListener { resolveCurrentConflict(false) }
    }

    private fun showCurrentConflict() {
        val conflict = conflictQueue[currentIndex]
        tvConflictInfo.text = "Conflict detected for ${conflict.type} #${conflict.id} " +
                "(${currentIndex + 1}/${conflictQueue.size})"
    }

    private fun resolveCurrentConflict(keepLocal: Boolean) {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.offlineDao()
        val conflict = conflictQueue[currentIndex]

        lifecycleScope.launch {
            try {
                when (conflict.type.lowercase()) {
                    "maintenance" -> {
                        val req = dao.getRequestById(conflict.id)
                        if (req != null) {
                            if (keepLocal) {
                                req.syncStatus = "pending"
                                dao.updateRequest(req)
                            } else {
                                dao.deleteRequest(req)
                            }
                        }
                    }
                    "payment" -> {
                        val pay = dao.getPaymentById(conflict.id)
                        if (pay != null) {
                            if (keepLocal) {
                                pay.syncStatus = "pending"
                                dao.updatePayment(pay)
                            } else {
                                dao.deletePayment(pay)
                            }
                        }
                    }
                }

                currentIndex++
                if (currentIndex < conflictQueue.size) {
                    showCurrentConflict()
                } else {
                    Toast.makeText(
                        this@SyncConflictsActivity,
                        "All conflicts resolved",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@SyncConflictsActivity,
                    "Error resolving conflict",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
