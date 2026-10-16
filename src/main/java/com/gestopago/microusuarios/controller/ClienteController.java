package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.*;
import com.gestopago.microusuarios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PreAuthorize("permitAll()")
    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        ClienteResponse response = clienteService.crearClienteDTO(request);
        return new ResponseEntity<>(ApiResponse.exito("Cliente registrado exitosamente", response), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/buscar")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> buscarClientes(@RequestBody(required = false) ClienteFiltroRequest filtro) {
        List<ClienteResponse> resultados = clienteService.buscarClientes(filtro);
        return ResponseEntity.ok(ApiResponse.exito("Consulta ejecutada exitosamente", resultados));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(@Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse response = clienteService.actualizarClienteDTO(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente actualizado exitosamente", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> bajaLogicaCliente(@Valid @RequestBody ClienteDeleteRequest request) {
        clienteService.bajaLogicaDTO(request);
        return ResponseEntity.ok(ApiResponse.exito("Cliente dado de baja exitosamente (baja lógica)"));
    }
}
