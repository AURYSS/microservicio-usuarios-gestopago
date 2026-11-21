package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private boolean exito;
    private String mensaje;

    // --- Campos de 2FA ---
    private boolean requiere2Fa;
    private String tempToken;
    private String codigo2FaDev; // Para pruebas locales / desarrollo
    private String correoEnmascarado;

    // --- Campos de Sesión / JWT ---
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private String sessionId;

    // --- Datos del Usuario ---
    private Long usuarioId;
    private Long clienteId;
    private String correo;
    private String rol;
    private String dispositivo;

    // --- Control de Sesión Concurrente ---
    private boolean sesionActivaEnOtroDispositivo;
    private String dispositivoActivo;
}
