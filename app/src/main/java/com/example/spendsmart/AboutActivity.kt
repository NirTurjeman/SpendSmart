package com.example.spendsmart

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val appIcon = findViewById<ImageView>(R.id.app_icon)
        val appName = findViewById<TextView>(R.id.app_name)
        val appVersion = findViewById<TextView>(R.id.app_version)
        val appDescription = findViewById<TextView>(R.id.app_description)
        val featuresList = findViewById<TextView>(R.id.features_list)
        val developerNames = findViewById<TextView>(R.id.developer_names)
        val contactEmail = findViewById<TextView>(R.id.contact_email)
        val returnButton = findViewById<ImageView>(R.id.about_return_icon)
        // Set text for the views (if necessary, modify from XML or add dynamic values)
        appName.text = getString(R.string.app_name)
        appVersion.text = "Version 1.0.0" // You can replace this with a dynamically fetched version number if available
        appDescription.text = getString(R.string.app_description)
        featuresList.text = getString(R.string.features_list)
        developerNames.text = getString(R.string.developer_names)
        contactEmail.text = getString(R.string.contact_email)

        // Handle icon click to show a brief message
        appIcon.setOnClickListener {
            Toast.makeText(this, "SpendSmart - Smart way to manage your finances!", Toast.LENGTH_SHORT).show()
        }
        returnButton.setOnClickListener {
            val intent = Intent(this, DashBoardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
