package com.jcode.authidentityservice.dto;

import lombok.Data;

@Data
public class RegisterUserRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String userType;       // MEDICO, PERSONAL_SALUD, etc.
    private String cedulaProfesional;
    private String tenantName;
    private String tenantCode;
}
