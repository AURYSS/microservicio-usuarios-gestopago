package com.gestopago.microusuarios.controller;

import com.gestopago.microusuarios.dto.ClienteRegistroRequest;
import com.gestopago.microusuarios.entity.Cliente;
import com.gestopago.microusuarios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public ResponseEntity<Cliente> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        Cliente nuevoCliente = clienteService.crearCliente(request.getCliente(), request.getPassword());
        return ResponseEntity.ok(nuevoCliente);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Cliente>> obtenerClientes() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, @Valid @RequestBody Cliente clienteActualizado) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, clienteActualizado));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogicaCliente(@PathVariable Long id) {
        clienteService.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/curp/{curp}")
    public ResponseEntity<Cliente> buscarPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.obtenerPorCurp(curp));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<Cliente> buscarPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.obtenerPorRfc(rfc));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/correo/{correo}")
    public ResponseEntity<Cliente> buscarPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(clienteService.obtenerPorCorreo(correo));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<Cliente> buscarPorNumeroCuenta(@PathVariable java.util.UUID numeroCuenta) {
        return ResponseEntity.ok(clienteService.obtenerPorNumeroCuenta(numeroCuenta));
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
