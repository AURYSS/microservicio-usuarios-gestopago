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
public class Verify2FaRequest {

    @NotBlank(message = "El token temporal es obligatorio")
    private String tempToken;

    @NotBlank(message = "El código de verificación 2FA es obligatorio")
    private String codigo;

    /**
     * Si es true, finaliza cualquier sesión activa previa en otro dispositivo.
     */
    private Boolean forzarCierre;
}
