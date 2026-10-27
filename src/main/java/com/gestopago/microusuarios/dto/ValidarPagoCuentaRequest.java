package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Representa el cuadro izquierdo del diagrama:
 * - NumCuenta (numeroCuenta / numCuenta)
 * - Monto (monto)
 * - Telefono/Ref (telefono / referencia / telefonoReferencia)
 * - ID Servicio (idServicio)
 * - ID Producto (idProducto)
 * - Tipo Front (tipoFront)
 * - Pin Autorizacion (pinAutorizacion / pin)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidarPagoCuentaRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    @JsonAlias({"numCuenta", "numeroCuenta"})
    private UUID numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser estrictamente mayor a 0")
    @JsonAlias({"monto", "montoPago"})
    private Double monto;

    @Size(max = 100, message = "El teléfono o referencia no puede exceder 100 caracteres")
    @JsonAlias({"telefonoReferencia", "telefonoRef", "telefono_ref"})
    private String telefonoReferencia;

    @Size(max = 100, message = "El teléfono no puede exceder 100 caracteres")
    private String telefono;

    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referencia;

    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser mayor a 0")
    private Integer idServicio;

    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser mayor a 0")
    private Integer idProducto;

    @Builder.Default
    private Short tipoFront = 2;

    @JsonAlias({"pin", "pinAutorizacion", "pin_autorizacion"})
    private String pinAutorizacion;

    private String horaLocal;

    @Size(max = 36, message = "El upc no puede exceder 36 caracteres")
    private String upc;

    /**
     * Obtiene la referencia/teléfono principal enviado en la petición.
     */
    public String obtenerTelefonoOReferencia() {
        if (telefonoReferencia != null && !telefonoReferencia.isBlank()) {
            return telefonoReferencia.trim();
        }
        if (referencia != null && !referencia.isBlank()) {
            return referencia.trim();
        }
        if (telefono != null && !telefono.isBlank()) {
            return telefono.trim();
        }
        return null;
    }
}
