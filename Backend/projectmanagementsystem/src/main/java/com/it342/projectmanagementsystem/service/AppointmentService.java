package com.it342.projectmanagementsystem.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    private final Firestore firestore;

    public AppointmentService(Firestore firestore) {
        this.firestore = firestore;
    }

    public Map<String, Object> getAppointmentStats(String adminEmail) throws Exception {
        // Verify admin privileges
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

        // Get total appointments
        QuerySnapshot appointmentDocs = firestore.collection("appointments").get().get();
        int totalAppointments = appointmentDocs.size();

        // Get appointments by status
        QuerySnapshot pendingDocs = firestore.collection("appointments")
                .whereEqualTo("status", "PENDING_APPROVAL")
                .get().get();
        
        QuerySnapshot scheduledDocs = firestore.collection("appointments")
                .whereEqualTo("status", "SCHEDULED")
                .get().get();
        
        QuerySnapshot completedDocs = firestore.collection("appointments")
                .whereEqualTo("status", "COMPLETED")
                .get().get();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAppointments", totalAppointments);
        stats.put("pendingAppointments", pendingDocs.size());
        stats.put("confirmedAppointments", scheduledDocs.size());
        stats.put("completedAppointments", completedDocs.size());

        return stats;
    }
} 