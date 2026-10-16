package com.gestopago.microusuarios;

import com.gestopago.microusuarios.dto.*;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.exception.CorreoDuplicadoException;
import com.gestopago.microusuarios.exception.UsuarioNoEncontradoException;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .correo("admin@gestopago.com")
                .password("encodedPassword")
                .role("ADMIN")
                .activo(true)
                .build();
    }

    @Test
    void testCrearUsuarioExitoso() {
        UsuarioCreateRequest request = UsuarioCreateRequest.builder()
                .correo("nuevo@gestopago.com")
                .password("Password123!")
                .role("CLIENTE")
                .build();

        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        UsuarioResponse response = usuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("nuevo@gestopago.com", response.getCorreo());
        assertEquals("CLIENTE", response.getRole());
        assertTrue(response.getActivo());
    }

    @Test
    void testCrearUsuarioCorreoDuplicado() {
        UsuarioCreateRequest request = UsuarioCreateRequest.builder()
                .correo("admin@gestopago.com")
                .password("Password123!")
                .role("ADMIN")
                .build();

        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.of(usuario));

        assertThrows(CorreoDuplicadoException.class, () -> usuarioService.crearUsuario(request));
    }

    @Test
    void testActualizarUsuarioNoEncontrado() {
        UsuarioUpdateRequest request = UsuarioUpdateRequest.builder()
                .id(99L)
                .correo("cambio@gestopago.com")
                .build();

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> usuarioService.actualizarUsuario(request));
    }

    @Test
    void testBajaLogicaUsuario() {
        UsuarioDeleteRequest request = UsuarioDeleteRequest.builder()
                .id(1L)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        usuarioService.bajaLogicaUsuario(request);

        assertFalse(usuario.getActivo());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBuscarUsuariosConFiltro() {
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .role("ADMIN")
                .build();

        when(usuarioRepository.findAll(any(Specification.class))).thenReturn(List.of(usuario));

        List<UsuarioResponse> resultados = usuarioService.buscarUsuarios(filtro);

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals("admin@gestopago.com", resultados.get(0).getCorreo());
    }
}
