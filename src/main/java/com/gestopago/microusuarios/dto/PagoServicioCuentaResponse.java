package com.gestopago.microusuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Respuesta completa del flujo de la izquierda del diagrama + conexión al cuadro central:
 * - Resultado de las validaciones de Cuenta (Activa, Saldo Cuenta, Pin Autorización, Validar Teléfono/Ref)
 * - Datos construidos para el cuadro central (IdServicio, IdProducto, Telefono/Referencia, HoraLocal, MontoPago, UPC)
 * - Respuesta del pago de servicio en api-gesto-pago (si se ejecutó el pago completo)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoServicioCuentaResponse {
    private UUID numeroCuenta;
    private Boolean cuentaActiva;
    private Boolean saldoSuficiente;
    private Boolean pinAutorizado;
    private Boolean referenciaValida;
    private Double saldoAnterior;
    private Double monto;
    private Double nuevoSaldo;
    private Short tipoFront;
    private PagarServicioGestoPagoRequest datosPagoCentral;
    private PagarServicioGestoPagoResponse resultadoPagoGestoPago;
}
