package com.it342.projectmanagementsystem.controller;

import com.it342.projectmanagementsystem.service.*;
import com.it342.projectmanagementsystem.dto.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
	// not used
	/*
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            String token = authService.registerUser(registerRequest.getEmail(), registerRequest.getPassword());
            return ResponseEntity.ok("User registered successfully. Firebase Token: " + token);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Registration Failed: " + e.getMessage());
        }
    }

    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.loginUser(loginRequest.getIdentifier(), loginRequest.getPassword());
            
            // Create response with more details
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful!");
            response.put("token", token);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Login Failed: " + e.getMessage());
        }
    }
    */
}