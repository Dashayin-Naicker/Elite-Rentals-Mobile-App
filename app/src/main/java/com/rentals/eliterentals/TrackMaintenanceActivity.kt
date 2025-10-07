package com.rentals.eliterentals

import android.os.Bundle
import android.widget.ImageView
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

        // Initially attach empty adapter to avoid "No adapter attached" error
        adapter = MaintenanceAdapter(emptyList())
        rv.adapter = adapter

        // Load saved JWT
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        jwtToken = prefs.getString("jwt", "") ?: ""

        if (jwtToken.isEmpty()) {
            Toast.makeText(this, "Not logged in. Please log in first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Load tenant maintenance requests
        fetchMaintenance(rv)

        // Back button navigation
        findViewById<ImageView>(R.id.ic_back).setOnClickListener { finish() }
    }

    private fun fetchMaintenance(rv: RecyclerView) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getMyRequests("Bearer $jwtToken")
                }

                if (response.isSuccessful) {
                    val requests = response.body() ?: emptyList()
                    adapter = MaintenanceAdapter(requests)
                    rv.adapter = adapter

                    if (requests.isEmpty()) {
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

}
