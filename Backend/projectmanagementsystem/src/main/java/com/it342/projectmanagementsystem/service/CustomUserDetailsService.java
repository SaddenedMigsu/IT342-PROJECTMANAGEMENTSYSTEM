package com.it342.projectmanagementsystem.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.it342.projectmanagementsystem.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final Firestore firestore;

    public CustomUserDetailsService(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .get()
                    .getDocuments();

            if (documents.isEmpty()) {
                throw new UsernameNotFoundException("User not found with email: " + email);
            }

            QueryDocumentSnapshot document = documents.get(0);
            User user = new User();
            user.setUserId(document.getId());
            user.setStudId(document.getString("studId"));
            user.setFirstName(document.getString("firstName"));
            user.setLastName(document.getString("lastName"));
            user.setCourse(document.getString("course"));
            user.setEmail(document.getString("email"));
            user.setPassword(document.getString("password"));
            user.setRole(document.getString("role"));
            user.setCreatedAt(document.getTimestamp("createdAt"));
            user.setEnabled(true);

            return user;
        } catch (InterruptedException | ExecutionException e) {
            throw new UsernameNotFoundException("Error loading user with email: " + email, e);
        }
    }
} 