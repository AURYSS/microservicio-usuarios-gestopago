package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.AuthResponse;
import com.gestopago.microusuarios.dto.CerrarSesionPreviaRequest;
import com.gestopago.microusuarios.dto.LoginRequest;
import com.gestopago.microusuarios.dto.ValidarSesionResponse;
import com.gestopago.microusuarios.dto.Verify2FaRequest;
import com.gestopago.microusuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación, 2FA y Control de Sesión", description = "Endpoints de login seguro, 2FA (OTP) y control de sesión única concurrente (Web vs Móvil)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Paso 1: Iniciar sesión y generar código 2FA", description = "Valida credenciales y genera el código OTP para el segundo factor de autenticación.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.iniciarLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-2fa")
    @Operation(summary = "Paso 2: Validar 2FA y verificar sesión única concurrente", description = "Valida el código OTP. Si existe una sesión activa en otro dispositivo (Móvil vs Web), devuelve 409 Conflict para confirmar el cierre de la sesión previa.")
    public ResponseEntity<AuthResponse> verify2Fa(@Valid @RequestBody Verify2FaRequest request) {
        AuthResponse response = authService.verificar2Fa(request);
        if (response.isSesionActivaEnOtroDispositivo()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cerrar-sesion-previa")
    @Operation(summary = "Confirmar traspaso de sesión y cerrar dispositivo anterior", description = "Invalida la sesión activa en el otro dispositivo y emite el nuevo token para el dispositivo actual.")
    public ResponseEntity<AuthResponse> cerrarSesionPrevia(@Valid @RequestBody CerrarSesionPreviaRequest request) {
        AuthResponse response = authService.cerrarSesionPrevia(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión activa", description = "Invalida la sesión activa del usuario.")
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication) {
        if (authentication != null) {
            authService.logout(authentication.getName());
        }
        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "Sesión finalizada correctamente."
        ));
    }

    @GetMapping("/validar-sesion")
    @Operation(summary = "Validar token y estado de sesión activa", description = "Permite a las apps frontend y al backend api-gesto-pago comprobar si el token sigue activo y no ha sido revocado.")
    public ResponseEntity<ValidarSesionResponse> validarSesion(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        ValidarSesionResponse response = authService.validarSesion(authorization);
        HttpStatus status = response.isValido() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/validar-token")
    @Operation(summary = "Validar token (POST)", description = "Alternativa por body para integración directa con api-gesto-pago.")
    public ResponseEntity<ValidarSesionResponse> validarTokenBody(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("token") : null;
        ValidarSesionResponse response = authService.validarSesion(token);
        HttpStatus status = response.isValido() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }
}
