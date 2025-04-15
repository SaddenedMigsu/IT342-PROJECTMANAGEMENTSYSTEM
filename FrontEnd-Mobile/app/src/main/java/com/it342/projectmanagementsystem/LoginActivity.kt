package com.it342.projectmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.it342.projectmanagementsystem.activities.RegisterActivity
import com.it342.projectmanagementsystem.activities.LoginFormActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var btnCreateAccount: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        tvLogin = findViewById(R.id.tvLogin)

        // Set click listeners
        btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvLogin.setOnClickListener {
            // Navigate to login form
            startActivity(Intent(this, LoginFormActivity::class.java))
        }
    }
} 