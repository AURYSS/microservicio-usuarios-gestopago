package com.gestopago.microusuarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "cuentas")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Cuenta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, updatable = false, nullable = false)
    private UUID numeroCuenta;
    
    @Min(value = 0, message = "El saldo no puede ser negativo")
    private Double saldo;
    
    @Builder.Default
    private Boolean activa = true;
    
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"cuentas", "usuario", "domicilio"})
    private Cliente cliente;
    
    @PrePersist
    protected void onCreate() {
        if (numeroCuenta == null) {
            numeroCuenta = UUID.randomUUID();
        }
        if (saldo == null) {
            saldo = 0.0;
        }
    }
}
