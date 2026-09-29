package com.gestopago.microusuarios.dto;

import com.gestopago.microusuarios.entity.Cliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRegistroRequest {
    @Valid
    private Cliente cliente;
    
    @NotBlank
    private String password;
}
