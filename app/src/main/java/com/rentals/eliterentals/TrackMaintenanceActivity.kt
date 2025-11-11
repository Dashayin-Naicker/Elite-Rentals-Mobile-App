package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class TrackMaintenanceActivity : AppCompatActivity() {

    private lateinit var adapter: MaintenanceAdapter
    private val api = RetrofitClient.instance
    private lateinit var jwtToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_maintenance)

        val rv = findViewById<RecyclerView>(R.id.rvMaintenance)
        rv.layoutManager = LinearLayoutManager(this)

        // Load saved JWT first
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        jwtToken = prefs.getString("jwt", "") ?: ""

        if (jwtToken.isEmpty()) {
            Toast.makeText(this, "Not logged in. Please log in first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Now attach the empty adapter safely
        adapter = MaintenanceAdapter(emptyList(), jwtToken)
        rv.adapter = adapter

        // Load tenant maintenance requests
        fetchMaintenance(rv)

        // Back button navigation
        findViewById<ImageView>(R.id.ic_back).setOnClickListener { finish() }

        // Bottom navigation
        setupBottomNav()
    }

    private fun fetchMaintenance(rv: RecyclerView) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getMyRequests("Bearer $jwtToken")
                }

                if (response.isSuccessful) {
                    val requests = response.body() ?: emptyList()

                    // ✅ Corrected image URL path
                    val mappedRequests = requests.map { m ->
                        m.copy(
                            imageUrl = "https://eliterentalsapi-czckh7fadmgbgtgf.southafricanorth-01.azurewebsites.net/api/maintenance/${m.maintenanceId}/proof"
                        )
                    }

                    adapter = MaintenanceAdapter(mappedRequests, jwtToken)
                    rv.adapter = adapter

                    rv.post {
                        for (i in 0 until rv.childCount) {
                            val viewHolder = rv.findViewHolderForAdapterPosition(i)
                            val itemView = viewHolder?.itemView ?: continue
                            val ivComment = itemView.findViewById<ImageView>(R.id.ivComment)
                            val maintenanceId = mappedRequests.getOrNull(i)?.maintenanceId ?: continue

                            ivComment.setOnClickListener {
                                val intent = Intent(this@TrackMaintenanceActivity, MessagesActivity::class.java)
                                intent.putExtra("maintenanceId", maintenanceId)
                                startActivity(intent)
                            }
                        }
                    }

                    if (mappedRequests.isEmpty()) {
                        Toast.makeText(
                            this@TrackMaintenanceActivity,
                            "No maintenance requests found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@TrackMaintenanceActivity,
                        "Failed to load requests: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@TrackMaintenanceActivity,
                    "Failed to load requests: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }


    private fun setupBottomNav() {
        // Dashboard
        findViewById<LinearLayout>(R.id.navDashboard).setOnClickListener {
            startActivity(Intent(this, TenantDashboardActivity::class.java))
            finish()
        }

        // Maintenance (current activity)
        findViewById<LinearLayout>(R.id.navMaintenance).setOnClickListener {
            Toast.makeText(this, "You are already in Maintenance", Toast.LENGTH_SHORT).show()
        }

        // Payments
        findViewById<LinearLayout>(R.id.navPayments).setOnClickListener {
            startActivity(Intent(this, UploadProofActivity::class.java))
            finish()
        }

        // Settings
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }
}
