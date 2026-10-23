package com.gestopago.microusuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa el cuadro central del diagrama que se envía a api-gesto-pago (/pagos/pagar-servicio):
 * - idServicio
 * - idProducto
 * - telefono / referencia
 * - horaLocal
 * - montoPago
 * - upc
 * - tipoFront
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagarServicioGestoPagoRequest {
    private Integer idServicio;
    private Integer idProducto;
    private String telefono;
    private String referencia;
    private String horaLocal;
    private BigDecimal montoPago;
    private String upc;
    private Short tipoFront;
}
