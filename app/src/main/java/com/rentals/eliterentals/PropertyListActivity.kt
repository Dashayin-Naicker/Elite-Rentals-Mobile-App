package com.rentals.eliterentals

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class PropertyListActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: PropertyAdapter
    private val api = RetrofitClient.instance
    private var jwt = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_list)

        rv = findViewById(R.id.rvProps)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = PropertyAdapter(mutableListOf(), ::onDeleteClicked, ::onEditClicked)
        rv.adapter = adapter

        jwt = getSharedPreferences("app", MODE_PRIVATE).getString("jwt", "") ?: ""

        fetchProps()
    }

    private fun fetchProps() {
        lifecycleScope.launch {
            try {
                val res = api.getAllProperties("Bearer $jwt") // pass bearer here
                if (res.isSuccessful) {
                    val properties = res.body() ?: emptyList() // safe fallback
                    adapter.submit(properties)
                } else {
                    Log.e("PropertyList", "Error ${res.code()}: ${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun onEditClicked(property: PropertyDto) {
        val intent = Intent(this, AddEditPropertyActivity::class.java)
        intent.putExtra("property", property)
        startActivity(intent)
    }

    private fun onDeleteClicked(property: PropertyDto) {
        lifecycleScope.launch {
            val res = api.deleteProperty("Bearer $jwt", property.propertyId)
            if (res.isSuccessful) fetchProps()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchProps()
    }
}
