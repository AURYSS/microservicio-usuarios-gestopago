package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaConsultaRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    private UUID numeroCuenta;
}
