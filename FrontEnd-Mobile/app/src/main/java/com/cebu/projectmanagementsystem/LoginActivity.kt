package com.cebu.projectmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var btnFaculty: Button
    private lateinit var btnStudent: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        btnFaculty = findViewById(R.id.btnFaculty)
        btnStudent = findViewById(R.id.btnStudent)
        tvLogin = findViewById(R.id.tvLogin)

        // Set click listeners
        btnFaculty.setOnClickListener {
            // Navigate directly to Faculty registration
            startActivity(Intent(this, FacultyRegisterActivity::class.java))
        }

        btnStudent.setOnClickListener {
            // Navigate directly to Student registration
            startActivity(Intent(this, StudentRegisterActivity::class.java))
        }

        tvLogin.setOnClickListener {
            // Navigate to login form
            startActivity(Intent(this, LoginFormActivity::class.java))
        }
    }
} 