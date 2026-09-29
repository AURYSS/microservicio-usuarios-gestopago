package com.gestopago.microusuarios.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "domicilios")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Domicilio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String calle;
    
    @NotBlank
    private String numeroExterior;
    
    private String numeroInterior;
    
    @NotBlank
    private String colonia;
    
    @NotBlank
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;
    
    @NotBlank
    private String municipio;
    
    @NotBlank
    private String estado;
    
    @NotBlank
    private String pais;
}
