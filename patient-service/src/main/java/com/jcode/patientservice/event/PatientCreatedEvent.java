package com.jcode.patientservice.event;

import com.jcode.patientservice.domain.enums.PatientSex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreatedEvent implements Serializable {

    private UUID patientId;

    private String tenantCode;

    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private PatientSex sex;
    private String curp;
    private String phone;
    private String email;
    private UUID createdByUserId;
    private UUID primaryDoctorUserId;
    private LocalDateTime timestamp;
    private String eventType = "PATIENT_CREATED";
}
