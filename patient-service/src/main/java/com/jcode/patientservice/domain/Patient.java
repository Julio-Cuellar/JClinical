package com.jcode.patientservice.domain;

import com.jcode.patientservice.domain.enums.PatientSex;
import com.jcode.patientservice.domain.enums.PatientStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_tenant_curp", columnList = "tenant_id, curp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    private PatientSex sex;

    @Column(name = "curp", length = 18)
    private String curp;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "country", length = 50)
    @Builder.Default
    private String country = "México";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PatientStatus status = PatientStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PatientDoctor> patientDoctors = new ArrayList<>();

    // Métodos helper para mantener la relación bidireccional
    public void addPatientDoctor(PatientDoctor patientDoctor) {
        patientDoctors.add(patientDoctor);
        patientDoctor.setPatient(this);
    }

    public void removePatientDoctor(PatientDoctor patientDoctor) {
        patientDoctors.remove(patientDoctor);
        patientDoctor.setPatient(null);
    }
}
