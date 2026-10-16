package com.gestopago.microusuarios.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long id;
    private String correo;
    private String role;
    private Boolean activo;
    private Long clienteId;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
