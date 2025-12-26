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
public class PatientUpdatedEvent implements Serializable {

    private UUID patientId;

    // Antes: private UUID tenantId;
    private String tenantCode;

    private UUID updatedByUserId;
    private String updatedFields; // JSON string con campos actualizados
    private LocalDateTime timestamp;
    private String eventType = "PATIENT_UPDATED";
}
