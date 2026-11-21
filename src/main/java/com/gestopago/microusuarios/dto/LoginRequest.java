package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "El nombre de usuario o correo es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    /**
     * Origen de la solicitud: "MOVIL" o "WEB" (default "MOVIL").
     */
    private String dispositivo;

    /**
     * Si es true, finaliza cualquier sesión previa activa en otro dispositivo.
     */
    private Boolean forzarCierre;
}
