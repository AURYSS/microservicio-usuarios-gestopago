package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CerrarSesionPreviaRequest {

    @NotBlank(message = "El token temporal es obligatorio")
    private String tempToken;
}
