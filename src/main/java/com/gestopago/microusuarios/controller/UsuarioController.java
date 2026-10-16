package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.*;
import com.gestopago.microusuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse response = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(ApiResponse.exito("Usuario registrado exitosamente", response), HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(@Valid @RequestBody UsuarioUpdateRequest request) {
        UsuarioResponse response = usuarioService.actualizarUsuario(request);
        return ResponseEntity.ok(ApiResponse.exito("Usuario actualizado exitosamente", response));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> bajaLogicaUsuario(@Valid @RequestBody UsuarioDeleteRequest request) {
        usuarioService.bajaLogicaUsuario(request);
        return ResponseEntity.ok(ApiResponse.exito("Usuario desactivado exitosamente (baja lógica)"));
    }

    @PostMapping("/buscar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> buscarUsuarios(
            @RequestBody(required = false) UsuarioFiltroRequest filtro) {
        List<UsuarioResponse> resultados = usuarioService.buscarUsuarios(filtro);
        return ResponseEntity.ok(ApiResponse.exito("Búsqueda ejecutada exitosamente", resultados));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerMiPerfil(Authentication authentication) {
        UsuarioResponse response = usuarioService.obtenerMiPerfil(authentication.getName());
        return ResponseEntity.ok(ApiResponse.exito("Perfil recuperado exitosamente", response));
    }
}
