package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.*;
import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cuentas")
@PreAuthorize("hasRole('ADMIN') or hasRole('CLIENTE')")
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
                .pinAutorizacion(cuenta.getPinAutorizacion())
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
                        .pinAutorizacion(c.getPinAutorizacion())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.exito("Listado de cuentas activas recuperado exitosamente", response));
    }
    
    @PostMapping("/saldo")
    public ResponseEntity<ApiResponse<Double>> obtenerSaldo(@Valid @RequestBody CuentaConsultaRequest request) {
        Double saldo = cuentaService.obtenerSaldo(request.getNumeroCuenta());
        return ResponseEntity.ok(ApiResponse.exito("Saldo consultado exitosamente", saldo));
    }

    @PostMapping("/validar-pago")
    @Operation(summary = "Validar datos de la izquierda del diagrama (Cuenta Activa, Saldo Cuenta, Pin Autorización, Validar Teléfono/Ref) y generar datos del cuadro central")
    public ResponseEntity<ApiResponse<PagoServicioCuentaResponse>> validarPago(
            @Valid @RequestBody ValidarPagoCuentaRequest request) {
        PagoServicioCuentaResponse resultado = cuentaService.validarPagoServicio(request);
        return ResponseEntity.ok(ApiResponse.exito("Validaciones de cuenta y referencia exitosas", resultado));
    }

    @PostMapping("/pagar-servicio")
    @Operation(summary = "Flujo completo: Validar Cuenta (Saldo, Activa, Pin, Ref) -> Armar Cuadro Central -> Enviar a api-gesto-pago (Firmar, Token, sendTx) -> Descontar saldo")
    public ResponseEntity<ApiResponse<PagoServicioCuentaResponse>> pagarServicio(
            @Valid @RequestBody ValidarPagoCuentaRequest request) {
        PagoServicioCuentaResponse resultado = cuentaService.pagarServicioConCuenta(request);
        return ResponseEntity.ok(ApiResponse.exito("Pago de servicio procesado exitosamente", resultado));
    }

    @PostMapping("/conciliar-pago")
    @Operation(summary = "Conciliar una operación en estado incierto (EN_PROCESO / timeout) contra el proveedor y ajustar el saldo si no se aplicó")
    public ResponseEntity<ApiResponse<PagoServicioCuentaResponse>> conciliarPago(
            @Valid @RequestBody ValidarPagoCuentaRequest request) {
        PagoServicioCuentaResponse resultado = cuentaService.conciliarPagoServicio(request);
        return ResponseEntity.ok(ApiResponse.exito("Conciliación de pago procesada exitosamente", resultado));
    }

    @PostMapping("/pagar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pagar(@Valid @RequestBody TransaccionRequest request) {
        Map<String, Object> resultado;
        if (request.esPagoDeServicioCompleto()) {
            resultado = cuentaService.pagar(request.toValidarPagoCuentaRequest());
        } else {
            resultado = cuentaService.pagar(request.getNumeroCuenta(), request.getMonto());
        }
        return ResponseEntity.ok(ApiResponse.exito("Pago procesado exitosamente", resultado));
    }

    @PatchMapping("/pin")
    @Operation(summary = "Actualizar el PIN de autorización de una cuenta")
    public ResponseEntity<ApiResponse<Map<String, Object>>> actualizarPin(@RequestBody Map<String, String> body) {
        java.util.UUID numeroCuenta = java.util.UUID.fromString(body.get("numeroCuenta"));
        String nuevoPin = body.getOrDefault("pinAutorizacion", body.get("pin"));
        Map<String, Object> resultado = cuentaService.actualizarPin(numeroCuenta, nuevoPin);
        return ResponseEntity.ok(ApiResponse.exito("PIN actualizado exitosamente", resultado));
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
