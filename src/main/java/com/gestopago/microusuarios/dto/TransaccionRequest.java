package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"numCuenta", "numeroCuenta"})
    private UUID numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser estrictamente mayor a 0")
    @JsonAlias({"monto", "montoPago"})
    private Double monto;

    @JsonAlias({"telefonoReferencia", "telefonoRef", "telefono_ref"})
    private String telefonoReferencia;

    private String telefono;

    private String referencia;

    private Integer idServicio;

    private Integer idProducto;

    private Short tipoFront;

    @JsonAlias({"pin", "pinAutorizacion", "pin_autorizacion"})
    private String pinAutorizacion;

    private String horaLocal;

    private String upc;

    public boolean esPagoDeServicioCompleto() {
        return idServicio != null && idProducto != null;
    }

    public ValidarPagoCuentaRequest toValidarPagoCuentaRequest() {
        return ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(monto)
                .telefonoReferencia(telefonoReferencia)
                .telefono(telefono)
                .referencia(referencia)
                .idServicio(idServicio)
                .idProducto(idProducto)
                .tipoFront(tipoFront != null ? tipoFront : (short) 2)
                .pinAutorizacion(pinAutorizacion)
                .horaLocal(horaLocal)
                .upc(upc)
                .build();
    }
}
