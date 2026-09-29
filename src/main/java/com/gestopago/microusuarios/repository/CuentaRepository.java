package com.gestopago.microusuarios.repository;
import com.gestopago.microusuarios.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByNumeroCuenta(UUID numeroCuenta);
    List<Cuenta> findByActivaTrue();
}
