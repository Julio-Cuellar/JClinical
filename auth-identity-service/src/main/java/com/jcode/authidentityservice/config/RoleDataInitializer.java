package com.jcode.authidentityservice.config;

import com.jcode.authidentityservice.domain.Permission;
import com.jcode.authidentityservice.domain.Role;
import com.jcode.authidentityservice.domain.enums.SystemRole;
import com.jcode.authidentityservice.repository.PermissionRepository;
import com.jcode.authidentityservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        initPermissions();
        initRoles();
    }

    private void initPermissions() {
        createPermissionIfNotExists("EXPEDIENTE_READ", "Leer expediente clínico");
        createPermissionIfNotExists("EXPEDIENTE_WRITE", "Escribir/actualizar expediente clínico");
        createPermissionIfNotExists("AGENDA_READ", "Leer agenda");
        createPermissionIfNotExists("AGENDA_WRITE", "Modificar agenda");
        createPermissionIfNotExists("FINANZAS_READ", "Ver finanzas");
        createPermissionIfNotExists("FINANZAS_WRITE", "Modificar finanzas");
    }

    private void createPermissionIfNotExists(String name, String description) {
        permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder()
                                .name(name)
                                .description(description)
                                .build()
                ));
    }

    private void initRoles() {
        Map<SystemRole, Set<String>> rolePermissions = Map.of(
                SystemRole.DOCTOR, Set.of("EXPEDIENTE_READ", "EXPEDIENTE_WRITE", "AGENDA_READ", "AGENDA_WRITE"),
                SystemRole.ASISTENTE, Set.of("EXPEDIENTE_READ", "AGENDA_READ", "AGENDA_WRITE"),
                SystemRole.PACIENTE, Set.of("EXPEDIENTE_READ", "AGENDA_READ"),
                SystemRole.ADMIN_PLATAFORMA, Set.of("EXPEDIENTE_READ", "EXPEDIENTE_WRITE",
                        "AGENDA_READ", "AGENDA_WRITE", "FINANZAS_READ", "FINANZAS_WRITE")
        );

        rolePermissions.forEach((systemRole, permNames) -> {
            String roleName = systemRole.name();
            Role role = roleRepository.findByName(roleName).orElseGet(() ->
                    Role.builder()
                            .name(roleName)
                            .description("Rol " + roleName)
                            .build()
            );

            var perms = permissionRepository.findAll().stream()
                    .filter(p -> permNames.contains(p.getName()))
                    .collect(Collectors.toSet());

            role.setPermissions(perms);
            roleRepository.save(role);
        });
    }
}
