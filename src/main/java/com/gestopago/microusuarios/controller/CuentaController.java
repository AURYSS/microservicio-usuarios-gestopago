package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.*;
import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cuentas")
@PreAuthorize("hasRole('ADMIN')")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping("/consultar")
    public ResponseEntity<ApiResponse<CuentaResponse>> obtenerCuenta(@Valid @RequestBody CuentaConsultaRequest request) {
        Cuenta cuenta = cuentaService.obtenerCuenta(request.getNumeroCuenta());
        CuentaResponse response = CuentaResponse.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .activa(cuenta.getActiva())
                .build();
        return ResponseEntity.ok(ApiResponse.exito("Cuenta recuperada exitosamente", response));
    }
    
    @GetMapping("/activas")
    public ResponseEntity<ApiResponse<List<CuentaResponse>>> obtenerCuentasActivas() {
        List<CuentaResponse> response = cuentaService.obtenerCuentasActivas().stream()
                .map(c -> CuentaResponse.builder()
                        .id(c.getId())
                        .numeroCuenta(c.getNumeroCuenta())
                        .saldo(c.getSaldo())
                        .activa(c.getActiva())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.exito("Listado de cuentas activas recuperado exitosamente", response));
    }
    
    @PostMapping("/saldo")
    public ResponseEntity<ApiResponse<Double>> obtenerSaldo(@Valid @RequestBody CuentaConsultaRequest request) {
        Double saldo = cuentaService.obtenerSaldo(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.exito("Saldo consultado exitosamente", saldo));
    }

    @PostMapping("/pagar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pagar(@Valid @RequestBody TransaccionRequest request) {
        Map<String, Object> resultado = cuentaService.pagar(request.getNumeroCuenta(), request.getMonto());
        return ResponseEntity.ok(ApiResponse.exito("Pago procesado exitosamente", resultado));
    }

    @PostMapping("/recargar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recargar(@Valid @RequestBody TransaccionRequest request) {
        Map<String, Object> resultado = cuentaService.recargar(request.getNumeroCuenta(), request.getMonto());
        return ResponseEntity.ok(ApiResponse.exito("Recarga procesada exitosamente", resultado));
    }

    @PostMapping("/historial")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerHistorial(@Valid @RequestBody CuentaConsultaRequest request) {
        Map<String, Object> historial = cuentaService.obtenerHistorial(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.exito("Historial recuperado exitosamente", historial));
    }
}
