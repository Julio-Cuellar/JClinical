package com.jcode.patientservice.dto;

import com.jcode.patientservice.domain.enums.PatientSex;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String lastName;

    @Size(max = 100, message = "El segundo apellido no puede exceder 100 caracteres")
    private String middleName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate dateOfBirth;

    @NotNull(message = "El sexo es obligatorio")
    private PatientSex sex;

    @Size(min = 18, max = 18, message = "El CURP debe tener exactamente 18 caracteres")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9]{2}$",
            message = "El formato del CURP no es válido")
    private String curp;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "El formato del teléfono no es válido")
    private String phone;

    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String addressLine1;

    @Size(max = 255, message = "La dirección 2 no puede exceder 255 caracteres")
    private String addressLine2;

    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    private String city;

    @Size(max = 100, message = "El estado no puede exceder 100 caracteres")
    private String state;

    @Size(max = 10, message = "El código postal no puede exceder 10 caracteres")
    private String zipCode;

    @Size(max = 50, message = "El país no puede exceder 50 caracteres")
    private String country;
}
