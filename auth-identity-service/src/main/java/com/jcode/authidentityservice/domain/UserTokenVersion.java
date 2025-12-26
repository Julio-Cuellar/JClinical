package com.jcode.authidentityservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_token_versions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenVersion {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    // Relaciones

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_token_versions_user"))
    private User user;
}
