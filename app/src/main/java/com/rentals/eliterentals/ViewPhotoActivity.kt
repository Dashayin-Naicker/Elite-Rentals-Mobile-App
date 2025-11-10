package com.rentals.eliterentals

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ViewPhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_photo)

        val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)
        val imageUrl = intent.getStringExtra("imageUrl")

        if (!imageUrl.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(imageUrl)
                Glide.with(this).load(uri).into(ivPhoto)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No photo available", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }
}
