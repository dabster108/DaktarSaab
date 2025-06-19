package com.example.daktarsaab.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.daktarsaab.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.black)

        // Immediately redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}