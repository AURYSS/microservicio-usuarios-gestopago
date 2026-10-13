package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
@PreAuthorize("hasRole('ADMIN')")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable UUID numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerCuenta(numeroCuenta));
    }
    
    @GetMapping("/activas")
    public ResponseEntity<List<Cuenta>> obtenerCuentasActivas() {
        return ResponseEntity.ok(cuentaService.obtenerCuentasActivas());
    }
    
    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<Double> obtenerSaldo(@PathVariable UUID numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerSaldo(numeroCuenta));
    }

    @PostMapping("/{numeroCuenta}/pagar")
    public ResponseEntity<Map<String, Object>> pagar(@PathVariable UUID numeroCuenta, @RequestParam Double monto) {
        return ResponseEntity.ok(cuentaService.pagar(numeroCuenta, monto));
    }

    @PostMapping("/{numeroCuenta}/recargar")
    public ResponseEntity<Map<String, Object>> recargar(@PathVariable UUID numeroCuenta, @RequestParam Double monto) {
        return ResponseEntity.ok(cuentaService.recargar(numeroCuenta, monto));
    }

    @GetMapping("/{numeroCuenta}/historial")
    public ResponseEntity<Map<String, Object>> obtenerHistorial(@PathVariable UUID numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerHistorial(numeroCuenta));
    }
}
