package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean exito;
    private String mensaje;
    private int codigoEstado;
    private Map<String, String> errores;
    private LocalDateTime timestamp;
}
