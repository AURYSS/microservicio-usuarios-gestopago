package com.gestopago.microusuarios.repository;

import com.gestopago.microusuarios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correo);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByFechaRegistroBetween(LocalDateTime start, LocalDateTime end);
    Optional<Cliente> findByCuentasNumeroCuenta(UUID numeroCuenta);
}
