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
        return crearCliente(cliente, plainPassword, "1234");
    }

    @Transactional
    public Cliente crearCliente(Cliente cliente, String plainPassword, String pinAutorizacion) {
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
        
        if (cliente.getIngresoMensual() != null && cliente.getIngresoMensual() <= 0) {
            throw new BadRequestException("El ingreso mensual debe ser mayor a cero.");
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
        
        String pinFinal = (pinAutorizacion != null && !pinAutorizacion.isBlank()) ? pinAutorizacion.trim() : "1234";
        Cuenta cuenta = Cuenta.builder()
                .cliente(savedCliente)
                .saldo(0.0)
                .activa(true)
                .pinAutorizacion(pinFinal)
                .build();
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
        
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BadRequestException("Operación denegada: No se puede modificar la información de un cliente inactivo.");
        }

        // Regla de Negocio: No permitir modificar CURP ni RFC
        if (datosActualizados.getCurp() != null && !datosActualizados.getCurp().trim().equalsIgnoreCase(cliente.getCurp())) {
            throw new BadRequestException("Regla de negocio: No está permitido modificar la CURP.");
        }
        if (datosActualizados.getRfc() != null && !datosActualizados.getRfc().trim().equalsIgnoreCase(cliente.getRfc())) {
            throw new BadRequestException("Regla de negocio: No está permitido modificar el RFC.");
        }

        // Regla de Negocio: Ingreso mensual debe ser mayor a cero
        if (datosActualizados.getIngresoMensual() != null && datosActualizados.getIngresoMensual() <= 0) {
            throw new BadRequestException("El ingreso mensual debe ser mayor a cero.");
        }

        // Regla de Negocio: Teléfono debe contener exactamente 10 dígitos
        if (datosActualizados.getTelefonoMovil() != null && !datosActualizados.getTelefonoMovil().matches("^\\d{10}$")) {
            throw new BadRequestException("El teléfono móvil debe contener exactamente 10 dígitos.");
        }
        if (datosActualizados.getTelefonoAlternativo() != null && !datosActualizados.getTelefonoAlternativo().isBlank() 
            && !datosActualizados.getTelefonoAlternativo().matches("^\\d{10}$")) {
            throw new BadRequestException("El teléfono alternativo debe contener exactamente 10 dígitos.");
        }
        
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

    @Transactional
    public com.gestopago.microusuarios.dto.ClienteResponse crearClienteDTO(com.gestopago.microusuarios.dto.ClienteRegistroRequest request) {
        Cliente cliente = crearCliente(request.getCliente(), request.getPassword(), request.getPinAutorizacion());
        return mapToResponse(cliente);
    }

    @Transactional
    public com.gestopago.microusuarios.dto.ClienteResponse actualizarClienteDTO(com.gestopago.microusuarios.dto.ClienteUpdateRequest request) {
        Cliente cliente = obtenerPorId(request.getId());

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BadRequestException("Operación denegada: No se puede modificar la información de un cliente inactivo.");
        }

        if (request.getNombre() != null && !request.getNombre().isBlank()) cliente.setNombre(request.getNombre());
        if (request.getSegundoNombre() != null) cliente.setSegundoNombre(request.getSegundoNombre());
        if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().isBlank()) cliente.setApellidoPaterno(request.getApellidoPaterno());
        if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().isBlank()) cliente.setApellidoMaterno(request.getApellidoMaterno());
        if (request.getSexo() != null) cliente.setSexo(request.getSexo());
        if (request.getNacionalidad() != null) cliente.setNacionalidad(request.getNacionalidad());
        if (request.getEstadoCivil() != null) cliente.setEstadoCivil(request.getEstadoCivil());
        if (request.getTelefonoMovil() != null && !request.getTelefonoMovil().isBlank()) cliente.setTelefonoMovil(request.getTelefonoMovil());
        if (request.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        if (request.getOcupacion() != null) cliente.setOcupacion(request.getOcupacion());
        if (request.getEmpresa() != null) cliente.setEmpresa(request.getEmpresa());
        if (request.getIngresoMensual() != null) cliente.setIngresoMensual(request.getIngresoMensual());

        if (request.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            if (dom == null) {
                dom = new Domicilio();
                cliente.setDomicilio(dom);
            }
            if (request.getDomicilio().getCalle() != null) dom.setCalle(request.getDomicilio().getCalle());
            if (request.getDomicilio().getNumeroExterior() != null) dom.setNumeroExterior(request.getDomicilio().getNumeroExterior());
            if (request.getDomicilio().getNumeroInterior() != null) dom.setNumeroInterior(request.getDomicilio().getNumeroInterior());
            if (request.getDomicilio().getColonia() != null) dom.setColonia(request.getDomicilio().getColonia());
            if (request.getDomicilio().getCodigoPostal() != null) dom.setCodigoPostal(request.getDomicilio().getCodigoPostal());
            if (request.getDomicilio().getMunicipio() != null) dom.setMunicipio(request.getDomicilio().getMunicipio());
            if (request.getDomicilio().getEstado() != null) dom.setEstado(request.getDomicilio().getEstado());
            if (request.getDomicilio().getPais() != null) dom.setPais(request.getDomicilio().getPais());
        }

        Cliente guardado = clienteRepository.save(cliente);
        return mapToResponse(guardado);
    }

    @Transactional
    public void bajaLogicaDTO(com.gestopago.microusuarios.dto.ClienteDeleteRequest request) {
        bajaLogica(request.getId());
    }

    @Transactional(readOnly = true)
    public List<com.gestopago.microusuarios.dto.ClienteResponse> buscarClientes(com.gestopago.microusuarios.dto.ClienteFiltroRequest filtro) {
        org.springframework.data.jpa.domain.Specification<Cliente> spec = com.gestopago.microusuarios.repository.ClienteSpecifications.conFiltros(filtro);
        return clienteRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public com.gestopago.microusuarios.dto.ClienteResponse mapToResponse(Cliente cliente) {
        List<com.gestopago.microusuarios.dto.CuentaResponse> cuentasResponse = cliente.getCuentas().stream()
                .map(c -> com.gestopago.microusuarios.dto.CuentaResponse.builder()
                        .id(c.getId())
                        .numeroCuenta(c.getNumeroCuenta())
                        .saldo(c.getSaldo())
                        .activa(c.getActiva())
                        .pinAutorizacion(c.getPinAutorizacion())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return com.gestopago.microusuarios.dto.ClienteResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                .activo(cliente.getActivo())
                .fechaRegistro(cliente.getFechaRegistro())
                .domicilio(cliente.getDomicilio())
                .cuentas(cuentasResponse)
                .build();
    }
}
