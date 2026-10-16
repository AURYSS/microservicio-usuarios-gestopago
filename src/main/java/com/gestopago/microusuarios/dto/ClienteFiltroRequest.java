package com.gestopago.microusuarios.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteFiltroRequest {
    private Long id;
    private String curp;
    private String rfc;
    private String correo;
    private UUID numeroCuenta;
    private Boolean activo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
}
