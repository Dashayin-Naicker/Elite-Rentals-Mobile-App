package com.rentals.eliterentals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch


class TenantsFragment : Fragment() {
    private lateinit var adapter: TenantAdapter
    private val api = RetrofitClient.instance

    private var jwt: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tenants, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        jwt = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("jwt", "") ?: ""

        val recycler = view.findViewById<RecyclerView>(R.id.rvTenants)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = TenantAdapter(mutableListOf()) { approveTenant(it) }
        recycler.adapter = adapter

        loadTenants()
    }

    private fun loadTenants() {
        lifecycleScope.launch {
            val res = api.getAllUsers("Bearer $jwt")
            if (res.isSuccessful) {
                val tenants = res.body()?.filter { it.role == "Tenant" } ?: emptyList()
                adapter.submit(tenants)
            }
        }
    }

    private fun approveTenant(u: UserDto) {
        val update = UserUpdateDto("Approved")
        lifecycleScope.launch {
            val res = api.updateUser("Bearer $jwt", u.userId, update)
            if (res.isSuccessful) {
                Toast.makeText(requireContext(), "Tenant Approved", Toast.LENGTH_SHORT).show()
                loadTenants()
            }
        }
    }

}
