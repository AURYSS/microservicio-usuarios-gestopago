package com.gestopago.microusuarios.repository;
import com.gestopago.microusuarios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correo);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByFechaRegistroBetween(LocalDateTime start, LocalDateTime end);
}
