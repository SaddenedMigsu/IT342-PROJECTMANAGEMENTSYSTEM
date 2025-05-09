package com.it342.projectmanagementsystem.controller;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.it342.projectmanagementsystem.dto.AuthResponse;
import com.it342.projectmanagementsystem.dto.LoginRequest;
import com.it342.projectmanagementsystem.dto.RegisterRequest;
import com.it342.projectmanagementsystem.dto.RegisterResponse;
import com.it342.projectmanagementsystem.model.User;
import com.it342.projectmanagementsystem.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
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
                logger.info("User found by email: {}", email);
            } else {
                // If not found by email, try to find by student ID
                var usersByStudId = firestore.collection("users")
                        .whereEqualTo("studId", request.getIdentifier())
                        .get()
                        .get()
                        .getDocuments();

                if (usersByStudId.isEmpty()) {
                    logger.error("Login failed: No user found with identifier {}", request.getIdentifier());
                    return ResponseEntity.badRequest().build();
                }
                email = usersByStudId.get(0).getString("email");
                logger.info("User found by student ID: {} (email: {})", request.getIdentifier(), email);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            logger.info("Successful login - User Details:");
            logger.info("User ID: {}", user.getUserId());
            logger.info("Email: {}", user.getEmail());
            logger.info("Role: {}", user.getRole());
            logger.info("Student ID: {}", user.getStudId());
            logger.info("Name: {} {}", user.getFirstName(), user.getLastName());
            logger.info("Course: {}", user.getCourse());
            logger.info("Token generated successfully");

            return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName()
            ));
        } catch (Exception e) {
            logger.error("Login failed for identifier: {}. Error: {}", request.getIdentifier(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Endpoint to register or update FCM token for a user
     * This is used by mobile devices to register for push notifications
     */
    @PostMapping("/fcm-token")
    public ResponseEntity<Map<String, Object>> updateFcmToken(
            @RequestBody Map<String, String> tokenRequest,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String fcmToken = tokenRequest.get("fcmToken");
            
            logger.info("Updating FCM token for user: {}", userEmail);
            
            if (fcmToken == null || fcmToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "FCM token is required"
                ));
            }
            
            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();
            
            // Store or update FCM token in user's devices collection
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("fcmToken", fcmToken);
            deviceData.put("updatedAt", Timestamp.now());
            deviceData.put("platform", "ANDROID"); // Assuming Android for now, could be passed in request
            
            // Use token as document ID to avoid duplicates from same device
            firestore.collection("users")
                    .document(userId)
                    .collection("devices")
                    .document(fcmToken) // Using token as document ID
                    .set(deviceData)
                    .get();
            
            // Also update user document with latest token for quick access
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", fcmToken);
            updates.put("lastTokenUpdate", Timestamp.now());
            
            userDoc.getReference().update(updates).get();
            
            logger.info("Successfully updated FCM token for user {}", userEmail);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "FCM token updated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error updating FCM token: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error updating FCM token: " + e.getMessage()
            ));
        }
    }
} 