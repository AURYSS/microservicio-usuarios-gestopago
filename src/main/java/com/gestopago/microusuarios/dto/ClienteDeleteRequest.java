package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDeleteRequest {

    @NotNull(message = "El ID del cliente es obligatorio para la baja lógica")
    @Positive(message = "El ID del cliente debe ser un número positivo")
    private Long id;
}
