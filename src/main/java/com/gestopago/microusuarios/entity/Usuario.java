package com.gestopago.microusuarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String correo;
    
    @Column(nullable = false)
    private String password;
    
    private String role;
    
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;
    
    @UpdateTimestamp
    private LocalDateTime fechaActualizacion;
    
    @OneToOne
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"usuario", "cuentas", "domicilio"})
    private Cliente cliente;

    // --- Control de Sesión Única y 2FA ---
    @Builder.Default
    @Column(name = "sesion_activa")
    private Boolean sesionActiva = false;

    @Column(name = "dispositivo_activo")
    private String dispositivoActivo;

    @Column(name = "session_id_activo")
    private String sessionIdActivo;

    @Column(name = "two_factor_codigo")
    private String twoFactorCodigo;

    @Column(name = "two_factor_expiracion")
    private LocalDateTime twoFactorExpiracion;

    @Column(name = "two_factor_temp_token")
    private String twoFactorTempToken;

    @Column(name = "two_factor_dispositivo")
    private String twoFactorDispositivo;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;
}
