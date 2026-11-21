package com.gestopago.microusuarios.service.impl;

import com.gestopago.microusuarios.dto.AuthResponse;
import com.gestopago.microusuarios.dto.CerrarSesionPreviaRequest;
import com.gestopago.microusuarios.dto.LoginRequest;
import com.gestopago.microusuarios.dto.ValidarSesionResponse;
import com.gestopago.microusuarios.dto.Verify2FaRequest;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.UsuarioInactivoException;
import com.gestopago.microusuarios.exception.UsuarioNoEncontradoException;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.security.JwtService;
import com.gestopago.microusuarios.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UsuarioRepository usuarioRepository,
                           JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse iniciarLogin(LoginRequest request) {
        String correoNormalizado = request.getUsername().trim().toLowerCase();

        // 1. Validar credenciales con Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correoNormalizado, request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + correoNormalizado));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("La cuenta de usuario está desactivada.");
        }

        String dispositivo = normalizarDispositivo(request.getDispositivo());

        // 2. Generar código OTP 2FA de 6 dígitos
        String codigo2Fa = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(5);
        String tempToken = UUID.randomUUID().toString();

        usuario.setTwoFactorCodigo(codigo2Fa);
        usuario.setTwoFactorExpiracion(expiracion);
        usuario.setTwoFactorTempToken(tempToken);
        usuario.setTwoFactorDispositivo(dispositivo);
        usuarioRepository.save(usuario);

        log.info("=================================================================");
        log.info("[2FA] CODIGO GENERADO para [{}]: {}", correoNormalizado, codigo2Fa);
        log.info("Dispositivo solicitante: {} | Expira en: {}", dispositivo, expiracion);
        log.info("=================================================================");

        return AuthResponse.builder()
                .exito(true)
                .requiere2Fa(true)
                .tempToken(tempToken)
                .codigo2FaDev(codigo2Fa)
                .correoEnmascarado(enmascararCorreo(correoNormalizado))
                .dispositivo(dispositivo)
                .mensaje("Código de verificación 2FA generado. Ingréselo para continuar.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verificar2Fa(Verify2FaRequest request) {
        Usuario usuario = usuarioRepository.findByTwoFactorTempToken(request.getTempToken())
                .orElseThrow(() -> new BadRequestException("Token temporal 2FA inválido o expirado."));

        if (usuario.getTwoFactorExpiracion() == null || usuario.getTwoFactorExpiracion().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El código 2FA ha expirado. Por favor, inicie sesión nuevamente.");
        }

        if (usuario.getTwoFactorCodigo() == null || !usuario.getTwoFactorCodigo().equals(request.getCodigo().trim())) {
            throw new BadRequestException("El código de verificación 2FA es incorrecto.");
        }

        String nuevoDispositivo = usuario.getTwoFactorDispositivo() != null
                ? usuario.getTwoFactorDispositivo()
                : "MOVIL";

        // 3. Control de concurrencia: Verificar si ya existe una sesion activa (en cualquier dispositivo o en el mismo)
        boolean sesionPreviaActiva = Boolean.TRUE.equals(usuario.getSesionActiva())
                && usuario.getSessionIdActivo() != null;

        if (sesionPreviaActiva && !Boolean.TRUE.equals(request.getForzarCierre())) {
            String dispPrevio = usuario.getDispositivoActivo() != null ? usuario.getDispositivoActivo() : "OTRO";
            log.warn("[ALERTA] Conflicto de sesion: Usuario {} ya tiene sesion activa en [{}] e intenta ingresar desde [{}]",
                    usuario.getCorreo(), dispPrevio, nuevoDispositivo);

            String descDispositivo = "WEB".equalsIgnoreCase(dispPrevio)
                    ? "otro navegador o ventana Web"
                    : "un dispositivo movil";

            return AuthResponse.builder()
                    .exito(false)
                    .sesionActivaEnOtroDispositivo(true)
                    .dispositivoActivo(dispPrevio)
                    .dispositivo(nuevoDispositivo)
                    .tempToken(request.getTempToken())
                    .mensaje("Existe una sesion activa en " + descDispositivo + ". Confirme si desea cerrar la sesion previa para continuar aqui.")
                    .build();
        }

        // 4. Si se autoriza o no hay conflicto, transferir o renovar la sesión activa
        String nuevoSessionId = UUID.randomUUID().toString();
        usuario.setSesionActiva(true);
        usuario.setDispositivoActivo(nuevoDispositivo);
        usuario.setSessionIdActivo(nuevoSessionId);
        usuario.setFechaUltimoAcceso(LocalDateTime.now());

        // Limpiar datos de 2FA ya utilizados
        usuario.setTwoFactorCodigo(null);
        usuario.setTwoFactorExpiracion(null);
        usuario.setTwoFactorTempToken(null);
        usuario.setTwoFactorDispositivo(null);
        usuarioRepository.save(usuario);

        // 5. Generar token JWT con claims de sesión única
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", nuevoSessionId);
        claims.put("dispositivo", nuevoDispositivo);
        claims.put("role", usuario.getRole());
        claims.put("userId", usuario.getId());
        if (usuario.getCliente() != null) {
            claims.put("clienteId", usuario.getCliente().getId());
        }

        String token = jwtService.generateToken(claims, usuario.getCorreo());

        log.info("[OK] Sesion iniciada con exito para [{}] en dispositivo [{}] (SessionId: {})",
                usuario.getCorreo(), nuevoDispositivo, nuevoSessionId);

        return AuthResponse.builder()
                .exito(true)
                .requiere2Fa(false)
                .token(token)
                .tokenType("Bearer")
                .sessionId(nuevoSessionId)
                .usuarioId(usuario.getId())
                .clienteId(usuario.getCliente() != null ? usuario.getCliente().getId() : null)
                .correo(usuario.getCorreo())
                .rol(usuario.getRole())
                .dispositivo(nuevoDispositivo)
                .mensaje("Autenticación y 2FA completados exitosamente.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse cerrarSesionPrevia(CerrarSesionPreviaRequest request) {
        Usuario usuario = usuarioRepository.findByTwoFactorTempToken(request.getTempToken())
                .orElseThrow(() -> new BadRequestException("Token temporal 2FA inválido o expirado."));

        if (usuario.getTwoFactorCodigo() == null) {
            throw new BadRequestException("No hay una verificación 2FA pendiente.");
        }

        // Reutilizar verificar2Fa con forzarCierre=true
        Verify2FaRequest verifyRequest = Verify2FaRequest.builder()
                .tempToken(request.getTempToken())
                .codigo(usuario.getTwoFactorCodigo())
                .forzarCierre(true)
                .build();

        return verificar2Fa(verifyRequest);
    }

    @Override
    @Transactional
    public void logout(String correo) {
        if (correo == null || correo.isBlank()) {
            return;
        }
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            usuario.setSesionActiva(false);
            usuario.setDispositivoActivo(null);
            usuario.setSessionIdActivo(null);
            usuarioRepository.save(usuario);
            log.info("[SESION] Sesion cerrada para usuario: {}", correo);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ValidarSesionResponse validarSesion(String token) {
        try {
            if (token == null || token.isBlank()) {
                return ValidarSesionResponse.builder().valido(false).mensaje("Token vacío").build();
            }
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (jwtService.isTokenExpired(token)) {
                return ValidarSesionResponse.builder().valido(false).mensaje("Token expirado").build();
            }

            String correo = jwtService.extractUsername(token);
            String tokenSessionId = jwtService.extractSessionId(token);
            String tokenDispositivo = jwtService.extractDispositivo(token);

            Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
            if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
                return ValidarSesionResponse.builder().valido(false).mensaje("Usuario inactivo o inexistente").build();
            }

            // Validar que siga siendo la sesión activa registrada
            if (tokenSessionId != null && (!Boolean.TRUE.equals(usuario.getSesionActiva())
                    || !tokenSessionId.equals(usuario.getSessionIdActivo()))) {
                return ValidarSesionResponse.builder()
                        .valido(false)
                        .mensaje("Sesión revocada: Se ha iniciado sesión en otro dispositivo")
                        .dispositivo(usuario.getDispositivoActivo())
                        .build();
            }

            return ValidarSesionResponse.builder()
                    .valido(true)
                    .correo(usuario.getCorreo())
                    .usuarioId(usuario.getId())
                    .clienteId(usuario.getCliente() != null ? usuario.getCliente().getId() : null)
                    .rol(usuario.getRole())
                    .dispositivo(tokenDispositivo != null ? tokenDispositivo : usuario.getDispositivoActivo())
                    .sessionId(tokenSessionId)
                    .mensaje("Sesión activa y válida")
                    .build();

        } catch (Exception e) {
            log.warn("Error al validar sesión con token: {}", e.getMessage());
            return ValidarSesionResponse.builder().valido(false).mensaje("Token inválido: " + e.getMessage()).build();
        }
    }

    private String normalizarDispositivo(String dispositivo) {
        if (dispositivo == null || dispositivo.isBlank()) {
            return "MOVIL";
        }
        return dispositivo.trim().equalsIgnoreCase("WEB") ? "WEB" : "MOVIL";
    }

    private String enmascararCorreo(String correo) {
        int atIndex = correo.indexOf('@');
        if (atIndex <= 2) {
            return correo;
        }
        return correo.substring(0, 2) + "***" + correo.substring(atIndex);
    }
}
