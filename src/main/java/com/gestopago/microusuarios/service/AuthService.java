package com.gestopago.microusuarios.service;

import com.gestopago.microusuarios.dto.AuthResponse;
import com.gestopago.microusuarios.dto.CerrarSesionPreviaRequest;
import com.gestopago.microusuarios.dto.LoginRequest;
import com.gestopago.microusuarios.dto.ValidarSesionResponse;
import com.gestopago.microusuarios.dto.Verify2FaRequest;

public interface AuthService {

    /**
     * Paso 1: Valida credenciales primarias y genera código 2FA.
     */
    AuthResponse iniciarLogin(LoginRequest request);

    /**
     * Paso 2: Valida el código 2FA y maneja el control de sesiones concurrentes (MOVIL vs WEB).
     */
    AuthResponse verificar2Fa(Verify2FaRequest request);

    /**
     * Cierra forzadamente la sesión previa en el otro dispositivo asociada al intento actual.
     */
    AuthResponse cerrarSesionPrevia(CerrarSesionPreviaRequest request);

    /**
     * Cierra la sesión activa del usuario autenticado.
     */
    void logout(String correo);

    /**
     * Valida si un token JWT recibido sigue siendo válido y pertenece a la sesión activa.
     */
    ValidarSesionResponse validarSesion(String token);
}
