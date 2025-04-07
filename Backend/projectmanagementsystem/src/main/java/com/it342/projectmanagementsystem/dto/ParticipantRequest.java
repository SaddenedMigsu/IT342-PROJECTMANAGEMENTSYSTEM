package com.it342.projectmanagementsystem.dto;

import java.util.List;

public class ParticipantRequest {
    private List<String> participantIds;

    public ParticipantRequest() {
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }
} 