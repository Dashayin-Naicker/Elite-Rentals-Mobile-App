package com.rentals.eliterentals

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class PropertyManagerMaintenanceActivity : AppCompatActivity() {

    private lateinit var adapter: PropertyManagerMaintenanceAdapter
    private lateinit var jwtToken: String
    private val api = RetrofitClient.instance
    private val maintenanceList = mutableListOf<Maintenance>()
    private val caretakersList = mutableListOf<UserDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pm_maintenance) // NEW layout


        val rv = findViewById<RecyclerView>(R.id.rvMaintenance)
        rv.layoutManager = LinearLayoutManager(this)

        jwtToken = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""
        if (jwtToken.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchCaretakersAndMaintenance(rv)
    }

    private fun fetchCaretakersAndMaintenance(rv: RecyclerView) {
        lifecycleScope.launch {
            try {
                // 1️⃣ Fetch all users (like MessagesActivity)
                val usersResponse = withContext(Dispatchers.IO) {
                    api.getAllUsers("Bearer $jwtToken") // fetch all users
                }

                caretakersList.clear()

                if (usersResponse.isSuccessful) {
                    usersResponse.body()?.let { list ->
                        // 1️⃣ Filter only caretakers safely and remove nulls
                        val filtered = list
                            .filterNotNull() // remove null UserDto
                            .filter { it.role.equals("Caretaker", ignoreCase = true) } // only caretakers

                        caretakersList.addAll(filtered)

                        // Log for debugging
                        caretakersList.forEach { Log.d("Caretakers", "${it.firstName} ${it.lastName} - ${it.role}") }
                    }
                }


                // 2️⃣ Fetch all maintenance requests
                val maintenanceResponse = withContext(Dispatchers.IO) {
                    api.getAllMaintenanceRequests("Bearer $jwtToken")
                }
                maintenanceList.clear()
                if (maintenanceResponse.isSuccessful) {
                    maintenanceResponse.body()?.let { maintenanceList.addAll(it) }
                } else {
                    Toast.makeText(this@PropertyManagerMaintenanceActivity, "Failed to load maintenance requests", Toast.LENGTH_SHORT).show()
                }

                // 3️⃣ Setup adapter after both lists are populated
                if (!::adapter.isInitialized) {
                    adapter = PropertyManagerMaintenanceAdapter(
                        maintenanceList,
                        caretakersList,
                        jwtToken,
                        onAssign = { mId, cId -> assignCaretaker(mId, cId) },
                        onStatusUpdate = { mId, status -> updateMaintenanceStatus(mId, status) }
                    )
                    rv.adapter = adapter
                } else {
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PropertyManagerMaintenanceActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun assignCaretaker(maintenanceId: Int, caretakerId: Int) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.assignCaretaker("Bearer $jwtToken", maintenanceId, AssignCaretakerDto(caretakerId))
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@PropertyManagerMaintenanceActivity, "Caretaker assigned", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PropertyManagerMaintenanceActivity, "Failed to assign caretaker", Toast.LENGTH_SHORT).show()
                }

            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMaintenanceStatus(maintenanceId: Int, status: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.updateMaintenanceStatus(
                        "Bearer $jwtToken",
                        maintenanceId,
                        MaintenanceStatusDto(status)
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@PropertyManagerMaintenanceActivity, "Status updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PropertyManagerMaintenanceActivity, "Failed to update status", Toast.LENGTH_SHORT).show()
                }

            } catch (e: HttpException) {
                e.printStackTrace()
            }
        }
    }
}
