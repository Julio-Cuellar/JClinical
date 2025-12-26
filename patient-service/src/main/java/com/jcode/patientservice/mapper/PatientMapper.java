package com.jcode.patientservice.mapper;

import com.jcode.patientservice.domain.Patient;
import com.jcode.patientservice.dto.PatientRequestDTO;
import com.jcode.patientservice.dto.PatientResponseDTO;
import com.jcode.patientservice.dto.PatientSummaryDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PatientMapper {

    public Patient toEntity(PatientRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Patient.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .middleName(dto.getMiddleName())
                .dateOfBirth(dto.getDateOfBirth())
                .sex(dto.getSex())
                .curp(dto.getCurp())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry() != null ? dto.getCountry() : "México")
                .build();
    }

    public PatientResponseDTO toResponseDTO(Patient patient) {
        if (patient == null) {
            return null;
        }

        return PatientResponseDTO.builder()
                .id(patient.getId())
                .tenantId(patient.getTenantId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .middleName(patient.getMiddleName())
                .dateOfBirth(patient.getDateOfBirth())
                .sex(patient.getSex())
                .curp(patient.getCurp())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .zipCode(patient.getZipCode())
                .country(patient.getCountry())
                .status(patient.getStatus())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .createdByUserId(patient.getCreatedByUserId())
                .age(calculateAge(patient.getDateOfBirth()))
                .fullName(buildFullName(patient))
                .build();
    }

    public PatientSummaryDTO toSummaryDTO(Patient patient) {
        if (patient == null) {
            return null;
        }

        return PatientSummaryDTO.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .middleName(patient.getMiddleName())
                .dateOfBirth(patient.getDateOfBirth())
                .sex(patient.getSex())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .status(patient.getStatus())
                .age(calculateAge(patient.getDateOfBirth()))
                .fullName(buildFullName(patient))
                .build();
    }

    public List<PatientResponseDTO> toResponseDTOList(List<Patient> patients) {
        return patients.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PatientSummaryDTO> toSummaryDTOList(List<Patient> patients) {
        return patients.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    private Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private String buildFullName(Patient patient) {
        StringBuilder fullName = new StringBuilder();
        fullName.append(patient.getFirstName());

        if (patient.getMiddleName() != null && !patient.getMiddleName().isEmpty()) {
            fullName.append(" ").append(patient.getMiddleName());
        }

        fullName.append(" ").append(patient.getLastName());
        return fullName.toString();
    }
}
