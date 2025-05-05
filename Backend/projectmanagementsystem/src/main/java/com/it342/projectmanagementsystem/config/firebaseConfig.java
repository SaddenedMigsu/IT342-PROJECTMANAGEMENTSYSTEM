package com.it342.projectmanagementsystem.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class firebaseConfig {

    private static Firestore firestoreInstance; // Store Firestore instance to prevent closing

    @Bean
    public Firestore firestore() throws IOException {
        if (firestoreInstance == null) {
            if (FirebaseApp.getApps().isEmpty()) {
                String firebaseConfig = System.getenv("FIREBASE_CONFIG");
                if (firebaseConfig == null) {
                    throw new IOException("FIREBASE_CONFIG environment variable not set!");
                }
                InputStream serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes(StandardCharsets.UTF_8));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
            }
            firestoreInstance = FirestoreClient.getFirestore();
        }
        return firestoreInstance; // Always return the same Firestore instance
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}