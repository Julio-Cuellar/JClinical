package com.jcode.patientservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_doctors",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_patient_doctor",
                columnNames = {"tenant_code", "patient_id", "doctor_user_id"}
        ),
        indexes = {
                @Index(name = "idx_pd_tenant_patient", columnList = "tenant_code, patient_id"),
                @Index(name = "idx_pd_tenant_doctor", columnList = "tenant_code, doctor_user_id"),
                @Index(name = "idx_pd_tenant_primary", columnList = "tenant_code, is_primary")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDoctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "doctor_user_id", nullable = false)
    private UUID doctorUserId;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientDoctor)) return false;
        PatientDoctor that = (PatientDoctor) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
