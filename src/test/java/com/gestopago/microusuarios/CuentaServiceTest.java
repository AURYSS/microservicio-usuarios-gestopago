package com.gestopago.microusuarios;

import com.gestopago.microusuarios.client.ApiGestoPagoClient;
import com.gestopago.microusuarios.dto.PagarServicioGestoPagoRequest;
import com.gestopago.microusuarios.dto.PagarServicioGestoPagoResponse;
import com.gestopago.microusuarios.dto.PagoServicioCuentaResponse;
import com.gestopago.microusuarios.dto.ValidarPagoCuentaRequest;
import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.CredencialesInvalidasException;
import com.gestopago.microusuarios.exception.CuentaInactivaException;
import com.gestopago.microusuarios.repository.CuentaRepository;
import com.gestopago.microusuarios.service.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ApiGestoPagoClient apiGestoPagoClient;

    private CuentaService cuentaService;

    private UUID numeroCuenta;
    private Cuenta cuentaActiva;

    @BeforeEach
    void setUp() {
        cuentaService = new CuentaService(cuentaRepository, apiGestoPagoClient, new BCryptPasswordEncoder());
        numeroCuenta = UUID.randomUUID();
        cuentaActiva = Cuenta.builder()
                .id(1L)
                .numeroCuenta(numeroCuenta)
                .saldo(500.0)
                .activa(true)
                .pinAutorizacion("1234")
                .build();
    }

    @Test
    void testPagarMontoNegativoOInvalido() {
        assertThrows(BadRequestException.class, () -> cuentaService.pagar(numeroCuenta, -50.0));
        assertThrows(BadRequestException.class, () -> cuentaService.pagar(numeroCuenta, 0.0));
    }

    @Test
    void testPagarSaldoInsuficiente() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        assertThrows(BadRequestException.class, () -> cuentaService.pagar(numeroCuenta, 1000.0));
    }

    @Test
    void testPagarCuentaInactiva() {
        cuentaActiva.setActiva(false);
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        assertThrows(CuentaInactivaException.class, () -> cuentaService.pagar(numeroCuenta, 100.0));
    }

    @Test
    void testPagarExitoso() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuentaActiva);

        Map<String, Object> resultado = cuentaService.pagar(numeroCuenta, 200.0);

        assertNotNull(resultado);
        assertEquals(300.0, resultado.get("nuevoSaldo"));
        assertEquals(300.0, cuentaActiva.getSaldo());
        verify(cuentaRepository, times(1)).save(cuentaActiva);
    }

    @Test
    void testValidarPagoServicioExitoso() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));

        ValidarPagoCuentaRequest request = ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(150.0)
                .telefonoReferencia("4681234567")
                .idServicio(10)
                .idProducto(20)
                .tipoFront((short) 2)
                .pinAutorizacion("1234")
                .build();

        PagoServicioCuentaResponse res = cuentaService.validarPagoServicio(request);

        assertNotNull(res);
        assertTrue(res.getCuentaActiva());
        assertTrue(res.getSaldoSuficiente());
        assertTrue(res.getPinAutorizado());
        assertTrue(res.getReferenciaValida());
        assertEquals(350.0, res.getNuevoSaldo());
        assertNotNull(res.getDatosPagoCentral());
        assertEquals(10, res.getDatosPagoCentral().getIdServicio());
        assertEquals(20, res.getDatosPagoCentral().getIdProducto());
        assertEquals("4681234567", res.getDatosPagoCentral().getReferencia());
        assertNotNull(res.getDatosPagoCentral().getHoraLocal());
        assertNotNull(res.getDatosPagoCentral().getUpc());
    }

    @Test
    void testValidarPagoServicioPinInvalido() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));

        ValidarPagoCuentaRequest request = ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(150.0)
                .telefonoReferencia("4681234567")
                .idServicio(10)
                .idProducto(20)
                .tipoFront((short) 2)
                .pinAutorizacion("9999")
                .build();

        assertThrows(CredencialesInvalidasException.class, () -> cuentaService.validarPagoServicio(request));
    }

    @Test
    void testPagarServicioConCuentaFlujoCompleto() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuentaActiva);
        when(apiGestoPagoClient.pagarServicio(any(PagarServicioGestoPagoRequest.class)))
                .thenReturn(PagarServicioGestoPagoResponse.builder()
                        .idTransaccion(99L)
                        .estado("APROBADA")
                        .codigoMensaje("01")
                        .mensaje("Operacion realizada con exito")
                        .numAutorizacion("887766")
                        .build());

        ValidarPagoCuentaRequest request = ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(120.0)
                .telefonoReferencia("5512345678")
                .idServicio(5)
                .idProducto(12)
                .tipoFront((short) 2)
                .pinAutorizacion("1234")
                .build();

        PagoServicioCuentaResponse res = cuentaService.pagarServicioConCuenta(request);

        assertNotNull(res);
        assertEquals(380.0, res.getNuevoSaldo());
        assertEquals(380.0, cuentaActiva.getSaldo());
        assertNotNull(res.getResultadoPagoGestoPago());
        assertEquals("APROBADA", res.getResultadoPagoGestoPago().getEstado());
        assertEquals("887766", res.getResultadoPagoGestoPago().getNumAutorizacion());
        verify(apiGestoPagoClient, times(1)).pagarServicio(any(PagarServicioGestoPagoRequest.class));
        verify(cuentaRepository, times(1)).save(cuentaActiva);
    }

    @Test
    void testRecargarMontoInvalido() {
        assertThrows(BadRequestException.class, () -> cuentaService.recargar(numeroCuenta, 0.0));
    }

    @Test
    void testRecargarExitoso() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuentaActiva);

        Map<String, Object> resultado = cuentaService.recargar(numeroCuenta, 250.0);

        assertNotNull(resultado);
        assertEquals(750.0, resultado.get("nuevoSaldo"));
        assertEquals(750.0, cuentaActiva.getSaldo());
        verify(cuentaRepository, times(1)).save(cuentaActiva);
    }

    @Test
    void testPagarServicioRechazadoPorProveedorRevierteSaldo() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        when(apiGestoPagoClient.pagarServicio(any(PagarServicioGestoPagoRequest.class)))
                .thenReturn(PagarServicioGestoPagoResponse.builder()
                        .estado("FALLIDA")
                        .codigoMensaje("03")
                        .mensaje("Carrier no disponible")
                        .build());

        ValidarPagoCuentaRequest request = ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(120.0)
                .telefonoReferencia("5512345678")
                .idServicio(5)
                .idProducto(12)
                .tipoFront((short) 2)
                .pinAutorizacion("1234")
                .build();

        assertThrows(BadRequestException.class, () -> cuentaService.pagarServicioConCuenta(request));
        assertEquals(500.0, cuentaActiva.getSaldo());
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void testPagarServicioDuplicadoPorUpcPrevieneDobleCargo() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuentaActiva);
        when(apiGestoPagoClient.pagarServicio(any(PagarServicioGestoPagoRequest.class)))
                .thenReturn(PagarServicioGestoPagoResponse.builder()
                        .idTransaccion(101L)
                        .estado("APROBADA")
                        .codigoMensaje("01")
                        .numAutorizacion("998877")
                        .build());

        ValidarPagoCuentaRequest request = ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(100.0)
                .telefonoReferencia("5512345678")
                .idServicio(5)
                .idProducto(12)
                .tipoFront((short) 2)
                .pinAutorizacion("1234")
                .upc("upc-unico-01")
                .build();

        PagoServicioCuentaResponse primero = cuentaService.pagarServicioConCuenta(request);
        PagoServicioCuentaResponse duplicado = cuentaService.pagarServicioConCuenta(request);

        assertEquals(400.0, primero.getNuevoSaldo());
        assertEquals(400.0, duplicado.getNuevoSaldo());
        assertEquals(400.0, cuentaActiva.getSaldo());
        verify(apiGestoPagoClient, times(1)).pagarServicio(any(PagarServicioGestoPagoRequest.class));
        verify(cuentaRepository, times(1)).save(cuentaActiva);
    }

    @Test
    void testTimeoutEnProveedorMantieneEnProcesoYConciliacionFallidaReembolsaSaldo() {
        when(cuentaRepository.findByNumeroCuenta(numeroCuenta)).thenReturn(Optional.of(cuentaActiva));
        when(cuentaRepository.save(any(Cuenta.class))).thenReturn(cuentaActiva);
        when(apiGestoPagoClient.pagarServicio(any(PagarServicioGestoPagoRequest.class)))
                .thenThrow(new RuntimeException("Read timed out"));

        ValidarPagoCuentaRequest request = ValidarPagoCuentaRequest.builder()
                .numeroCuenta(numeroCuenta)
                .monto(150.0)
                .telefonoReferencia("5512345678")
                .idServicio(5)
                .idProducto(12)
                .tipoFront((short) 2)
                .pinAutorizacion("1234")
                .upc("upc-timeout-01")
                .build();

        // 1. Ante timeout, no se asume fallida: se retiene el saldo y queda EN_PROCESO
        PagoServicioCuentaResponse enProceso = cuentaService.pagarServicioConCuenta(request);
        assertEquals("EN_PROCESO", enProceso.getResultadoPagoGestoPago().getEstado());
        assertEquals(350.0, cuentaActiva.getSaldo());

        // 2. Al conciliar con confirmTx, el proveedor informa que NO se aplicó (FALLIDA / código 70) -> se reintegra el saldo
        when(apiGestoPagoClient.confirmarServicio(any(PagarServicioGestoPagoRequest.class)))
                .thenReturn(PagarServicioGestoPagoResponse.builder()
                        .estado("FALLIDA")
                        .codigoMensaje("70")
                        .mensaje("La transaccion no fue aplicada")
                        .build());

        PagoServicioCuentaResponse conciliado = cuentaService.conciliarPagoServicio(request);
        assertEquals("FALLIDA", conciliado.getResultadoPagoGestoPago().getEstado());
        assertEquals(500.0, conciliado.getNuevoSaldo());
        assertEquals(500.0, cuentaActiva.getSaldo());
    }
}
