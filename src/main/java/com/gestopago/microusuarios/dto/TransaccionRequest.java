package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    private UUID numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser estrictamente mayor a 0")
    private Double monto;
}
