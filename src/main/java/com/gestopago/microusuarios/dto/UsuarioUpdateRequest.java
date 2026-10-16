package com.gestopago.microusuarios.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioUpdateRequest {

    @NotNull(message = "El ID del usuario es obligatorio para actualizar")
    @Positive(message = "El ID del usuario debe ser un número positivo")
    private Long id;

    @Email(message = "El formato del correo electrónico es inválido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
        message = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;

    @Pattern(regexp = "^(ADMIN|CLIENTE)$", message = "El rol debe ser estrictamente ADMIN o CLIENTE")
    private String role;

    private Boolean activo;
}
