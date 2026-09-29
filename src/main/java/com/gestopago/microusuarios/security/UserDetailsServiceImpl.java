package com.gestopago.microusuarios.security;

import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Usuario> user = usuarioRepository.findByCorreo(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
        
        Usuario u = user.get();

        return User.builder()
                .username(u.getCorreo())
                .password(u.getPassword())
                .roles(u.getRole() != null ? u.getRole() : "USER")
                .disabled(!u.getActivo())
                .build();
    }
}
