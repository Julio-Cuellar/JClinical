package com.jcode.authidentityservice.domain;

import com.jcode.authidentityservice.domain.enums.TenantRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_memberships_user_tenant",
                        columnNames = { "user_id", "tenant_id" }
                )
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_role", nullable = false, length = 40)
    private TenantRole tenantRole;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Relaciones

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tenant_memberships_user"))
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tenant_memberships_tenant"))
    private Tenant tenant;
}
