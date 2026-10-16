package com.gestopago.microusuarios.repository;

import com.gestopago.microusuarios.dto.UsuarioFiltroRequest;
import com.gestopago.microusuarios.entity.Usuario;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecifications {

    public static Specification<Usuario> conFiltros(UsuarioFiltroRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getId() != null) {
                predicates.add(cb.equal(root.get("id"), filtro.getId()));
            }

            if (filtro.getCorreo() != null && !filtro.getCorreo().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("correo")), "%" + filtro.getCorreo().trim().toLowerCase() + "%"));
            }

            if (filtro.getRole() != null && !filtro.getRole().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("role")), filtro.getRole().trim().toUpperCase()));
            }

            if (filtro.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), filtro.getActivo()));
            }

            if (filtro.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaInicio()));
            }

            if (filtro.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), filtro.getFechaFin()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
