package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
@PreAuthorize("hasRole('ADMIN')")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public ResponseEntity<Cuenta> obtenerCuenta(@RequestParam(required = false) Optional<UUID> numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            throw new BadRequestException("El parámetro 'numeroCuenta' es requerido en la petición.");
        }
        return ResponseEntity.ok(cuentaService.obtenerCuenta(numeroCuenta.get()));
    }
    
    @GetMapping("/activas")
    public ResponseEntity<List<Cuenta>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }
    
    @GetMapping("/saldo")
    public ResponseEntity<Double> obtenerSaldo(@RequestParam(required = false) Optional<UUID> numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            throw new BadRequestException("El parámetro 'numeroCuenta' es requerido en la petición.");
        }
        return ResponseEntity.ok(cuentaService.obtenerSaldo(numeroCuenta.get()));
    }

    @PostMapping("/pagar")
    public ResponseEntity<Map<String, Object>> pagar(
            @RequestParam(required = false) Optional<UUID> numeroCuenta,
            @RequestParam(required = false) Optional<Double> monto) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            throw new BadRequestException("El parámetro 'numeroCuenta' es requerido en la petición.");
        }
        if (monto == null || monto.isEmpty()) {
            throw new BadRequestException("El parámetro 'monto' es requerido en la petición.");
        }
        return ResponseEntity.ok(cuentaService.pagar(numeroCuenta.get(), monto.get()));
    }

    @PostMapping("/recargar")
    public ResponseEntity<Map<String, Object>> recargar(
            @RequestParam(required = false) Optional<UUID> numeroCuenta,
            @RequestParam(required = false) Optional<Double> monto) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            throw new BadRequestException("El parámetro 'numeroCuenta' es requerido en la petición.");
        }
        if (monto == null || monto.isEmpty()) {
            throw new BadRequestException("El parámetro 'monto' es requerido en la petición.");
        }
        return ResponseEntity.ok(cuentaService.recargar(numeroCuenta.get(), monto.get()));
    }

    @GetMapping("/historial")
    public ResponseEntity<Map<String, Object>> obtenerHistorial(@RequestParam(required = false) Optional<UUID> numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            throw new BadRequestException("El parámetro 'numeroCuenta' es requerido en la petición.");
        }
        return ResponseEntity.ok(cuentaService.obtenerHistorial(numeroCuenta.get()));
    }
}
