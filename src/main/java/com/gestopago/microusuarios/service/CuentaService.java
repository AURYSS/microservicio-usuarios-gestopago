package com.gestopago.microusuarios.service;

import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.CuentaInactivaException;
import com.gestopago.microusuarios.exception.CuentaNoEncontradaException;
import com.gestopago.microusuarios.repository.CuentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuenta(UUID numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con el numero: " + numeroCuenta));
    }

    @Transactional(readOnly = true)
    public List<Cuenta> obtenerCuentasActivas() {
        return cuentaRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public Double obtenerSaldo(UUID numeroCuenta) {
        Cuenta cuenta = obtenerCuenta(numeroCuenta);
        return cuenta.getSaldo();
    }

    @Transactional
    public Map<String, Object> pagar(UUID numeroCuenta, Double monto) {
        if (monto == null || monto <= 0) {
            throw new BadRequestException("El monto a pagar debe ser mayor a 0.");
        }

        Cuenta cuenta = obtenerCuenta(numeroCuenta);

        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new CuentaInactivaException("Operacion denegada: La cuenta esta inactiva y no puede realizar pagos.");
        }

        if (cuenta.getSaldo() < monto) {
            throw new BadRequestException("Saldo insuficiente para realizar el pago.");
        }

        cuenta.setSaldo(cuenta.getSaldo() - monto);
        cuentaRepository.save(cuenta);

        return Map.of(
            "mensaje", "Pago realizado exitosamente",
            "nuevoSaldo", cuenta.getSaldo()
        );
    }

    @Transactional
    public Map<String, Object> recargar(UUID numeroCuenta, Double monto) {
        if (monto == null || monto <= 0) {
            throw new BadRequestException("El monto a recargar debe ser mayor a 0.");
        }

        Cuenta cuenta = obtenerCuenta(numeroCuenta);

        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new CuentaInactivaException("Operacion denegada: La cuenta esta inactiva y no puede recibir recargas.");
        }

        cuenta.setSaldo(cuenta.getSaldo() + monto);
        cuentaRepository.save(cuenta);

        return Map.of(
            "mensaje", "Recarga exitosa",
            "nuevoSaldo", cuenta.getSaldo()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerHistorial(UUID numeroCuenta) {
        Cuenta cuenta = obtenerCuenta(numeroCuenta);

        return Map.of(
            "numeroCuenta", numeroCuenta,
            "estatus", Boolean.TRUE.equals(cuenta.getActiva()) ? "ACTIVA" : "INACTIVA",
            "saldo", cuenta.getSaldo(),
            "historial", List.of(
                "Transaccion 1 - Abono de apertura",
                "Transaccion 2 - Consulta de saldo"
            )
        );
    }
}
