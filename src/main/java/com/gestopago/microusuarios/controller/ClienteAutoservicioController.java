package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.entity.Cliente;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.service.ClienteService;
import com.gestopago.microusuarios.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (usuario.getCliente() == null) {
            throw new ResourceNotFoundException("El usuario no tiene un cliente asociado");
        }
        return usuario.getCliente();
    }

    // 1. GET - Obtener mi perfil
    @GetMapping("/perfil")
    public ResponseEntity<Cliente> obtenerMiPerfil(Authentication authentication) {
        return ResponseEntity.ok(getAuthenticatedCliente(authentication));
    }

    // 2. PATCH - Actualizar mi perfil parcialmente (ej. solo el teléfono)
    @PatchMapping("/perfil")
    public ResponseEntity<Cliente> actualizarMiPerfil(Authentication authentication, @RequestBody Map<String, Object> updates) {
        Cliente cliente = getAuthenticatedCliente(authentication);
        
        // Simulación simple de actualización parcial
        if (updates.containsKey("telefonoMovil")) {
            cliente.setTelefonoMovil(updates.get("telefonoMovil").toString());
        }
        if (updates.containsKey("telefonoAlternativo")) {
            cliente.setTelefonoAlternativo(updates.get("telefonoAlternativo").toString());
        }
        
        // Se usaría el servicio para guardar y validar
        Cliente actualizado = clienteService.actualizarCliente(cliente.getId(), cliente);
        return ResponseEntity.ok(actualizado);
    }

    // 3. POST - Reconocimiento facial (mock)
    @PostMapping(value = "/reconocimiento-facial", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> verificarIdentidad(Authentication authentication, @RequestParam("imagen") MultipartFile imagen) {
        // Aquí iría la lógica de integración con un servicio de IA (ej. AWS Rekognition o similar)
        // Por ahora, simulamos una validación exitosa
        Cliente cliente = getAuthenticatedCliente(authentication);
        
        if (imagen.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen es requerida"));
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "mensaje", "Reconocimiento facial exitoso para el cliente: " + cliente.getNombre(),
            "confianza", "98.5%"
        ));
    }
}
