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
}
