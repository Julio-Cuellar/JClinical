package com.jcode.patientservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDeactivatedEvent implements Serializable {

    private UUID patientId;

    private String tenantCode;

    private UUID deactivatedByUserId;
    private String reason;
    private LocalDateTime timestamp;
    private String eventType = "PATIENT_DEACTIVATED";
}
