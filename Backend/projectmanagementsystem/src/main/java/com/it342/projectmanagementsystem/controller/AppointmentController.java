package com.it342.projectmanagementsystem.controller;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.it342.projectmanagementsystem.dto.*;
import com.it342.projectmanagementsystem.model.Appointment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final Firestore firestore;

    public AppointmentController(Firestore firestore) {
        this.firestore = firestore;
    }

    // 1. Create and Edit Appointment
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(
            @RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();

            // Validate time format
            Instant startTime = Instant.parse(request.getStartTime());
            Instant endTime = Instant.parse(request.getEndTime());

            // Create appointment data
            Map<String, Object> appointmentData = new HashMap<>();
            appointmentData.put("title", request.getTitle());
            appointmentData.put("description", request.getDescription());
            appointmentData.put("startTime", Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(), startTime.getNano()));
            appointmentData.put("endTime", Timestamp.ofTimeSecondsAndNanos(
                    endTime.getEpochSecond(), endTime.getNano()));
            appointmentData.put("createdBy", userId);
            appointmentData.put("participants", request.getParticipants());
            appointmentData.put("status", "SCHEDULED");
            appointmentData.put("createdAt", Timestamp.now());
            appointmentData.put("updatedAt", Timestamp.now());

            // Save to Firestore
            var docRef = firestore.collection("appointments").document();
            docRef.set(appointmentData).get();

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(docRef.getId());
            appointment.setTitle(request.getTitle());
            appointment.setDescription(request.getDescription());
            appointment.setStartTime(Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(), startTime.getNano()));
            appointment.setEndTime(Timestamp.ofTimeSecondsAndNanos(
                    endTime.getEpochSecond(), endTime.getNano()));
            appointment.setCreatedBy(userId);
            appointment.setParticipants(request.getParticipants());
            appointment.setStatus("SCHEDULED");

            return ResponseEntity.ok(appointment);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();

            // Check if appointment exists and user has permission
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists() || !userId.equals(appointmentDoc.getString("createdBy"))) {
                return ResponseEntity.notFound().build();
            }

            // Validate time format
            Instant startTime = Instant.parse(request.getStartTime());
            Instant endTime = Instant.parse(request.getEndTime());

            // Update appointment data
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", request.getTitle());
            updates.put("description", request.getDescription());
            updates.put("startTime", Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(), startTime.getNano()));
            updates.put("endTime", Timestamp.ofTimeSecondsAndNanos(
                    endTime.getEpochSecond(), endTime.getNano()));
            updates.put("participants", request.getParticipants());
            updates.put("updatedAt", Timestamp.now());

            // Update in Firestore
            firestore.collection("appointments").document(appointmentId).update(updates).get();

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(appointmentId);
            appointment.setTitle(request.getTitle());
            appointment.setDescription(request.getDescription());
            appointment.setStartTime(Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(), startTime.getNano()));
            appointment.setEndTime(Timestamp.ofTimeSecondsAndNanos(
                    endTime.getEpochSecond(), endTime.getNano()));
            appointment.setCreatedBy(userId);
            appointment.setParticipants(request.getParticipants());
            appointment.setStatus(appointmentDoc.getString("status"));

            return ResponseEntity.ok(appointment);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. Manage Participants
    @PostMapping("/{appointmentId}/participants")
    public ResponseEntity<Appointment> updateParticipants(
            @PathVariable String appointmentId,
            @RequestBody ParticipantRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();

            // Check if appointment exists and user has permission
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists() || !userId.equals(appointmentDoc.getString("createdBy"))) {
                return ResponseEntity.notFound().build();
            }

            // Update participants
            Map<String, Object> updates = new HashMap<>();
            updates.put("participants", request.getParticipantIds());
            updates.put("updatedAt", Timestamp.now());

            // Update in Firestore
            firestore.collection("appointments").document(appointmentId).update(updates).get();

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(appointmentId);
            appointment.setTitle(appointmentDoc.getString("title"));
            appointment.setDescription(appointmentDoc.getString("description"));
            appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
            appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
            appointment.setCreatedBy(userId);
            appointment.setParticipants(request.getParticipantIds());
            appointment.setStatus(appointmentDoc.getString("status"));

            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{appointmentId}/participants/{participantId}")
    public ResponseEntity<?> removeParticipant(@PathVariable String appointmentId, @PathVariable String participantId) {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{appointmentId}/participants/{participantId}")
    public ResponseEntity<?> updateParticipant(@PathVariable String appointmentId, @PathVariable String participantId, 
                                             @RequestBody ParticipantRequest request) {
        return ResponseEntity.ok().build();
    }

    // 3. Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData() {
        return ResponseEntity.ok().build();
    }

    // 4. Push Notifications (Mobile)
    @PostMapping("/{appointmentId}/notify")
    public ResponseEntity<?> sendNotification(@PathVariable String appointmentId, @RequestBody NotificationRequest request) {
        return ResponseEntity.ok().build();
    }

    // 5. Quick Appointment Scheduling
    @PostMapping("/quick")
    public ResponseEntity<?> quickSchedule(@RequestBody QuickAppointmentRequest request) {
        return ResponseEntity.ok().build();
    }

    // 6. View Appointments
    @GetMapping
    public ResponseEntity<List<Appointment>> getAppointments(Authentication authentication) {
        try {
            String userId = authentication.getName();

            // Get appointments where user is creator or participant
            var appointments = new ArrayList<Appointment>();
            var appointmentDocs = firestore.collection("appointments")
                    .whereArrayContains("participants", userId)
                    .get()
                    .get()
                    .getDocuments();

            for (QueryDocumentSnapshot doc : appointmentDocs) {
                Appointment appointment = new Appointment();
                appointment.setAppointmentId(doc.getId());
                appointment.setTitle(doc.getString("title"));
                appointment.setDescription(doc.getString("description"));
                appointment.setStartTime(doc.getTimestamp("startTime"));
                appointment.setEndTime(doc.getTimestamp("endTime"));
                appointment.setCreatedBy(doc.getString("createdBy"));
                appointment.setParticipants((List<String>) doc.get("participants"));
                appointment.setStatus(doc.getString("status"));
                appointment.setCreatedAt(doc.getTimestamp("createdAt"));
                appointment.setUpdatedAt(doc.getTimestamp("updatedAt"));
                appointments.add(appointment);
            }

            return ResponseEntity.ok(appointments);
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getAppointmentDetails(@PathVariable String appointmentId) {
        return ResponseEntity.ok().build();
    }

    // 7. Appointment Conflict Detection
    @PostMapping("/check-conflicts")
    public ResponseEntity<?> checkConflicts(@RequestBody ConflictCheckRequest request) {
        return ResponseEntity.ok().build();
    }

    // 8. Export Appointment Summary
    @GetMapping("/export")
    public ResponseEntity<?> exportAppointments(@RequestParam String format,
                                              @RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate) {
        // TODO: Implement service logic
        return ResponseEntity.ok().build();
    }

    // 9. Appointment Status Tracking
    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable String appointmentId, 
                                                   @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok().build();
    }

    // 10. Appointment Tagging System
    @PostMapping("/{appointmentId}/tags")
    public ResponseEntity<?> addTag(@PathVariable String appointmentId, @RequestBody TagRequest request) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{appointmentId}/tags/{tagId}")
    public ResponseEntity<?> removeTag(@PathVariable String appointmentId, @PathVariable String tagId) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tags")
    public ResponseEntity<?> getAllTags() {

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(
            @PathVariable String appointmentId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();

            // Check if appointment exists and user has permission
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists() || !userId.equals(appointmentDoc.getString("createdBy"))) {
                return ResponseEntity.notFound().build();
            }

            // Delete from Firestore
            firestore.collection("appointments").document(appointmentId).delete().get();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 