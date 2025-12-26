package com.jcode.patientservice.dto;

import com.jcode.patientservice.domain.enums.PatientSex;
import com.jcode.patientservice.domain.enums.PatientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSummaryDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private PatientSex sex;
    private String phone;
    private String email;
    private PatientStatus status;
    private Integer age;
    private String fullName;
}
