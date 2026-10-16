package com.gestopago.microusuarios.service.impl;

import com.gestopago.microusuarios.dto.*;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.CorreoDuplicadoException;
import com.gestopago.microusuarios.exception.UsuarioNoEncontradoException;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import com.gestopago.microusuarios.repository.UsuarioSpecifications;
import com.gestopago.microusuarios.service.UsuarioService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioCreateRequest request) {
        if (usuarioRepository.findByCorreo(request.getCorreo().trim().toLowerCase()).isPresent()) {
            throw new CorreoDuplicadoException("El correo ya se encuentra registrado: " + request.getCorreo());
        }

        Usuario usuario = Usuario.builder()
                .correo(request.getCorreo().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().trim().toUpperCase())
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("Operación denegada: No se puede modificar la información de un usuario inactivo.");
        }

        if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            String nuevoCorreo = request.getCorreo().trim().toLowerCase();
            if (!nuevoCorreo.equalsIgnoreCase(usuario.getCorreo())) {
                Optional<Usuario> existente = usuarioRepository.findByCorreo(nuevoCorreo);
                if (existente.isPresent() && !existente.get().getId().equals(usuario.getId())) {
                    throw new CorreoDuplicadoException("El correo ya está en uso por otro usuario: " + nuevoCorreo);
                }
                usuario.setCorreo(nuevoCorreo);
            }
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            usuario.setRole(request.getRole().trim().toUpperCase());
        }

        if (request.getActivo() != null) {
            usuario.setActivo(request.getActivo());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void bajaLogicaUsuario(UsuarioDeleteRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + request.getId()));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscarUsuarios(UsuarioFiltroRequest filtro) {
        Specification<Usuario> spec = UsuarioSpecifications.conFiltros(filtro);
        return usuarioRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerMiPerfil(String correoAutenticado) {
        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado con correo: " + correoAutenticado));
        return mapToResponse(usuario);
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .correo(usuario.getCorreo())
                .role(usuario.getRole())
                .activo(usuario.getActivo())
                .clienteId(usuario.getCliente() != null ? usuario.getCliente().getId() : null)
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }
}
