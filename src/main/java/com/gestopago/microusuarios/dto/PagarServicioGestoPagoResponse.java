package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta por api-gesto-pago al ejecutar POST /pagos/pagar-servicio
 * (Firmar -> Obtener Token -> Enviar sendTx).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagarServicioGestoPagoResponse {
    private Long idTransaccion;
    private Integer idServicio;
    private Integer idProducto;
    private String telefono;
    private String referencia;
    private String horaLocal;
    private String montoPago;
    private String upc;
    private String firma;
    private String estado;
    private String codigoMensaje;
    private String mensaje;
    private String idTx;
    private String numAutorizacion;
    private String saldo;
    private String comision;
    private String pin;
    private String legend;
    private String fecha;
}
