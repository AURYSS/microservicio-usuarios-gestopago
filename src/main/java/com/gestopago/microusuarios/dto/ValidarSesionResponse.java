package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidarSesionResponse {

    private boolean valido;
    private String mensaje;
    private String correo;
    private Long usuarioId;
    private Long clienteId;
    private String rol;
    private String dispositivo;
    private String sessionId;
}
