package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaResponse {
    private Long id;
    private UUID numeroCuenta;
    private Double saldo;
    private Boolean activa;

    @JsonIgnore
    private String pinAutorizacion;
}
