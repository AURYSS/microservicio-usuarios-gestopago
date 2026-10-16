package com.gestopago.microusuarios.dto;

import com.gestopago.microusuarios.entity.Domicilio;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteUpdateRequest {

    @NotNull(message = "El ID del cliente es obligatorio para la actualización")
    @Positive(message = "El ID debe ser un número positivo")
    private Long id;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "Solo letras y espacios en el nombre")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "Solo letras y espacios en el segundo nombre")
    private String segundoNombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "Solo letras y espacios en el apellido paterno")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "Solo letras y espacios en el apellido materno")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    private String sexo;
    private String nacionalidad;
    private String estadoCivil;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = "El teléfono alternativo debe contener exactamente 10 dígitos si se proporciona")
    private String telefonoAlternativo;

    private String ocupacion;
    private String empresa;

    @Positive(message = "El ingreso mensual debe ser mayor a cero")
    private Double ingresoMensual;

    private Domicilio domicilio;
}
