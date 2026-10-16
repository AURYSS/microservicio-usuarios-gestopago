package com.gestopago.microusuarios.dto;

import com.gestopago.microusuarios.entity.Domicilio;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponse {
    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private Double ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private Domicilio domicilio;
    private List<CuentaResponse> cuentas;
}
