package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDeleteRequest {

    @NotNull(message = "El ID del usuario es obligatorio para la operación")
    @Positive(message = "El ID del usuario debe ser un número positivo")
    private Long id;
}
