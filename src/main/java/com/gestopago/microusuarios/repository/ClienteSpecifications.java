package com.gestopago.microusuarios.repository;

import com.gestopago.microusuarios.dto.ClienteFiltroRequest;
import com.gestopago.microusuarios.entity.Cliente;
import com.gestopago.microusuarios.entity.Cuenta;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClienteSpecifications {

    public static Specification<Cliente> conFiltros(ClienteFiltroRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtro.getId()));
            }

            if (filtro.getCurp() != null && !filtro.getCurp().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("curp")), filtro.getCurp().trim().toUpperCase()));
            }

            if (filtro.getRfc() != null && !filtro.getRfc().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("rfc")), filtro.getRfc().trim().toUpperCase()));
            }

            if (filtro.getCorreo() != null && !filtro.getCorreo().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("correo")), filtro.getCorreo().trim().toLowerCase()));
            }

            if (filtro.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
            }

            if (filtro.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaInicio()));
            }

            if (filtro.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), filtro.getFechaFin()));
            }

            if (filtro.getNumeroCuenta() != null) {
                Join<Cliente, Cuenta> cuentasJoin = root.join("cuentas", JoinType.INNER);
                predicates.add(cb.equal(cuentasJoin.get("numeroCuenta"), filtro.getNumeroCuenta()));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
