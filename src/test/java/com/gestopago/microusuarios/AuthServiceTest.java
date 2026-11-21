package com.gestopago.microusuarios;

import com.gestopago.microusuarios.dto.AuthResponse;
import com.gestopago.microusuarios.dto.LoginRequest;
import com.gestopago.microusuarios.dto.ValidarSesionResponse;
import com.gestopago.microusuarios.dto.Verify2FaRequest;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.security.JwtService;
import com.gestopago.microusuarios.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .correo("cliente@gestopago.com")
                .password("encodedPass")
                .role("CLIENTE")
                .activo(true)
                .sesionActiva(false)
                .build();
    }

    @Test
    @DisplayName("Paso 1: Iniciar login genera código 2FA y tempToken")
    void testIniciarLoginExitoso() {
        LoginRequest request = new LoginRequest();
        request.setUsername("cliente@gestopago.com");
        request.setPassword("Password123!");
        request.setDispositivo("MOVIL");

        when(usuarioRepository.findByCorreo("cliente@gestopago.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        AuthResponse response = authService.iniciarLogin(request);

        assertNotNull(response);
        assertTrue(response.isExito());
        assertTrue(response.isRequiere2Fa());
        assertNotNull(response.getTempToken());
        assertNotNull(response.getCodigo2FaDev());
        assertEquals(6, response.getCodigo2FaDev().length());
        assertEquals("MOVIL", response.getDispositivo());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("Paso 2: Verificar 2FA exitoso y emisión de JWT para Móvil")
    void testVerificar2FaExitoso() {
        usuario.setTwoFactorCodigo("123456");
        usuario.setTwoFactorExpiracion(LocalDateTime.now().plusMinutes(5));
        usuario.setTwoFactorTempToken("temp-uuid-123");
        usuario.setTwoFactorDispositivo("MOVIL");

        Verify2FaRequest request = Verify2FaRequest.builder()
                .tempToken("temp-uuid-123")
                .codigo("123456")
                .build();

        when(usuarioRepository.findByTwoFactorTempToken("temp-uuid-123")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(anyMap(), eq("cliente@gestopago.com"))).thenReturn("jwt.token.mock");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        AuthResponse response = authService.verificar2Fa(request);

        assertNotNull(response);
        assertTrue(response.isExito());
        assertFalse(response.isRequiere2Fa());
        assertEquals("jwt.token.mock", response.getToken());
        assertEquals("MOVIL", response.getDispositivo());
        assertTrue(usuario.getSesionActiva());
        assertEquals("MOVIL", usuario.getDispositivoActivo());
        assertNotNull(usuario.getSessionIdActivo());
    }

    @Test
    @DisplayName("Paso 2: Código 2FA incorrecto lanza BadRequestException")
    void testVerificar2FaCodigoIncorrecto() {
        usuario.setTwoFactorCodigo("123456");
        usuario.setTwoFactorExpiracion(LocalDateTime.now().plusMinutes(5));
        usuario.setTwoFactorTempToken("temp-uuid-123");

        Verify2FaRequest request = Verify2FaRequest.builder()
                .tempToken("temp-uuid-123")
                .codigo("999999")
                .build();

        when(usuarioRepository.findByTwoFactorTempToken("temp-uuid-123")).thenReturn(Optional.of(usuario));

        assertThrows(BadRequestException.class, () -> authService.verificar2Fa(request));
    }

    @Test
    @DisplayName("Control de concurrencia: Detecta sesión activa previa en Móvil al intentar ingresar desde Web")
    void testConflictoSesionActivaEnOtroDispositivo() {
        // Usuario ya tiene sesión activa en MOVIL
        usuario.setSesionActiva(true);
        usuario.setDispositivoActivo("MOVIL");
        usuario.setSessionIdActivo("session-movil-123");

        // Intento de validación 2FA desde WEB
        usuario.setTwoFactorCodigo("654321");
        usuario.setTwoFactorExpiracion(LocalDateTime.now().plusMinutes(5));
        usuario.setTwoFactorTempToken("temp-web-456");
        usuario.setTwoFactorDispositivo("WEB");

        Verify2FaRequest request = Verify2FaRequest.builder()
                .tempToken("temp-web-456")
                .codigo("654321")
                .forzarCierre(false) // No fuerza cierre
                .build();

        when(usuarioRepository.findByTwoFactorTempToken("temp-web-456")).thenReturn(Optional.of(usuario));

        AuthResponse response = authService.verificar2Fa(request);

        assertNotNull(response);
        assertFalse(response.isExito());
        assertTrue(response.isSesionActivaEnOtroDispositivo());
        assertEquals("MOVIL", response.getDispositivoActivo());
        assertEquals("WEB", response.getDispositivo());
        assertNull(response.getToken()); // No emite token hasta confirmar cierre
    }

    @Test
    @DisplayName("Control de concurrencia: Forzar cierre invalida sesión anterior y activa la nueva")
    void testForzarCierreSesionPrevia() {
        // Usuario con sesión en MOVIL
        usuario.setSesionActiva(true);
        usuario.setDispositivoActivo("MOVIL");
        usuario.setSessionIdActivo("session-movil-vieja");

        // Intento de WEB forzando cierre
        usuario.setTwoFactorCodigo("654321");
        usuario.setTwoFactorExpiracion(LocalDateTime.now().plusMinutes(5));
        usuario.setTwoFactorTempToken("temp-web-456");
        usuario.setTwoFactorDispositivo("WEB");

        Verify2FaRequest request = Verify2FaRequest.builder()
                .tempToken("temp-web-456")
                .codigo("654321")
                .forzarCierre(true) // Usuario confirma cerrar la sesión de MOVIL
                .build();

        when(usuarioRepository.findByTwoFactorTempToken("temp-web-456")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(anyMap(), eq("cliente@gestopago.com"))).thenReturn("jwt.token.nuevo.web");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        AuthResponse response = authService.verificar2Fa(request);

        assertNotNull(response);
        assertTrue(response.isExito());
        assertEquals("jwt.token.nuevo.web", response.getToken());
        assertEquals("WEB", response.getDispositivo());
        assertEquals("WEB", usuario.getDispositivoActivo());
        assertNotEquals("session-movil-vieja", usuario.getSessionIdActivo());
    }

    @Test
    @DisplayName("Validación de sesión: Rechaza token con sesión revocada")
    void testValidarSesionRevocada() {
        usuario.setSesionActiva(true);
        usuario.setSessionIdActivo("sesion-actual-web");
        usuario.setDispositivoActivo("WEB");

        when(jwtService.isTokenExpired("token.antiguo")).thenReturn(false);
        when(jwtService.extractUsername("token.antiguo")).thenReturn("cliente@gestopago.com");
        when(jwtService.extractSessionId("token.antiguo")).thenReturn("sesion-vieja-movil");
        when(usuarioRepository.findByCorreo("cliente@gestopago.com")).thenReturn(Optional.of(usuario));

        ValidarSesionResponse response = authService.validarSesion("Bearer token.antiguo");

        assertNotNull(response);
        assertFalse(response.isValido());
        assertTrue(response.getMensaje().contains("revocada"));
    }

    @Test
    @DisplayName("Logout: Invalida la sesión activa")
    void testLogout() {
        usuario.setSesionActiva(true);
        usuario.setDispositivoActivo("MOVIL");
        usuario.setSessionIdActivo("session-activa-123");

        when(usuarioRepository.findByCorreo("cliente@gestopago.com")).thenReturn(Optional.of(usuario));

        authService.logout("cliente@gestopago.com");

        assertFalse(usuario.getSesionActiva());
        assertNull(usuario.getDispositivoActivo());
        assertNull(usuario.getSessionIdActivo());
        verify(usuarioRepository).save(usuario);
    }
}
