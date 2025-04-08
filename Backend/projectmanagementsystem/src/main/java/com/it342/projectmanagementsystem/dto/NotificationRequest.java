package com.it342.projectmanagementsystem.dto;

import java.util.List;
import lombok.Data;

@Data
public class NotificationRequest {
    private String message;
    private String type;
    private List<String> recipientIds;
} 