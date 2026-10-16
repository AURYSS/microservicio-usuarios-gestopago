package com.gestopago.microusuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean exito;
    private String mensaje;
    private T datos;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> exito(String mensaje, T datos) {
        return ApiResponse.<T>builder()
                .exito(true)
                .mensaje(mensaje)
                .datos(datos)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> exito(String mensaje) {
        return exito(mensaje, null);
    }
}
