package com.it342.projectmanagementsystem.service;

import com.it342.projectmanagementsystem.model.*;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
//import com.google.cloud.firestore.v1.FirestoreClient;
import com.google.firebase.auth.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

	private final Firestore firestore;
    private final FirebaseAuth firebaseAuth;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(Firestore firestore, FirebaseAuth firebaseAuth, PasswordEncoder passwordEncoder) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
        this.passwordEncoder = passwordEncoder;
    }
    
   
    public String registerUser(String studId, String firstName, String lastName, String course, String email, String password, String role) throws Exception {
        if (firestore == null) {
            throw new IllegalStateException("Firestore instance is not available!");
        }

        //Create user in Firebase Authentication
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(studId);

        UserRecord userRecord = firebaseAuth.createUser(request);
        String userId = userRecord.getUid();

        // Store user in Firestore Database
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("studId", studId);
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("course", course);
        userMap.put("email", email);
        String encryptedPassword = passwordEncoder.encode(password);
        userMap.put("password", encryptedPassword);
        userMap.put("role", role);
        userMap.put("createdAt", com.google.cloud.firestore.FieldValue.serverTimestamp());
        userMap.put("enabled", true);  // Set enabled to true by default for new users

        try {
            firestore.collection("users").document(userId).set(userMap).get(); // Forces synchronous write
            System.out.println("User successfully written to Firestore: " + userMap);
        } catch (Exception e) {
            System.err.println("Firestore write failed: " + e.getMessage());
            throw new RuntimeException("Error writing user to Firestore", e);
        }

        return userId;
    }
    public User getUserByEmailOrUsername(String identifier) {
        // Use the injected Firestore instance
        CollectionReference users = firestore.collection("users");

        try {
            // Try fetching user by email
            Query query = users.whereEqualTo("email", identifier).limit(1);
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            
            if (!documents.isEmpty()) {
                return documents.get(0).toObject(User.class);
            }

            // If not found by email, search by username
            query = users.whereEqualTo("username", identifier).limit(1);
            future = query.get();
            documents = future.get().getDocuments();
            
            if (!documents.isEmpty()) {
                return documents.get(0).toObject(User.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // User not found
    }


    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword()); // Replace with hashed password validation
    }

    // Dummy token generator (replace with JWT implementation)
    public String generateToken(User user) {
        return UUID.randomUUID().toString(); // Replace with JWT token generation
    }
    
    public String loginUser(String identifier, String password) {
        User user = getUserByEmailOrUsername(identifier);

        if (user == null) {
            return "User not found!";
        }

        if (!validatePassword(user, password)) {
            return "Invalid password!";
        }

        // Generate token upon successful authentication
        return generateToken(user); 
    }


    // Authenticate User (Login)
    public User authenticateUser(String email) throws ExecutionException, InterruptedException {
        CollectionReference usersRef = firestore.collection("users");
        Query query = usersRef.whereEqualTo("email", email).limit(1);
        ApiFuture<QuerySnapshot> future = query.get();

        QuerySnapshot snapshot = future.get();
        if (!snapshot.isEmpty()) {
            DocumentSnapshot document = snapshot.getDocuments().get(0);
            return document.toObject(User.class);
        }

        return null; // User not found
    }

    //  Get User by ID
    public User getUserById(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection("users").document(userId);
        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            return document.toObject(User.class);
        }
        return null;
    }
    

    //  Update User
    public void updateUser(String userId, Map<String, Object> updates) {
        firestore.collection("users").document(userId).update(updates);
    }

    //  Delete User (Admin only)
    public void deleteUserAsAdmin(String userId, String adminEmail) throws Exception {
        // Check if user exists in Firestore
        DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();
        if (!userDoc.exists()) {
            throw new IllegalArgumentException("User not found in Firestore: " + userId);
        }

        // Check if admin exists and has admin role
        var adminDocs = firestore.collection("users")
                .whereEqualTo("email", adminEmail)
                .get()
                .get()
                .getDocuments();
        
        if (adminDocs.isEmpty()) {
            throw new SecurityException("Admin user not found");
        }

        var adminDoc = adminDocs.iterator().next();
        if (!"ADMIN".equals(adminDoc.getString("role"))) {
            throw new SecurityException("User does not have admin privileges");
        }

        try {
            // First try to delete from Firebase Auth
            firebaseAuth.deleteUser(userId);
        } catch (FirebaseAuthException e) {
            // If user doesn't exist in Firebase Auth, log it but continue with Firestore deletion
            System.out.println("User not found in Firebase Auth, continuing with Firestore deletion: " + e.getMessage());
        }

        // Then delete from Firestore
        firestore.collection("users").document(userId).delete().get();
    }

    public Map<String, Object> updateUserProfile(String userEmail, Map<String, Object> updates) throws Exception {
        // Get user document
        var userDocs = firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .get()
                .getDocuments();

        if (userDocs.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        var userDoc = userDocs.iterator().next();
        String userId = userDoc.getId();

        // Validate and prepare updates
        Map<String, Object> validUpdates = new HashMap<>();

        if (updates.containsKey("firstName")) {
            validUpdates.put("firstName", updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            validUpdates.put("lastName", updates.get("lastName"));
        }
        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            // Only process email update if it's different from current email
            if (!newEmail.equals(userEmail)) {
                // Check if email is already in use by another user
                var existingUserDocs = firestore.collection("users")
                        .whereEqualTo("email", newEmail)
                        .get()
                        .get()
                        .getDocuments();
                
                if (!existingUserDocs.isEmpty() && !existingUserDocs.iterator().next().getId().equals(userId)) {
                    throw new IllegalArgumentException("Email is already in use");
                }
                validUpdates.put("email", newEmail);

                // Try to update email in Firebase Auth
                try {
                    // First try to get user by email
                    UserRecord userRecord = null;
                    try {
                        userRecord = firebaseAuth.getUserByEmail(userEmail);
                    } catch (FirebaseAuthException e) {
                        // If user not found by email, try by UID
                        try {
                            userRecord = firebaseAuth.getUser(userId);
                        } catch (FirebaseAuthException ex) {
                            // If user doesn't exist in Firebase Auth, create one
                            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                                .setUid(userId)
                                .setEmail(newEmail)
                                .setEmailVerified(false)
                                .setDisplayName(userDoc.getString("firstName") + " " + userDoc.getString("lastName"));
                            userRecord = firebaseAuth.createUser(createRequest);
                        }
                    }

                    // Update the email if user exists
                    if (userRecord != null) {
                        UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(userRecord.getUid())
                            .setEmail(newEmail);
                        firebaseAuth.updateUser(updateRequest);
                    }
                } catch (FirebaseAuthException e) {
                    // Log the error but don't stop the Firestore update
                    System.err.println("Warning: Failed to update email in Firebase Auth: " + e.getMessage());
                }
            }
        }
        if (updates.containsKey("phoneNumber")) {
            validUpdates.put("phoneNumber", updates.get("phoneNumber"));
        }
        if (updates.containsKey("profilePicture")) {
            validUpdates.put("profilePicture", updates.get("profilePicture"));
        }

        if (validUpdates.isEmpty()) {
            throw new IllegalArgumentException("No valid fields to update");
        }

        // Update the user document
        firestore.collection("users").document(userId).update(validUpdates).get();

        // Get and return updated user data
        var updatedUserDoc = firestore.collection("users").document(userId).get().get();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", updatedUserDoc.getId());
        responseData.put("firstName", updatedUserDoc.getString("firstName"));
        responseData.put("lastName", updatedUserDoc.getString("lastName"));
        responseData.put("email", updatedUserDoc.getString("email"));
        responseData.put("phoneNumber", updatedUserDoc.getString("phoneNumber"));
        responseData.put("profilePicture", updatedUserDoc.getString("profilePicture"));
        responseData.put("role", updatedUserDoc.getString("role"));

        return responseData;
    }

    public Map<String, Object> getUserProfile(String userEmail) throws Exception {
        // Get user document
        var userDocs = firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .get()
                .getDocuments();

        if (userDocs.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        var userDoc = userDocs.iterator().next();
        
        // Create response with user profile data
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", userDoc.getId());
        profileData.put("firstName", userDoc.getString("firstName"));
        profileData.put("lastName", userDoc.getString("lastName"));
        profileData.put("email", userDoc.getString("email"));
        profileData.put("phoneNumber", userDoc.getString("phoneNumber"));
        profileData.put("profilePicture", userDoc.getString("profilePicture"));
        profileData.put("role", userDoc.getString("role"));

        return profileData;
    }

    public long getTotalActiveUsers() throws ExecutionException, InterruptedException {
        // Get all users from Firestore
        var userDocs = firestore.collection("users")
                .get()
                .get()
                .getDocuments();
        
        // Count all users (they are considered active by default)
        return userDocs.size();
    }

    public List<Map<String, Object>> getAllStudents() throws ExecutionException, InterruptedException {
        // Get all users with STUDENT role from Firestore
        var userDocs = firestore.collection("users")
                .whereEqualTo("role", "STUDENT")
                .get()
                .get()
                .getDocuments();

        List<Map<String, Object>> students = new ArrayList<>();
        for (var userDoc : userDocs) {
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("userId", userDoc.getId());
            studentData.put("studId", userDoc.getString("studId"));
            studentData.put("firstName", userDoc.getString("firstName"));
            studentData.put("lastName", userDoc.getString("lastName"));
            studentData.put("email", userDoc.getString("email"));
            studentData.put("course", userDoc.getString("course"));
            students.add(studentData);
        }

        return students;
    }
}