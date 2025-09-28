package com.rentals.eliterentals

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch

class PropertiesFragment : Fragment() {
    private lateinit var adapter: PropertyAdapter
    private val api = RetrofitClient.instance

    private var jwt: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_properties, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        jwt = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("jwt", "") ?: ""

        val recycler = view.findViewById<RecyclerView>(R.id.rvProps)
        val btnAdd = view.findViewById<Button>(R.id.btnAddProperty)

        adapter = PropertyAdapter(mutableListOf(),
            onDelete = { deleteProperty(it) },
            onEdit = { editProperty(it) }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditPropertyActivity::class.java))
        }

        loadProperties()
    }

    override fun onResume() {
        super.onResume()
        loadProperties()
    }

    private fun loadProperties() {
        lifecycleScope.launch {
            try {
                val res = api.getAllProperties("Bearer $jwt")
                if (res.isSuccessful) {
                    val props = res.body() ?: emptyList()
                    adapter.submit(props)
                } else {
                    Log.e("API_ERROR", "Code: ${res.code()} ${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Error fetching properties", e)
            }
        }

    }


    private fun deleteProperty(p: PropertyDto) {
        lifecycleScope.launch {
            val res = api.deleteProperty("Bearer $jwt", p.propertyId)
            if (res.isSuccessful) {
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                loadProperties()
            }
        }
    }

    private fun editProperty(p: PropertyDto) {
        val intent = Intent(requireContext(), AddEditPropertyActivity::class.java)
        intent.putExtra("property", p)
        startActivity(intent)
    }
}
