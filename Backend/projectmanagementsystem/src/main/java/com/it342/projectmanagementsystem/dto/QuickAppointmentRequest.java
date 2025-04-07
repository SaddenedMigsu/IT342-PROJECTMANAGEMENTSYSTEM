package com.it342.projectmanagementsystem.dto;

import java.util.List;
import lombok.Data;

@Data
public class QuickAppointmentRequest {
    private String title;
    private String startTime;
    private String endTime;
    private List<String> participantIds;
} 