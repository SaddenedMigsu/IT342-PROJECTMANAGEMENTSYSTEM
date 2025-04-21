package com.it342.projectmanagementsystem.controller;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.it342.projectmanagementsystem.dto.*;
import com.it342.projectmanagementsystem.model.Appointment;
import com.it342.projectmanagementsystem.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
    private final Firestore firestore;
    private final AppointmentService appointmentService;

    public AppointmentController(Firestore firestore, AppointmentService appointmentService) {
        this.firestore = firestore;
        this.appointmentService = appointmentService;
    }

    // Helper method to create user-appointment relationships
    private void createUserAppointmentRelationships(String appointmentId, String creatorId, List<String> participantIds) {
        try {
            // Create relationship for creator
            Map<String, Object> creatorRelationship = new HashMap<>();
            creatorRelationship.put("userId", creatorId);
            creatorRelationship.put("appointmentId", appointmentId);
            creatorRelationship.put("role", "CREATOR");
            creatorRelationship.put("status", "CONFIRMED");
            creatorRelationship.put("createdAt", Timestamp.now());
            creatorRelationship.put("updatedAt", Timestamp.now());
            
            firestore.collection("user_appointments").document().set(creatorRelationship).get();
            
            // Create relationships for participants
            for (String participantId : participantIds) {
                Map<String, Object> participantRelationship = new HashMap<>();
                participantRelationship.put("userId", participantId);
                participantRelationship.put("appointmentId", appointmentId);
                participantRelationship.put("role", "PARTICIPANT");
                participantRelationship.put("status", "PENDING");
                participantRelationship.put("createdAt", Timestamp.now());
                participantRelationship.put("updatedAt", Timestamp.now());
                
                firestore.collection("user_appointments").document().set(participantRelationship).get();
            }
            
            logger.info("Created user-appointment relationships for appointment: {}", appointmentId);
        } catch (Exception e) {
            logger.error("Error creating user-appointment relationships: {}", e.getMessage());
        }
    }

    // 1. Create and Edit Appointment
    @PostMapping("/create")
    public ResponseEntity<Appointment> createAppointment(
            @RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            logger.info("Creating appointment request from user: {}", userId);

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
            appointmentData.put("status", "PENDING_APPROVAL");
            appointmentData.put("createdAt", Timestamp.now());
            appointmentData.put("updatedAt", Timestamp.now());
            
            // Add faculty approval tracking
            appointmentData.put("facultyApprovals", new HashMap<String, Boolean>());
            appointmentData.put("requiresApproval", true);

            // Save to Firestore
            var docRef = firestore.collection("appointments").document();
            docRef.set(appointmentData).get();
            
            // Create user-appointment relationships
            createUserAppointmentRelationships(docRef.getId(), userId, request.getParticipants());
            
            // Send approval requests to faculty participants
            sendApprovalRequests(docRef.getId(), request.getParticipants(), userId);

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
            appointment.setStatus("PENDING_APPROVAL");

            logger.info("Appointment request created with ID: {}", docRef.getId());
            return ResponseEntity.ok(appointment);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format in appointment request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating appointment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper method to send approval requests to faculty
    private void sendApprovalRequests(String appointmentId, List<String> participantIds, String requesterId) {
        try {
            // Get appointment details
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                logger.error("Appointment not found when sending approval requests: {}", appointmentId);
                return;
            }
            
            // Get appointment details for the notification
            String title = appointmentDoc.getString("title");
            String description = appointmentDoc.getString("description");
            Timestamp startTime = appointmentDoc.getTimestamp("startTime");
            Timestamp endTime = appointmentDoc.getTimestamp("endTime");
            
            // Get requester details
            var requesterDoc = firestore.collection("users").document(requesterId).get().get();
            String requesterName = requesterDoc.getString("firstName") + " " + requesterDoc.getString("lastName");
            
            // Create notification for each faculty participant
            for (String participantId : participantIds) {
                // Check if participant is faculty
                var userDoc = firestore.collection("users").document(participantId).get().get();
                if (userDoc.exists() && "FACULTY".equals(userDoc.getString("role"))) {
                    // Create notification
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("userId", participantId);
                    notificationData.put("appointmentId", appointmentId);
                    notificationData.put("type", "APPOINTMENT_REQUEST");
                    notificationData.put("title", "Appointment Request: " + title);
                    notificationData.put("message", requesterName + " has requested an appointment with you.");
                    notificationData.put("details", Map.of(
                        "title", title,
                        "description", description,
                        "startTime", startTime,
                        "endTime", endTime,
                        "requesterId", requesterId,
                        "requesterName", requesterName
                    ));
                    notificationData.put("status", "UNREAD");
                    notificationData.put("createdAt", Timestamp.now());
                    
                    // Save notification
                    firestore.collection("notifications").document().set(notificationData).get();
                    
                    logger.info("Sent approval request to faculty: {} for appointment: {}", participantId, appointmentId);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending approval requests: {}", e.getMessage());
        }
    }
    
    // Endpoint for faculty to respond to appointment requests
    @PostMapping("/{appointmentId}/approve")
    public ResponseEntity<Appointment> approveAppointment(
            @PathVariable String appointmentId,
            @RequestBody Map<String, Boolean> approval,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            boolean isApproved = approval.getOrDefault("approved", false);
            logger.info("Faculty with email {} {} appointment: {}", userEmail, isApproved ? "approved" : "denied", appointmentId);
            
            // Check if appointment exists
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                logger.error("Appointment not found: {}", appointmentId);
                return ResponseEntity.notFound().build();
            }
            
            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();
            
            // Check if user is faculty
            if (!"FACULTY".equals(userDoc.getString("role"))) {
                logger.error("User {} is not faculty and cannot approve appointments", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Check if user is a participant
            List<String> participants = (List<String>) appointmentDoc.get("participants");
            if (participants == null || !participants.contains(userId)) {
                logger.error("User {} is not a participant in appointment {}", userEmail, appointmentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Timestamp now = Timestamp.now();
            
            // Update faculty approval status
            Map<String, Object> facultyApprovals = (Map<String, Object>) appointmentDoc.get("facultyApprovals");
            if (facultyApprovals == null) {
                facultyApprovals = new HashMap<>();
            }
            facultyApprovals.put(userId, isApproved);
            
            // Check if all faculty have approved
            boolean allApproved = true;
            for (String participantId : participants) {
                var participantDoc = firestore.collection("users").document(participantId).get().get();
                if (participantDoc.exists() && "FACULTY".equals(participantDoc.getString("role"))) {
                    Boolean participantApproval = (Boolean) facultyApprovals.get(participantId);
                    if (participantApproval == null || !participantApproval) {
                        allApproved = false;
                        break;
                    }
                }
            }
            
            // Update appointment status
            Map<String, Object> updates = new HashMap<>();
            updates.put("facultyApprovals", facultyApprovals);
            updates.put("updatedAt", now);
            
            String newStatus = allApproved ? "SCHEDULED" : "PENDING_APPROVAL";
            updates.put("status", newStatus);
            
            // Update in Firestore
            firestore.collection("appointments").document(appointmentId).update(updates).get();
            
            // Update user-appointment relationship
            var userAppointments = firestore.collection("user_appointments")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("appointmentId", appointmentId)
                    .get()
                    .get()
                    .getDocuments();
            
            if (!userAppointments.isEmpty()) {
                var userAppointment = userAppointments.iterator().next();
                Map<String, Object> relationshipUpdates = new HashMap<>();
                relationshipUpdates.put("status", isApproved ? "CONFIRMED" : "DENIED");
                relationshipUpdates.put("hasApproved", isApproved);
                relationshipUpdates.put("updatedAt", now);
                userAppointment.getReference().update(relationshipUpdates).get();
            }
            
            // Create notification for requester
            String requesterId = appointmentDoc.getString("createdBy");
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("userId", requesterId);
            notificationData.put("appointmentId", appointmentId);
            notificationData.put("type", "APPOINTMENT_RESPONSE");
            notificationData.put("title", "Appointment " + (isApproved ? "Approved" : "Denied"));
            notificationData.put("message", userDoc.getString("firstName") + " " + userDoc.getString("lastName") + 
                    " has " + (isApproved ? "approved" : "denied") + " your appointment request.");
            notificationData.put("status", "UNREAD");
            notificationData.put("createdAt", now);
            
            // Save notification
            firestore.collection("notifications").document().set(notificationData).get();
            
            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(appointmentId);
            appointment.setTitle(appointmentDoc.getString("title"));
            appointment.setDescription(appointmentDoc.getString("description"));
            appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
            appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
            appointment.setCreatedBy(requesterId);
            appointment.setParticipants(participants);
            appointment.setStatus(newStatus);
            appointment.setCreatedAt(appointmentDoc.getTimestamp("createdAt"));
            appointment.setUpdatedAt(now);
            appointment.setUserRole("PARTICIPANT");  // Faculty's role
            appointment.setUserStatus(isApproved ? "CONFIRMED" : "DENIED");
            appointment.setHasApproved(isApproved);
            
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            logger.error("Error processing appointment approval: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Updating appointment: {} by user: {}", appointmentId, userEmail);

            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();

            // Check if appointment exists and user has permission
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists() || !userId.equals(appointmentDoc.getString("createdBy"))) {
                logger.error("Appointment not found or user does not have permission");
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

            logger.info("Successfully updated appointment: {}", appointmentId);
            return ResponseEntity.ok(appointment);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format in update request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating appointment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper method to check if user has permission to modify participants
    private boolean hasPermissionToModifyParticipants(DocumentSnapshot userDoc, DocumentSnapshot appointmentDoc) {
        String userId = userDoc.getId();
        String userRole = userDoc.getString("role");
        String creatorId = appointmentDoc.getString("createdBy");
        
        // Allow if user is the creator or is a faculty member
        return userId.equals(creatorId) || "FACULTY".equals(userRole);
    }

    @PostMapping("/{appointmentId}/participants")
    public ResponseEntity<Appointment> addParticipants(
            @PathVariable String appointmentId,
            @RequestBody ParticipantRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Adding participants to appointment: {}", appointmentId);

            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();

            // Check if appointment exists
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                logger.error("Appointment not found: {}", appointmentId);
                return ResponseEntity.notFound().build();
            }
            
            // Check if user has permission
            if (!hasPermissionToModifyParticipants(userDoc, appointmentDoc)) {
                logger.error("User {} does not have permission to modify appointment {}", userEmail, appointmentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String userId = userDoc.getId();
            
            // Get current participants
            List<String> currentParticipants = (List<String>) appointmentDoc.get("participants");
            if (currentParticipants == null) {
                currentParticipants = new ArrayList<>();
            }

            // Add new participants
            for (String participantId : request.getParticipantIds()) {
                if (!currentParticipants.contains(participantId)) {
                    currentParticipants.add(participantId);
                }
            }

            // Update participants
            Map<String, Object> updates = new HashMap<>();
            updates.put("participants", currentParticipants);
            updates.put("updatedAt", Timestamp.now());

            // Update in Firestore
            firestore.collection("appointments").document(appointmentId).update(updates).get();
            logger.info("Successfully added {} participants to appointment {}", 
                    request.getParticipantIds().size(), appointmentId);

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(appointmentId);
            appointment.setTitle(appointmentDoc.getString("title"));
            appointment.setDescription(appointmentDoc.getString("description"));
            appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
            appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
            appointment.setCreatedBy(appointmentDoc.getString("createdBy"));
            appointment.setParticipants(currentParticipants);
            appointment.setStatus(appointmentDoc.getString("status"));

            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            logger.error("Error adding participants to appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{appointmentId}/participants/{participantId}")
    public ResponseEntity<Appointment> removeParticipant(
            @PathVariable String appointmentId,
            @PathVariable String participantId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Removing participant {} from appointment: {}", participantId, appointmentId);

            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();

            // Check if appointment exists
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                logger.error("Appointment not found: {}", appointmentId);
                return ResponseEntity.notFound().build();
            }
            
            // Check if user has permission
            if (!hasPermissionToModifyParticipants(userDoc, appointmentDoc)) {
                logger.error("User {} does not have permission to modify appointment {}", userEmail, appointmentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String userId = userDoc.getId();

            // Get current participants
            List<String> currentParticipants = (List<String>) appointmentDoc.get("participants");
            if (currentParticipants == null) {
                currentParticipants = new ArrayList<>();
            }

            // Remove participant
            currentParticipants.remove(participantId);

            // Update participants
            Map<String, Object> updates = new HashMap<>();
            updates.put("participants", currentParticipants);
            updates.put("updatedAt", Timestamp.now());

            // Update in Firestore
            firestore.collection("appointments").document(appointmentId).update(updates).get();
            logger.info("Successfully removed participant {} from appointment {}", participantId, appointmentId);

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(appointmentId);
            appointment.setTitle(appointmentDoc.getString("title"));
            appointment.setDescription(appointmentDoc.getString("description"));
            appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
            appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
            appointment.setCreatedBy(appointmentDoc.getString("createdBy"));
            appointment.setParticipants(currentParticipants);
            appointment.setStatus(appointmentDoc.getString("status"));

            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            logger.error("Error removing participant from appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{appointmentId}/participants")
    public ResponseEntity<Appointment> updateParticipants(
            @PathVariable String appointmentId,
            @RequestBody ParticipantRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Updating participants for appointment: {}", appointmentId);

            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();

            // Check if appointment exists
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                logger.error("Appointment not found: {}", appointmentId);
                return ResponseEntity.notFound().build();
            }
            
            // Check if user has permission
            if (!hasPermissionToModifyParticipants(userDoc, appointmentDoc)) {
                logger.error("User {} does not have permission to modify appointment {}", userEmail, appointmentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String userId = userDoc.getId();

            // Update participants
            Map<String, Object> updates = new HashMap<>();
            updates.put("participants", request.getParticipantIds());
            updates.put("updatedAt", Timestamp.now());

            // Update in Firestore
            firestore.collection("appointments").document(appointmentId).update(updates).get();
            logger.info("Successfully updated participants for appointment {}", appointmentId);

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(appointmentId);
            appointment.setTitle(appointmentDoc.getString("title"));
            appointment.setDescription(appointmentDoc.getString("description"));
            appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
            appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
            appointment.setCreatedBy(appointmentDoc.getString("createdBy"));
            appointment.setParticipants(request.getParticipantIds());
            appointment.setStatus(appointmentDoc.getString("status"));

            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            logger.error("Error updating participants for appointment {}: {}", appointmentId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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
            String userEmail = authentication.getName();
            logger.info("Getting appointments for user with email: {}", userEmail);

            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();
            String userRole = userDoc.getString("role");

            // Get user's appointment relationships
            var userAppointments = firestore.collection("user_appointments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .get()
                    .getDocuments();

            List<Appointment> appointments = new ArrayList<>();
            for (var userAppointment : userAppointments) {
                String appointmentId = userAppointment.getString("appointmentId");
                var appointmentDoc = firestore.collection("appointments")
                        .document(appointmentId)
                        .get()
                        .get();

                if (appointmentDoc.exists()) {
                    Appointment appointment = new Appointment();
                    appointment.setAppointmentId(appointmentId);
                    appointment.setTitle(appointmentDoc.getString("title"));
                    appointment.setDescription(appointmentDoc.getString("description"));
                    appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
                    appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
                    appointment.setCreatedBy(appointmentDoc.getString("createdBy"));
                    appointment.setParticipants((List<String>) appointmentDoc.get("participants"));
                    appointment.setStatus(appointmentDoc.getString("status"));
                    appointment.setCreatedAt(appointmentDoc.getTimestamp("createdAt"));
                    appointment.setUpdatedAt(appointmentDoc.getTimestamp("updatedAt"));
                    
                    // Add user-specific appointment data
                    appointment.setUserRole(userAppointment.getString("role"));
                    appointment.setUserStatus(userAppointment.getString("status"));

                    // For faculty, add approval status if applicable
                    if ("FACULTY".equals(userRole)) {
                        Map<String, Object> facultyApprovals = (Map<String, Object>) appointmentDoc.get("facultyApprovals");
                        if (facultyApprovals != null) {
                            Boolean hasApproved = (Boolean) facultyApprovals.get(userId);
                            appointment.setHasApproved(hasApproved != null ? hasApproved : false);
                        }
                    }
                    
                    appointments.add(appointment);
                }
            }

            logger.info("Successfully retrieved {} appointments for user {}", appointments.size(), userEmail);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            logger.error("Error fetching appointments: {}", e.getMessage());
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
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAppointments(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Exporting appointments for user: {} between {} and {}", userEmail, startDate, endDate);

            // Get user document
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();
            logger.info("Found user ID: {}", userId);

            // Get user's appointments
            var userAppointments = firestore.collection("user_appointments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();

            logger.info("Found {} user appointments", userAppointments.size());

            // Convert dates if provided
            Timestamp startTimestamp = null;
            Timestamp endTimestamp = null;
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    Instant start = Instant.parse(startDate);
                    startTimestamp = Timestamp.ofTimeSecondsAndNanos(
                        start.getEpochSecond(),
                        start.getNano()
                    );
                    logger.info("Parsed start date: {} ({})", startTimestamp.toDate(), startTimestamp);
                } catch (Exception e) {
                    logger.error("Error parsing start date: {}", e.getMessage());
                }
            }
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    Instant end = Instant.parse(endDate);
                    endTimestamp = Timestamp.ofTimeSecondsAndNanos(
                        end.getEpochSecond(),
                        end.getNano()
                    );
                    logger.info("Parsed end date: {} ({})", endTimestamp.toDate(), endTimestamp);
                } catch (Exception e) {
                    logger.error("Error parsing end date: {}", e.getMessage());
                }
            }

            // Prepare CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Title,Description,Start Time,End Time,Status,Participants,Tags\n");

            // Get appointment details and build CSV
            ZoneId philippineZone = ZoneId.of("Asia/Manila");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(philippineZone);

            int processedAppointments = 0;
            int skippedAppointments = 0;
            for (var userAppointment : userAppointments) {
                try {
                    String appointmentId = userAppointment.getString("appointmentId");
                    logger.info("Processing appointment: {}", appointmentId);

                    var appointmentDoc = firestore.collection("appointments")
                            .document(appointmentId)
                            .get()
                            .get();

                    if (appointmentDoc.exists()) {
                        Timestamp appointmentStart = appointmentDoc.getTimestamp("startTime");
                        Timestamp appointmentEnd = appointmentDoc.getTimestamp("endTime");

                        logger.info("Appointment {} time range: {} ({}) to {} ({})", 
                            appointmentId, 
                            appointmentStart.toDate(), appointmentStart,
                            appointmentEnd.toDate(), appointmentEnd);

                        // Apply date filter if dates are provided
                        // Check if appointment falls within the date range
                        if (startTimestamp != null && endTimestamp != null) {
                            // Log comparison results
                            boolean isAfterEndDate = appointmentStart.compareTo(endTimestamp) > 0;
                            boolean isBeforeStartDate = appointmentEnd.compareTo(startTimestamp) < 0;
                            
                            logger.info("Date range check for appointment {}: isAfterEndDate={}, isBeforeStartDate={}", 
                                appointmentId, isAfterEndDate, isBeforeStartDate);
                            
                            // Skip if appointment starts after end date or ends before start date
                            if (isAfterEndDate || isBeforeStartDate) {
                                logger.info("Appointment {} outside date range - Skipping", appointmentId);
                                skippedAppointments++;
                                continue;
                            }
                        } else if (startTimestamp != null) {
                            // Skip if appointment ends before start date
                            boolean isBeforeStartDate = appointmentEnd.compareTo(startTimestamp) < 0;
                            logger.info("Start date check for appointment {}: isBeforeStartDate={}", 
                                appointmentId, isBeforeStartDate);
                            
                            if (isBeforeStartDate) {
                                logger.info("Appointment {} before start date - Skipping", appointmentId);
                                skippedAppointments++;
                                continue;
                            }
                        } else if (endTimestamp != null) {
                            // Skip if appointment starts after end date
                            boolean isAfterEndDate = appointmentStart.compareTo(endTimestamp) > 0;
                            logger.info("End date check for appointment {}: isAfterEndDate={}", 
                                appointmentId, isAfterEndDate);
                            
                            if (isAfterEndDate) {
                                logger.info("Appointment {} after end date - Skipping", appointmentId);
                                skippedAppointments++;
                                continue;
                            }
                        }

                        // Get participant names
                        List<String> participantIds = (List<String>) appointmentDoc.get("participants");
                        List<String> participantNames = new ArrayList<>();
                        if (participantIds != null) {
                            for (String participantId : participantIds) {
                                var participantDoc = firestore.collection("users")
                                        .document(participantId)
                                        .get()
                                        .get();
                                if (participantDoc.exists()) {
                                    String name = participantDoc.getString("firstName") + " " + 
                                                participantDoc.getString("lastName");
                                    participantNames.add(name);
                                }
                            }
                        }

                        // Get tags
                        List<Object> tags = (List<Object>) appointmentDoc.get("tags");
                        List<String> tagNames = new ArrayList<>();
                        if (tags != null) {
                            for (Object tagObj : tags) {
                                if (tagObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> tag = (Map<String, Object>) tagObj;
                                    String tagName = (String) tag.get("name");
                                    if (tagName != null) {
                                        tagNames.add(tagName);
                                    }
                                }
                            }
                        }

                        // Format CSV line
                        csv.append(String.format("\"%s\",", escapeCsvField(appointmentDoc.getString("title"))))
                           .append(String.format("\"%s\",", escapeCsvField(appointmentDoc.getString("description"))))
                           .append(String.format("\"%s\",", formatter.format(appointmentStart.toDate().toInstant())))
                           .append(String.format("\"%s\",", formatter.format(appointmentEnd.toDate().toInstant())))
                           .append(String.format("\"%s\",", appointmentDoc.getString("status")))
                           .append(String.format("\"%s\",", escapeCsvField(String.join(", ", participantNames))))
                           .append(String.format("\"%s\"\n", escapeCsvField(String.join(", ", tagNames))));

                        processedAppointments++;
                    } else {
                        logger.warn("Appointment {} not found", appointmentId);
                    }
                } catch (Exception e) {
                    logger.error("Error processing appointment: {}", e.getMessage());
                    // Continue processing other appointments
                    continue;
                }
            }

            logger.info("Processed {} appointments, skipped {} appointments", processedAppointments, skippedAppointments);

            // Generate file name with timestamp
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .withZone(philippineZone)
                    .format(Instant.now());
            String filename = "appointments_" + timestamp + ".csv";

            // Set response headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvBytes.length));

            logger.info("Generated CSV with {} bytes", csvBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);

        } catch (Exception e) {
            logger.error("Error exporting appointments: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper method to escape CSV fields
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        return field.replace("\"", "\"\"");
    }

    // 9. Appointment Status Tracking
    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable String appointmentId, 
                                                   @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok().build();
    }

    // 10. Appointment Tagging System
    @PostMapping("/{appointmentId}/tags")
    public ResponseEntity<Map<String, Object>> addTag(
            @PathVariable String appointmentId,
            @RequestBody TagRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Adding tag to appointment: {} by user: {}", appointmentId, userEmail);

            // Get user document
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();

            // Check if appointment exists and user has access
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                return ResponseEntity.notFound().build();
            }

            List<String> participants = (List<String>) appointmentDoc.get("participants");
            if (!participants.contains(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Create new tag
            Map<String, Object> newTag = new HashMap<>();
            newTag.put("name", request.getName());
            newTag.put("color", request.getColor());

            // Get current tags or initialize new list
            List<Map<String, Object>> currentTags = (List<Map<String, Object>>) appointmentDoc.get("tags");
            if (currentTags == null) {
                currentTags = new ArrayList<>();
            }

            // Check if tag with same name already exists
            boolean tagExists = currentTags.stream()
                    .anyMatch(tag -> request.getName().equals(tag.get("name")));

            if (tagExists) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Tag with this name already exists"));
            }

            // Add new tag
            currentTags.add(newTag);

            // Update appointment
            appointmentDoc.getReference().update("tags", currentTags).get();

            return ResponseEntity.ok(newTag);
        } catch (Exception e) {
            logger.error("Error adding tag to appointment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{appointmentId}/tags/{tagName}")
    public ResponseEntity<Void> removeTag(
            @PathVariable String appointmentId,
            @PathVariable String tagName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Removing tag {} from appointment: {} by user: {}", tagName, appointmentId, userEmail);

            // Get user document
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();

            // Check if appointment exists and user has access
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                return ResponseEntity.notFound().build();
            }

            List<String> participants = (List<String>) appointmentDoc.get("participants");
            if (!participants.contains(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get current tags
            List<Map<String, Object>> currentTags = (List<Map<String, Object>>) appointmentDoc.get("tags");
            if (currentTags == null || currentTags.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Remove tag with matching name
            boolean removed = currentTags.removeIf(tag -> tagName.equals(tag.get("name")));
            if (!removed) {
                return ResponseEntity.notFound().build();
            }

            // Update appointment
            appointmentDoc.getReference().update("tags", currentTags).get();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing tag from appointment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{appointmentId}/tags")
    public ResponseEntity<List<Map<String, Object>>> getAppointmentTags(
            @PathVariable String appointmentId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Fetching tags for appointment: {} by user: {}", appointmentId, userEmail);

            // Get user document
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();

            // Check if appointment exists and user has access
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                return ResponseEntity.notFound().build();
            }

            List<String> participants = (List<String>) appointmentDoc.get("participants");
            if (!participants.contains(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get tags
            List<Map<String, Object>> tags = (List<Map<String, Object>>) appointmentDoc.get("tags");
            return ResponseEntity.ok(tags != null ? tags : new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error fetching appointment tags: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> deleteAppointment(
            @PathVariable String appointmentId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Attempting to delete appointment: {} by user: {}", appointmentId, userEmail);

            // Get user document by email
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                logger.error("User with email {} not found", userEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();

            // Check if appointment exists
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                logger.error("Appointment not found: {}", appointmentId);
                return ResponseEntity.notFound().build();
            }

            // Check if user is the creator
            if (!userId.equals(appointmentDoc.getString("createdBy"))) {
                logger.error("User {} is not the creator of appointment {}", userEmail, appointmentId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Delete appointment
            firestore.collection("appointments").document(appointmentId).delete().get();
            
            // Delete all user-appointment relationships for this appointment
            var userAppointments = firestore.collection("user_appointments")
                    .whereEqualTo("appointmentId", appointmentId)
                    .get()
                    .get()
                    .getDocuments();
            
            for (var userAppointment : userAppointments) {
                userAppointment.getReference().delete().get();
            }

            // Delete any notifications related to this appointment
            var notifications = firestore.collection("notifications")
                    .whereEqualTo("appointmentId", appointmentId)
                    .get()
                    .get()
                    .getDocuments();
            
            for (var notification : notifications) {
                notification.getReference().delete().get();
            }

            logger.info("Successfully deleted appointment: {} and all related data", appointmentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting appointment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // New endpoint to get user's appointments
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Appointment>> getUserAppointments(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            String requestingUserEmail = authentication.getName();
            
            // Get the requesting user's document by email
            var requestingUserDocs = firestore.collection("users")
                    .whereEqualTo("email", requestingUserEmail)
                    .get()
                    .get()
                    .getDocuments();

            if (requestingUserDocs.isEmpty()) {
                logger.error("Requesting user with email {} not found", requestingUserEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            var requestingUserDoc = requestingUserDocs.iterator().next();
            String requestingUserId = requestingUserDoc.getId();

            // Check if the requesting user has permission to view these appointments
            if (!requestingUserId.equals(userId)) {
                // TODO: Add role-based permission check here if needed
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get user's appointment relationships
            var userAppointments = firestore.collection("user_appointments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .get()
                    .getDocuments();

            List<Appointment> appointments = new ArrayList<>();
            for (var userAppointment : userAppointments) {
                String appointmentId = userAppointment.getString("appointmentId");
                var appointmentDoc = firestore.collection("appointments")
                        .document(appointmentId)
                        .get()
                        .get();

                if (appointmentDoc.exists()) {
                    Appointment appointment = new Appointment();
                    appointment.setAppointmentId(appointmentId);
                    appointment.setTitle(appointmentDoc.getString("title"));
                    appointment.setDescription(appointmentDoc.getString("description"));
                    appointment.setStartTime(appointmentDoc.getTimestamp("startTime"));
                    appointment.setEndTime(appointmentDoc.getTimestamp("endTime"));
                    appointment.setCreatedBy(appointmentDoc.getString("createdBy"));
                    appointment.setParticipants((List<String>) appointmentDoc.get("participants"));
                    appointment.setStatus(appointmentDoc.getString("status"));
                    
                    // Add user-specific appointment data
                    appointment.setUserRole(userAppointment.getString("role"));
                    appointment.setUserStatus(userAppointment.getString("status"));
                    
                    appointments.add(appointment);
                }
            }

            logger.info("Successfully retrieved {} appointments for user {}", appointments.size(), userId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            logger.error("Error fetching user appointments: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint for students to request appointments with faculty
    @PostMapping("/request-faculty")
    public ResponseEntity<?> requestFacultyAppointment(
            @RequestBody FacultyAppointmentRequest request,
            Authentication authentication) {
        try {
            String studentEmail = authentication.getName();
            logger.info("Student {} requesting appointment with faculty: {}", studentEmail, request.getUserId());

            // Verify the student's role using email
            var studentDocs = firestore.collection("users")
                    .whereEqualTo("email", studentEmail)
                    .get()
                    .get()
                    .getDocuments();

            if (studentDocs.isEmpty()) {
                logger.error("User with email {} not found", studentEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            var studentDoc = studentDocs.iterator().next();
            String studentId = studentDoc.getId();

            if (!"STUDENT".equals(studentDoc.getString("role"))) {
                logger.error("User {} is not a student and cannot request faculty appointments", studentEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Verify the faculty exists
            DocumentSnapshot facultyDoc = firestore.collection("users").document(request.getUserId()).get().get();
            if (!facultyDoc.exists() || !"FACULTY".equals(facultyDoc.getString("role"))) {
                logger.error("Faculty {} does not exist or is not a faculty member", request.getUserId());
                return ResponseEntity.badRequest().build();
            }

            // Check for appointment conflicts
            List<Map<String, Object>> conflicts = appointmentService.checkAppointmentConflicts(
                request.getStartTime(),
                request.getEndTime(),
                List.of(studentId, request.getUserId())
            );

            if (!conflicts.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Appointment conflicts detected");
                response.put("conflicts", conflicts);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Validate time format and convert to Philippine timezone
            ZoneId philippineZone = ZoneId.of("Asia/Manila");
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME.withZone(philippineZone);
            
            Instant startTime = Instant.parse(request.getStartTime());
            Instant endTime = Instant.parse(request.getEndTime());
            
            // Convert to Philippine time
            ZonedDateTime phStartTime = startTime.atZone(philippineZone);
            ZonedDateTime phEndTime = endTime.atZone(philippineZone);

            // Create timestamps for tracking
            Timestamp now = Timestamp.now();

            // Create appointment data
            Map<String, Object> appointmentData = new HashMap<>();
            appointmentData.put("title", request.getTitle());
            appointmentData.put("description", request.getDescription());
            appointmentData.put("startTime", Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(), startTime.getNano()));
            appointmentData.put("endTime", Timestamp.ofTimeSecondsAndNanos(
                    endTime.getEpochSecond(), endTime.getNano()));
            appointmentData.put("timezone", "Asia/Manila");
            appointmentData.put("createdBy", studentId);
            appointmentData.put("participants", List.of(studentId, request.getUserId()));
            appointmentData.put("status", "PENDING_APPROVAL");
            appointmentData.put("createdAt", now);
            appointmentData.put("updatedAt", now);
            
            // Add faculty approval tracking
            Map<String, Boolean> facultyApprovals = new HashMap<>();
            facultyApprovals.put(request.getUserId(), false);
            appointmentData.put("facultyApprovals", facultyApprovals);
            appointmentData.put("requiresApproval", true);
            appointmentData.put("appointmentType", "FACULTY_REQUEST");

            // Save to Firestore
            var docRef = firestore.collection("appointments").document();
            docRef.set(appointmentData).get();
            
            // Create user-appointment relationships with proper roles and status
            // For student (creator)
            Map<String, Object> studentRelationship = new HashMap<>();
            studentRelationship.put("userId", studentId);
            studentRelationship.put("appointmentId", docRef.getId());
            studentRelationship.put("role", "CREATOR");
            studentRelationship.put("status", "PENDING");
            studentRelationship.put("createdAt", now);
            studentRelationship.put("updatedAt", now);
            firestore.collection("user_appointments").document().set(studentRelationship).get();
            
            // For faculty
            Map<String, Object> facultyRelationship = new HashMap<>();
            facultyRelationship.put("userId", request.getUserId());
            facultyRelationship.put("appointmentId", docRef.getId());
            facultyRelationship.put("role", "PARTICIPANT");
            facultyRelationship.put("status", "PENDING");
            facultyRelationship.put("hasApproved", false);
            facultyRelationship.put("createdAt", now);
            facultyRelationship.put("updatedAt", now);
            firestore.collection("user_appointments").document().set(facultyRelationship).get();
            
            // Send notification to faculty
            sendFacultyAppointmentRequest(docRef.getId(), request.getUserId(), studentId, request);

            // Create response
            Appointment appointment = new Appointment();
            appointment.setAppointmentId(docRef.getId());
            appointment.setTitle(request.getTitle());
            appointment.setDescription(request.getDescription());
            appointment.setStartTime(Timestamp.ofTimeSecondsAndNanos(
                    startTime.getEpochSecond(), startTime.getNano()));
            appointment.setEndTime(Timestamp.ofTimeSecondsAndNanos(
                    endTime.getEpochSecond(), endTime.getNano()));
            appointment.setCreatedBy(studentId);
            appointment.setParticipants(List.of(studentId, request.getUserId()));
            appointment.setStatus("PENDING_APPROVAL");
            appointment.setCreatedAt(now);
            appointment.setUpdatedAt(now);
            appointment.setUserRole("CREATOR");  // For student creating the request
            appointment.setUserStatus("PENDING");
            appointment.setHasApproved(false);  // For tracking faculty approval

            logger.info("Faculty appointment request created with ID: {} for time: {} to {}", 
                docRef.getId(), 
                phStartTime.format(formatter),
                phEndTime.format(formatter));
            return ResponseEntity.ok(appointment);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format in faculty appointment request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating faculty appointment request: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Helper method to send faculty appointment request notification
    private void sendFacultyAppointmentRequest(String appointmentId, String facultyId, String studentId, FacultyAppointmentRequest request) {
        try {
            // Get student details
            var studentDoc = firestore.collection("users").document(studentId).get().get();
            String studentName = studentDoc.getString("firstName") + " " + studentDoc.getString("lastName");
            
            // Create notification for faculty
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("userId", facultyId);
            notificationData.put("appointmentId", appointmentId);
            notificationData.put("type", "FACULTY_APPOINTMENT_REQUEST");
            notificationData.put("title", "Appointment Request from Student");
            notificationData.put("message", studentName + " has requested an appointment with you.");
            notificationData.put("details", Map.of(
                "title", request.getTitle(),
                "description", request.getDescription(),
                "startTime", Timestamp.ofTimeSecondsAndNanos(
                    Instant.parse(request.getStartTime()).getEpochSecond(),
                    Instant.parse(request.getStartTime()).getNano()),
                "endTime", Timestamp.ofTimeSecondsAndNanos(
                    Instant.parse(request.getEndTime()).getEpochSecond(),
                    Instant.parse(request.getEndTime()).getNano()),
                "studentId", studentId,
                "studentName", studentName,
                "reason", request.getReason()
            ));
            notificationData.put("status", "UNREAD");
            notificationData.put("createdAt", Timestamp.now());
            
            // Save notification
            firestore.collection("notifications").document().set(notificationData).get();
            
            logger.info("Sent faculty appointment request notification to: {} for appointment: {}", facultyId, appointmentId);
        } catch (Exception e) {
            logger.error("Error sending faculty appointment request notification: {}", e.getMessage());
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

    // Get top 3 most booked faculty (Admin only)
    @GetMapping("/faculty/most-booked")
    public ResponseEntity<List<Map<String, Object>>> getTopBookedFaculty(Authentication authentication) {
        try {
            // Check if user is admin
            if (!isAdmin(authentication)) {
                logger.error("Unauthorized access attempt to most booked faculty by user: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            logger.info("Admin {} fetching top 3 most booked faculty members", authentication.getName());

            // Get all appointments
            var appointments = firestore.collection("appointments")
                    .whereEqualTo("status", "SCHEDULED")  // Only count confirmed appointments
                    .get()
                    .get()
                    .getDocuments();

            // Count appointments per faculty
            Map<String, Integer> facultyBookings = new HashMap<>();
            Map<String, String> facultyNames = new HashMap<>();

            for (var appointment : appointments) {
                List<String> participants = (List<String>) appointment.get("participants");
                if (participants != null) {
                    for (String participantId : participants) {
                        // Check if participant is faculty
                        var userDoc = firestore.collection("users")
                                .document(participantId)
                                .get()
                                .get();
                        
                        if (userDoc.exists() && "FACULTY".equals(userDoc.getString("role"))) {
                            // Count the booking
                            facultyBookings.merge(participantId, 1, Integer::sum);
                            
                            // Store faculty name if not already stored
                            if (!facultyNames.containsKey(participantId)) {
                                String firstName = userDoc.getString("firstName");
                                String lastName = userDoc.getString("lastName");
                                facultyNames.put(participantId, firstName + " " + lastName);
                            }
                        }
                    }
                }
            }

            // Sort faculty by booking count and get top 3
            List<Map<String, Object>> topFaculty = facultyBookings.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(entry -> {
                        Map<String, Object> facultyData = new HashMap<>();
                        facultyData.put("userId", entry.getKey());
                        facultyData.put("name", facultyNames.get(entry.getKey()));
                        facultyData.put("bookingCount", entry.getValue());
                        return facultyData;
                    })
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved top 3 most booked faculty members");
            return ResponseEntity.ok(topFaculty);
        } catch (Exception e) {
            logger.error("Error fetching top booked faculty: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAppointmentStats(Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            logger.info("Admin {} requesting appointment statistics", adminEmail);

            Map<String, Object> stats = appointmentService.getAppointmentStats(adminEmail);
            
            logger.info("Successfully retrieved appointment statistics");
            return ResponseEntity.ok(stats);
        } catch (SecurityException e) {
            logger.error("Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error fetching appointment statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{appointmentId}/tags/{tagName}")
    public ResponseEntity<Map<String, Object>> updateTag(
            @PathVariable String appointmentId,
            @PathVariable String tagName,
            @RequestBody TagRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            logger.info("Updating tag {} in appointment: {} by user: {}", tagName, appointmentId, userEmail);

            // Get user document
            var userDocs = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .get()
                    .getDocuments();
            
            if (userDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            var userDoc = userDocs.iterator().next();
            String userId = userDoc.getId();

            // Check if appointment exists and user has access
            var appointmentDoc = firestore.collection("appointments").document(appointmentId).get().get();
            if (!appointmentDoc.exists()) {
                return ResponseEntity.notFound().build();
            }

            List<String> participants = (List<String>) appointmentDoc.get("participants");
            if (!participants.contains(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get current tags
            List<Map<String, Object>> currentTags = (List<Map<String, Object>>) appointmentDoc.get("tags");
            if (currentTags == null || currentTags.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if new tag name already exists (only if name is being changed)
            if (!tagName.equals(request.getName()) && 
                currentTags.stream().anyMatch(tag -> request.getName().equals(tag.get("name")))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Tag with this name already exists"));
            }

            // Find and update the tag
            boolean updated = false;
            Map<String, Object> updatedTag = null;
            for (Map<String, Object> tag : currentTags) {
                if (tagName.equals(tag.get("name"))) {
                    tag.put("name", request.getName());
                    tag.put("color", request.getColor());
                    updatedTag = tag;
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                return ResponseEntity.notFound().build();
            }

            // Update appointment
            appointmentDoc.getReference().update("tags", currentTags).get();

            return ResponseEntity.ok(updatedTag);
        } catch (Exception e) {
            logger.error("Error updating tag in appointment: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
} 