package com.gestopago.microusuarios.service;

import com.gestopago.microusuarios.entity.*;
import com.gestopago.microusuarios.repository.*;
import com.gestopago.microusuarios.exception.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, CuentaRepository cuentaRepository, 
                          UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Cliente crearCliente(Cliente cliente, String plainPassword) {
        if (Period.between(cliente.getFechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw new BadRequestException("El cliente debe ser mayor de 18 años.");
        }
        
        if (clienteRepository.findByCurp(cliente.getCurp()).isPresent()) {
            throw new CurpDuplicadaException("CURP ya registrada.");
        }
        if (clienteRepository.findByRfc(cliente.getRfc()).isPresent()) {
            throw new RfcDuplicadoException("RFC ya registrado.");
        }
        if (clienteRepository.findByCorreo(cliente.getCorreo()).isPresent()) {
            throw new CorreoDuplicadoException("Correo electrónico ya registrado.");
        }
        
        // Password validation
        if (plainPassword == null || plainPassword.length() < 8 || 
            !plainPassword.matches(".*[A-Z].*") || 
            !plainPassword.matches(".*[a-z].*") || 
            !plainPassword.matches(".*\\d.*") || 
            !plainPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new ContrasenaInvalidaException("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
        }
        
        // Ensure relations are set
        if(cliente.getDomicilio() != null) {
            // Unidirectional Cascade is set
        }

        Cliente savedCliente = clienteRepository.save(cliente);
        
        Cuenta cuenta = Cuenta.builder().cliente(savedCliente).saldo(0.0).activa(true).build();
        cuenta = cuentaRepository.save(cuenta);
        savedCliente.getCuentas().add(cuenta);
        
        Usuario usuario = Usuario.builder()
            .correo(savedCliente.getCorreo())
            .password(passwordEncoder.encode(plainPassword))
            .role("CLIENTE")
            .activo(true)
            .cliente(savedCliente)
            .build();
        usuarioRepository.save(usuario);
        
        return savedCliente;
    }
    
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }
    
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con id: " + id));
    }
    
    public Cliente obtenerPorCurp(String curp) {
        return clienteRepository.findByCurp(curp)
            .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con curp: " + curp));
    }
    
    public Cliente obtenerPorRfc(String rfc) {
        return clienteRepository.findByRfc(rfc)
            .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con rfc: " + rfc));
    }
    
    public Cliente obtenerPorCorreo(String correo) {
        return clienteRepository.findByCorreo(correo)
            .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con correo: " + correo));
    }
    
    public Cliente obtenerPorNumeroCuenta(java.util.UUID numeroCuenta) {
        return clienteRepository.findByCuentasNumeroCuenta(numeroCuenta)
            .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con número de cuenta: " + numeroCuenta));
    }
    
    public List<Cliente> obtenerActivos() {
        return clienteRepository.findByActivoTrue();
    }
    
    public List<Cliente> obtenerPorRangoFechas(LocalDateTime start, LocalDateTime end) {
        return clienteRepository.findByFechaRegistroBetween(start, end);
    }

    @Transactional
    public Cliente actualizarCliente(Long id, Cliente datosActualizados) {
        Cliente cliente = obtenerPorId(id);
        
        cliente.setNombre(datosActualizados.getNombre());
        cliente.setSegundoNombre(datosActualizados.getSegundoNombre());
        cliente.setApellidoPaterno(datosActualizados.getApellidoPaterno());
        cliente.setApellidoMaterno(datosActualizados.getApellidoMaterno());
        cliente.setSexo(datosActualizados.getSexo());
        cliente.setNacionalidad(datosActualizados.getNacionalidad());
        cliente.setEstadoCivil(datosActualizados.getEstadoCivil());
        cliente.setTelefonoMovil(datosActualizados.getTelefonoMovil());
        cliente.setTelefonoAlternativo(datosActualizados.getTelefonoAlternativo());
        cliente.setOcupacion(datosActualizados.getOcupacion());
        cliente.setEmpresa(datosActualizados.getEmpresa());
        cliente.setIngresoMensual(datosActualizados.getIngresoMensual());
        
        if (datosActualizados.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            if (dom == null) {
                dom = new Domicilio();
                cliente.setDomicilio(dom);
            }
            dom.setCalle(datosActualizados.getDomicilio().getCalle());
            dom.setNumeroExterior(datosActualizados.getDomicilio().getNumeroExterior());
            dom.setNumeroInterior(datosActualizados.getDomicilio().getNumeroInterior());
            dom.setColonia(datosActualizados.getDomicilio().getColonia());
            dom.setCodigoPostal(datosActualizados.getDomicilio().getCodigoPostal());
            dom.setMunicipio(datosActualizados.getDomicilio().getMunicipio());
            dom.setEstado(datosActualizados.getDomicilio().getEstado());
            dom.setPais(datosActualizados.getDomicilio().getPais());
        }
        
        // Note: CURP, RFC, Correo, Cuenta are not updated
        
        return clienteRepository.save(cliente);
    }
    
    @Transactional
    public void bajaLogica(Long id) {
        Cliente cliente = obtenerPorId(id);
        cliente.setActivo(false);
        
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
        }
        
        for (Cuenta cuenta : cliente.getCuentas()) {
            cuenta.setActiva(false);
        }
        
        clienteRepository.save(cliente);
    }
}
