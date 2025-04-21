package com.it342.projectmanagementsystem.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.time.Instant;
import com.google.cloud.Timestamp;

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

    public List<Map<String, Object>> checkAppointmentConflicts(String startTime, String endTime, List<String> participants) throws Exception {
        Map<String, Map<String, Object>> uniqueConflicts = new HashMap<>();
        Instant proposedStart = Instant.parse(startTime);
        Instant proposedEnd = Instant.parse(endTime);

        // Convert to Firestore Timestamp for comparison
        Timestamp start = Timestamp.ofTimeSecondsAndNanos(proposedStart.getEpochSecond(), proposedStart.getNano());
        Timestamp end = Timestamp.ofTimeSecondsAndNanos(proposedEnd.getEpochSecond(), proposedEnd.getNano());

        // Assuming the second participant is always the faculty (based on the request-faculty endpoint)
        String facultyId = participants.get(1);

        // Get faculty's existing appointments
        var facultyAppointments = firestore.collection("user_appointments")
                .whereEqualTo("userId", facultyId)
                .get()
                .get()
                .getDocuments();

        // Check each of faculty's appointments for time conflicts
        for (var userAppointment : facultyAppointments) {
            String appointmentId = userAppointment.getString("appointmentId");
            var appointmentDoc = firestore.collection("appointments")
                    .document(appointmentId)
                    .get()
                    .get();

            if (appointmentDoc.exists()) {
                Timestamp existingStart = appointmentDoc.getTimestamp("startTime");
                Timestamp existingEnd = appointmentDoc.getTimestamp("endTime");
                String status = appointmentDoc.getString("status");

                // Check conflicts with confirmed or pending appointments
                if (("SCHEDULED".equals(status) || "PENDING_APPROVAL".equals(status)) &&
                    isTimeOverlapping(start, end, existingStart, existingEnd)) {
                    
                    // Create conflict entry
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("conflictingAppointmentId", appointmentId);
                    conflict.put("conflictingTitle", appointmentDoc.getString("title"));
                    conflict.put("conflictingStartTime", existingStart);
                    conflict.put("conflictingEndTime", existingEnd);
                    conflict.put("conflictingStatus", status);
                    
                    // Get the student who requested this conflicting appointment
                    String conflictingStudentId = appointmentDoc.getString("createdBy");
                    var studentDoc = firestore.collection("users").document(conflictingStudentId).get().get();
                    if (studentDoc.exists()) {
                        String studentName = studentDoc.getString("firstName") + " " + studentDoc.getString("lastName");
                        conflict.put("conflictingStudent", studentName);
                    }
                    
                    uniqueConflicts.put(appointmentId, conflict);
                }
            }
        }

        return new ArrayList<>(uniqueConflicts.values());
    }

    private boolean isTimeOverlapping(Timestamp start1, Timestamp end1, Timestamp start2, Timestamp end2) {
        // Check if one appointment starts during another appointment
        // or if one appointment completely contains another
        return (start1.compareTo(start2) >= 0 && start1.compareTo(end2) < 0) ||
               (end1.compareTo(start2) > 0 && end1.compareTo(end2) <= 0) ||
               (start1.compareTo(start2) <= 0 && end1.compareTo(end2) >= 0);
    }
} 