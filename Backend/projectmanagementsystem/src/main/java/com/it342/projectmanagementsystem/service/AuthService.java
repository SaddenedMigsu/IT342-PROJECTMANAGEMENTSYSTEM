package com.it342.projectmanagementsystem.service;



import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class AuthService {

    private static final String FIREBASE_AUTH_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:";
    
    private static final String API_KEY = "AIzaSyDnsu-HOVnmAWYxGLiTpRSaUkYp1wr64t4"; // API Key

    public String registerUser(String email, String password) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = ImmutableMap.of(
                "email", email,
                "password", password,
                "returnSecureToken", true
        );

        Map<String, Object> response = restTemplate.postForObject(
                FIREBASE_AUTH_URL + "signUp?key=" + API_KEY, request, Map.class
        );

        if (response != null && response.containsKey("idToken")) {
            return response.get("idToken").toString(); // Return Firebase token
        } else {
            throw new RuntimeException("Registration failed");
        }
    }

    public String loginUser(String email, String password) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = ImmutableMap.of(
                "email", email,
                "password", password,
                "returnSecureToken", true
        );

        Map<String, Object> response = restTemplate.postForObject(
                FIREBASE_AUTH_URL + "signInWithPassword?key=" + API_KEY, request, Map.class
        );

        if (response != null && response.containsKey("idToken")) {
            return response.get("idToken").toString(); // Return Firebase token
        } else {
            throw new RuntimeException("Invalid login credentials");
        }
    }
}