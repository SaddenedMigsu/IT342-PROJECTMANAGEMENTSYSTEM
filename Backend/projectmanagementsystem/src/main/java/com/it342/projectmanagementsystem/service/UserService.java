package com.it342.projectmanagementsystem.service;

import com.it342.projectmanagementsystem.model.*;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
//import com.google.cloud.firestore.v1.FirestoreClient;
import com.google.firebase.auth.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    //  Delete User
    public void deleteUser(String userId) throws FirebaseAuthException {
        FirebaseAuth.getInstance().deleteUser(userId);
        firestore.collection("users").document(userId).delete();
    }
}