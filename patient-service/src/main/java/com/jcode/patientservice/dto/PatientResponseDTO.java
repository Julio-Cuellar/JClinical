package com.jcode.patientservice.dto;

import com.jcode.patientservice.domain.enums.PatientSex;
import com.jcode.patientservice.domain.enums.PatientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponseDTO {

    private UUID id;
    private String tenantCode;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private PatientSex sex;
    private String curp;
    private String phone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private PatientStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdByUserId;

    private Integer age;
    private String fullName;
}
