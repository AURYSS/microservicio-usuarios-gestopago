package com.gestopago.microusuarios.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioFiltroRequest {
    private Long id;
    private String correo;
    private String role;
    private Boolean activo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
}
