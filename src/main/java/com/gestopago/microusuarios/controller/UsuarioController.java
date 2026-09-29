package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.exception.ResourceNotFoundException;
import com.gestopago.microusuarios.exception.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<Usuario> obtenerPerfil() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                
        return ResponseEntity.ok(usuario);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuarioPorId(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return ResponseEntity.ok(usuario);
    }
    
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> actualizarPassword(@PathVariable Long id, @RequestBody String newPassword) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                
        if (newPassword == null || newPassword.length() < 8 || 
            !newPassword.matches(".*[A-Z].*") || 
            !newPassword.matches(".*[a-z].*") || 
            !newPassword.matches(".*\\d.*") || 
            !newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new BadRequestException("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
        }
        
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok().build();
    }
}
