package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.ClienteRegistroRequest;
import com.gestopago.microusuarios.entity.Cliente;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PreAuthorize("permitAll()")
    @PostMapping
    public ResponseEntity<Cliente> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        Cliente nuevoCliente = clienteService.crearCliente(request.getCliente(), request.getPassword());
        return ResponseEntity.ok(nuevoCliente);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> obtenerClientes(@RequestParam(required = false) Optional<Long> id) {
        if (id != null && id.isPresent()) {
            return ResponseEntity.ok(clienteService.obtenerPorId(id.get()));
        }
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/id")
    public ResponseEntity<Cliente> obtenerClientePorId(@RequestParam(required = false) Optional<Long> id) {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("El parámetro 'id' es requerido en la petición.");
        }
        return ResponseEntity.ok(clienteService.obtenerPorId(id.get()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<Cliente> actualizarCliente(
            @RequestParam(required = false) Optional<Long> id, 
            @Valid @RequestBody Cliente clienteActualizado) {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("El parámetro 'id' es requerido para actualizar el cliente.");
        }
        return ResponseEntity.ok(clienteService.actualizarCliente(id.get(), clienteActualizado));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> bajaLogicaCliente(@RequestParam(required = false) Optional<Long> id) {
        if (id == null || id.isEmpty()) {
            throw new BadRequestException("El parámetro 'id' es requerido para la baja lógica del cliente.");
        }
        clienteService.bajaLogica(id.get());
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/curp")
    public ResponseEntity<Cliente> buscarPorCurp(@RequestParam(required = false) Optional<String> curp) {
        if (curp == null || curp.isEmpty() || curp.get().isBlank()) {
            throw new BadRequestException("El parámetro 'curp' es requerido en la petición.");
        }
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp.get()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rfc")
    public ResponseEntity<Cliente> buscarPorRfc(@RequestParam(required = false) Optional<String> rfc) {
        if (rfc == null || rfc.isEmpty() || rfc.get().isBlank()) {
            throw new BadRequestException("El parámetro 'rfc' es requerido en la petición.");
        }
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc.get()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/correo")
    public ResponseEntity<Cliente> buscarPorCorreo(@RequestParam(required = false) Optional<String> correo) {
        if (correo == null || correo.isEmpty() || correo.get().isBlank()) {
            throw new BadRequestException("El parámetro 'correo' es requerido en la petición.");
        }
        return ResponseEntity.ok(clienteService.obtenerPorCorreo(correo.get()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cuenta")
    public ResponseEntity<Cliente> buscarPorNumeroCuenta(@RequestParam(required = false) Optional<UUID> numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            throw new BadRequestException("El parámetro 'numeroCuenta' es requerido en la petición.");
        }
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta.get()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/activos")
    public ResponseEntity<List<Cliente>> obtenerClientesActivos() {
        return ResponseEntity.ok(clienteService.obtenerActivos());
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/fechas")
    public ResponseEntity<List<Cliente>> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(clienteService.obtenerPorRangoFechas(start, end));
    }
}
