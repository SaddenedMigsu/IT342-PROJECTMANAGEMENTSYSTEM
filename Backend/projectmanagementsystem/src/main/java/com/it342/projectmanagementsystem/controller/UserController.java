package com.it342.projectmanagementsystem.controller;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.cloud.Timestamp;

import com.it342.projectmanagementsystem.model.*;
import com.it342.projectmanagementsystem.service.*;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final Firestore firestore;

    public UserController(UserService userService, Firestore firestore) {
        this.userService = userService;
        this.firestore = firestore;
    }

    // Register User
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        try {
           
            if (!userData.containsKey("studId") || !userData.containsKey("firstName") ||!userData.containsKey("lastName") ||
            	!userData.containsKey("course") ||!userData.containsKey("email") ||!userData.containsKey("password") || !userData.containsKey("role")) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            String studId = userData.get("studId");
            String firstName = userData.get("firstName");
            String lastName = userData.get("lastName");
            String course = userData.get("course");
            String email = userData.get("email");
            String password = userData.get("password");
            String role = userData.get("role");

            // Call user service to register user
            String userId = userService.registerUser(studId, firstName, lastName, course, email,  password, role);

            // Success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully!");
            response.put("userId", userId);

            return ResponseEntity.ok(response);

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body("Firebase authentication failed: " + e.getMessage());

        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Firestore write error: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Registration failed: " + e.getMessage());
        }
    }


    // Login User
    @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
    try {
        if (loginData == null || !loginData.containsKey("identifier") || !loginData.containsKey("password")) {
            return ResponseEntity.badRequest().body("Username/Email and Password are required.");
        }

        String identifier = loginData.get("identifier");
        String password = loginData.get("password");

        // Find user by email or studID
        User user = userService.getUserByEmailOrUsername(identifier);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body("Invalid username/email or password.");
        }

        // Validate password
        boolean isPasswordValid = userService.validatePassword(user, password);
        if (!isPasswordValid) {
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body("Invalid username/email or password.");
        }

        // Generate authentication token
        String token = userService.generateToken(user);

        // Return success response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful!");
        response.put("token", token);
        response.put("userId", user.getUserId());
        response.put("studId", user.getStudId());
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("An error occurred during login: " + e.getMessage());
    }
}




    // Get User by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) throws ExecutionException, InterruptedException {
        User user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testGetEndpoint(){
    	return ResponseEntity.ok("Test Get Endpoint is WORKING!!!");
    }

    // Update User
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        userService.updateUser(id, updates);
        return ResponseEntity.ok("User updated successfully");
    }

    // Delete User (Admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            logger.info("Admin {} attempting to delete user: {}", adminEmail, id);

            userService.deleteUserAsAdmin(id, adminEmail);

            logger.info("User {} successfully deleted by admin {}", id, adminEmail);
            return ResponseEntity.ok("User deleted successfully");
        } catch (IllegalArgumentException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            logger.error("Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Deletion failed: " + e.getMessage());
        }
    }

    // Helper method to check if user is admin
    private boolean isAdmin(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (!userDocs.isEmpty()) {
                var userDoc = userDocs.iterator().next();
                return "ADMIN".equals(userDoc.getString("role"));
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking admin status: {}", e.getMessage());
            return false;
        }
    }

    // Get all users (Admin only)
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(Authentication authentication) {
        try {
            // Check if user is admin
            if (!isAdmin(authentication)) {
                logger.error("Unauthorized access attempt to get all users by user: {}", authentication.getName());
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }

            logger.info("Admin {} fetching all users", authentication.getName());

            // Get all users
            var userDocs = firestore.collection("users")
                    .get()
                    .get()
                    .getDocuments();

            List<Map<String, Object>> users = new ArrayList<>();
            for (var userDoc : userDocs) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", userDoc.getId());
                userData.put("firstName", userDoc.getString("firstName"));
                userData.put("lastName", userDoc.getString("lastName"));
                userData.put("email", userDoc.getString("email"));
                userData.put("role", userDoc.getString("role"));
                userData.put("createdAt", userDoc.getTimestamp("createdAt"));
                users.add(userData);
            }

            logger.info("Successfully retrieved {} users", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching all users: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/faculties")
    public ResponseEntity<List<Map<String, Object>>> getAllFaculties() {
        try {
            logger.info("Fetching all faculty members");

            // Query users collection for faculty members
            var facultyDocs = firestore.collection("users")
                    .whereEqualTo("role", "FACULTY")
                    .get()
                    .get()
                    .getDocuments();

            List<Map<String, Object>> faculties = new ArrayList<>();
            for (var facultyDoc : facultyDocs) {
                Map<String, Object> facultyData = new HashMap<>();
                facultyData.put("userId", facultyDoc.getId());
                facultyData.put("firstName", facultyDoc.getString("firstName"));
                facultyData.put("lastName", facultyDoc.getString("lastName"));
                facultyData.put("email", facultyDoc.getString("email"));
                facultyData.put("course", facultyDoc.getString("course"));
                facultyData.put("createdAt", facultyDoc.getTimestamp("createdAt"));
                faculties.add(facultyData);
            }

            logger.info("Successfully retrieved {} faculty members", faculties.size());
            return ResponseEntity.ok(faculties);
        } catch (Exception e) {
            logger.error("Error fetching faculty members: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> updates, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("User {} attempting to update profile", userEmail);

            Map<String, Object> updatedProfile = userService.updateUserProfile(userEmail, updates);
            logger.info("Profile updated successfully for: {}", userEmail);
            return ResponseEntity.ok(updatedProfile);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid update request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to update profile: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Fetching profile for user: {}", userEmail);

            Map<String, Object> profile = userService.getUserProfile(userEmail);
            logger.info("Successfully retrieved profile for user: {}", userEmail);
            return ResponseEntity.ok(profile);

        } catch (IllegalArgumentException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching profile: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to fetch profile: " + e.getMessage());
        }
    }

    // Update FCM Token
    @PostMapping("/fcm-token")
    public ResponseEntity<?> updateFcmToken(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            String fcmToken = request.get("fcmToken");

            if (fcmToken == null || fcmToken.isEmpty()) {
                return ResponseEntity.badRequest().body("FCM token is required");
            }

            // Update user's FCM token in Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", fcmToken);
            updates.put("updatedAt", Timestamp.now());

            firestore.collection("users").document(userId).update(updates).get();

            return ResponseEntity.ok().body("FCM token updated successfully");
        } catch (Exception e) {
            logger.error("Error updating FCM token: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
