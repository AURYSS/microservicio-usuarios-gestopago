package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.ApiResponse;
import com.gestopago.microusuarios.dto.ClienteResponse;
import com.gestopago.microusuarios.dto.ClienteUpdateRequest;
import com.gestopago.microusuarios.entity.Cliente;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.ResourceNotFoundException;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
public class ClienteAutoservicioController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;

    public ClienteAutoservicioController(UsuarioRepository usuarioRepository, ClienteService clienteService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
    }

    private Cliente getAuthenticatedCliente(Authentication authentication) {
        String correo = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + correo));
        if (usuario.getCliente() == null) {
            throw new ResourceNotFoundException("El usuario no tiene un cliente asociado");
        }
        return usuario.getCliente();
    }

    // 1. GET - Obtener mi perfil (Response DTO)
    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerMiPerfil(Authentication authentication) {
        Cliente cliente = getAuthenticatedCliente(authentication);
        ClienteResponse response = clienteService.mapToResponse(cliente);
        return ResponseEntity.ok(ApiResponse.exito("Perfil recuperado exitosamente", response));
    }

    // 2. PATCH - Actualizar mi perfil parcialmente mediante DTO
    @PatchMapping("/perfil")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarMiPerfil(Authentication authentication, @RequestBody ClienteUpdateRequest updates) {
        Cliente cliente = getAuthenticatedCliente(authentication);
        updates.setId(cliente.getId());
        ClienteResponse response = clienteService.actualizarClienteDTO(updates);
        return ResponseEntity.ok(ApiResponse.exito("Perfil actualizado exitosamente", response));
    }

    // 3. POST - Reconocimiento facial (mock innovación)
    @PostMapping(value = "/reconocimiento-facial", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> verificarIdentidad(Authentication authentication, @RequestPart("imagen") MultipartFile imagen) {
        Cliente cliente = getAuthenticatedCliente(authentication);
        
        if (imagen == null || imagen.isEmpty()) {
            throw new BadRequestException("El archivo de imagen es obligatorio para la verificación facial.");
        }
        
        Map<String, String> data = Map.of(
            "status", "success",
            "mensaje", "Reconocimiento facial exitoso para el cliente: " + cliente.getNombre(),
            "confianza", "98.5%"
        );
        return ResponseEntity.ok(ApiResponse.exito("Verificación facial completada", data));
    }
}
