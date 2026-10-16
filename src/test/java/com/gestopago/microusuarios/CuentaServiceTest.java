package com.gestopago.microusuarios;

import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.CuentaInactivaException;
import com.gestopago.microusuarios.repository.CuentaRepository;
import com.gestopago.microusuarios.service.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private CuentaService cuentaService;

    private UUID numeroCuenta;
    private Cuenta cuentaActiva;

    @BeforeEach
    void setUp() {
        numeroCuenta = UUID.randomUUID();
        cuentaActiva = Cuenta.builder()
                .id(1L)
                .numeroCuenta(numeroCuenta)
                .saldo(500.0)
                .activa(true)
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
}
