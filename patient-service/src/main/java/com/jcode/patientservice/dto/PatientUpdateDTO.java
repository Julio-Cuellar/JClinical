package com.jcode.patientservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientUpdateDTO {

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
