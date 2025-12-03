package com.example.speakeasy

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PremiumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        val btnSubscribe = findViewById<Button>(R.id.btnSubscribe)
        btnSubscribe.setOnClickListener {
            // Mock subscription success
            Toast.makeText(this, "Welcome to SpeakEasy Pro! 🚀", Toast.LENGTH_LONG).show()
            finish() // Close the screen
        }
    }
}
