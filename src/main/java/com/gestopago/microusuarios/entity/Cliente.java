package com.gestopago.microusuarios.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    @Size(min = 2, max = 50)
    private String nombre;
    
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "Solo letras y espacios")
    private String segundoNombre;
    
    @NotBlank
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    @Size(min = 2, max = 50)
    private String apellidoPaterno;
    
    @NotBlank
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Solo letras y espacios")
    @Size(min = 2, max = 50)
    private String apellidoMaterno;
    
    @NotNull @Past
    private LocalDate fechaNacimiento;
    
    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z\\d]\\d$")
    @Size(min = 18, max = 18)
    @Column(unique = true, updatable = false)
    private String curp;
    
    @NotBlank
    @Pattern(regexp = "^[A-Z&Ñ]{3,4}\\d{6}[A-V1-9][A-Z1-9][0-9A]$")
    @Size(min = 12, max = 13)
    @Column(unique = true, updatable = false)
    private String rfc;
    
    @NotBlank
    private String sexo;
    
    @NotBlank
    private String nacionalidad;
    
    @NotBlank
    private String estadoCivil;
    
    @NotBlank
    @Email
    @Size(max = 100)
    @Column(unique = true)
    private String correo;
    
    @NotBlank
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefonoMovil;
    
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefonoAlternativo;
    
    @NotBlank
    private String ocupacion;
    
    @NotBlank
    private String empresa;
    
    @NotNull
    @Positive
    private Double ingresoMensual;
    
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaRegistro;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "domicilio_id", referencedColumnName = "id")
    private Domicilio domicilio;
    
    @Builder.Default
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("cliente")
    private List<Cuenta> cuentas = new ArrayList<>();
    
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("cliente")
    private Usuario usuario;
}
