package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.repository.CuentaRepository;
import com.gestopago.microusuarios.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class CuentaController {

    private final CuentaRepository cuentaRepository;

    public CuentaController(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable UUID numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con el número: " + numeroCuenta));
        return ResponseEntity.ok(cuenta);
    }
    
    @GetMapping("/activas")
    public ResponseEntity<List<Cuenta>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaRepository.findByActivaTrue());
    }
    
    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<Double> obtenerSaldo(@PathVariable UUID numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con el número: " + numeroCuenta));
        return ResponseEntity.ok(cuenta.getSaldo());
    }

    @PostMapping("/{numeroCuenta}/pagar")
    public ResponseEntity<?> pagar(@PathVariable UUID numeroCuenta, @RequestParam Double monto) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
        
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new com.gestopago.microusuarios.exception.CuentaInactivaException("Operacion denegada: La cuenta esta inactiva y no puede realizar pagos.");
        }
        
        if (monto <= 0) {
            return ResponseEntity.badRequest().body("El monto a pagar debe ser mayor a 0");
        }
        
        if (cuenta.getSaldo() < monto) {
            return ResponseEntity.badRequest().body("Saldo insuficiente");
        }
        
        cuenta.setSaldo(cuenta.getSaldo() - monto);
        cuentaRepository.save(cuenta);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Pago realizado exitosamente", "nuevoSaldo", cuenta.getSaldo()));
    }

    @PostMapping("/{numeroCuenta}/recargar")
    public ResponseEntity<?> recargar(@PathVariable UUID numeroCuenta, @RequestParam Double monto) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
        
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new com.gestopago.microusuarios.exception.CuentaInactivaException("Operacion denegada: La cuenta esta inactiva y no puede recibir recargas.");
        }
        
        if (monto <= 0) {
            return ResponseEntity.badRequest().body("El monto a recargar debe ser mayor a 0");
        }
        
        cuenta.setSaldo(cuenta.getSaldo() + monto);
        cuentaRepository.save(cuenta);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Recarga exitosa", "nuevoSaldo", cuenta.getSaldo()));
    }

    @GetMapping("/{numeroCuenta}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable UUID numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
                
        return ResponseEntity.ok(java.util.Map.of(
            "numeroCuenta", numeroCuenta,
            "estatus", Boolean.TRUE.equals(cuenta.getActiva()) ? "ACTIVA" : "INACTIVA",
            "historial", java.util.List.of(
                "Transaccion 1 - Abono de apertura",
                "Transaccion 2 - Consulta de saldo"
            )
        ));
    }
}
