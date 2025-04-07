package com.it342.projectmanagementsystem.controller;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.it342.projectmanagementsystem.dto.AuthResponse;
import com.it342.projectmanagementsystem.dto.LoginRequest;
import com.it342.projectmanagementsystem.dto.RegisterRequest;
import com.it342.projectmanagementsystem.dto.RegisterResponse;
import com.it342.projectmanagementsystem.model.User;
import com.it342.projectmanagementsystem.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Firestore firestore;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                  JwtService jwtService,
                                  Firestore firestore,
                                  PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.firestore = firestore;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            var existingUsers = firestore.collection("users")
                    .whereEqualTo("email", request.getEmail())
                    .get()
                    .get()
                    .getDocuments();

            if (!existingUsers.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Create new user
            Map<String, Object> userData = new HashMap<>();
            userData.put("studId", request.getStudId());
            userData.put("firstName", request.getFirstName());
            userData.put("lastName", request.getLastName());
            userData.put("course", request.getCourse());
            userData.put("email", request.getEmail());
            userData.put("password", passwordEncoder.encode(request.getPassword()));
            userData.put("role", request.getRole());
            userData.put("createdAt", Timestamp.now());

            var docRef = firestore.collection("users").document();
            docRef.set(userData).get();

            return ResponseEntity.ok(new RegisterResponse(docRef.getId(), 
                    request.getEmail(), request.getRole()));
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            // First try to find user by email
            var usersByEmail = firestore.collection("users")
                    .whereEqualTo("email", request.getIdentifier())
                    .get()
                    .get()
                    .getDocuments();

            String email;
            if (!usersByEmail.isEmpty()) {
                email = request.getIdentifier();
            } else {
                // If not found by email, try to find by student ID
                var usersByStudId = firestore.collection("users")
                        .whereEqualTo("studId", request.getIdentifier())
                        .get()
                        .get()
                        .getDocuments();

                if (usersByStudId.isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                email = usersByStudId.get(0).getString("email");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), 
                    user.getEmail(), user.getRole()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 