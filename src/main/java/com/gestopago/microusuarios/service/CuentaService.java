package com.gestopago.microusuarios.service;

import com.gestopago.microusuarios.client.ApiGestoPagoClient;
import com.gestopago.microusuarios.dto.PagarServicioGestoPagoRequest;
import com.gestopago.microusuarios.dto.PagarServicioGestoPagoResponse;
import com.gestopago.microusuarios.dto.PagoServicioCuentaResponse;
import com.gestopago.microusuarios.dto.ValidarPagoCuentaRequest;
import com.gestopago.microusuarios.entity.Cliente;
import com.gestopago.microusuarios.entity.Cuenta;
import com.gestopago.microusuarios.entity.Usuario;
import com.gestopago.microusuarios.exception.BadRequestException;
import com.gestopago.microusuarios.exception.CredencialesInvalidasException;
import com.gestopago.microusuarios.exception.CuentaInactivaException;
import com.gestopago.microusuarios.exception.CuentaNoEncontradaException;
import com.gestopago.microusuarios.repository.CuentaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
public class CuentaService {

    private static final DateTimeFormatter HORA_LOCAL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault());

    private final CuentaRepository cuentaRepository;
    private final ApiGestoPagoClient apiGestoPagoClient;
    private final PasswordEncoder passwordEncoder;

    private final ConcurrentHashMap<UUID, ReentrantLock> locksPorCuenta = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PagoServicioCuentaResponse> pagosPorIdempotencia = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<String>> historialMovimientos = new ConcurrentHashMap<>();

    public CuentaService(CuentaRepository cuentaRepository) {
        this(cuentaRepository, null, null);
    }

    @Autowired
    public CuentaService(CuentaRepository cuentaRepository,
                         ApiGestoPagoClient apiGestoPagoClient,
                         PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.apiGestoPagoClient = apiGestoPagoClient;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuenta(UUID numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con el numero: " + numeroCuenta));
    }

    private Cuenta obtenerCuentaParaActualizacion(UUID numeroCuenta) {
        return cuentaRepository.findByNumeroCuentaForUpdate(numeroCuenta)
                .or(() -> cuentaRepository.findByNumeroCuenta(numeroCuenta))
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta no encontrada con el numero: " + numeroCuenta));
    }

    @Transactional(readOnly = true)
    public List<Cuenta> obtenerCuentasActivas() {
        return cuentaRepository.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public Double obtenerSaldo(UUID numeroCuenta) {
        Cuenta cuenta = obtenerCuenta(numeroCuenta);
        return cuenta.getSaldo();
    }

    /**
     * Implementa toda la parte izquierda del diagrama:
     * 1. Recibe NumCuenta, Monto, Telefono/Ref, ID Servicio, ID Producto, Tipo Front y Pin Autorizacion.
     * 2. Valida:
     *    - Cuenta Activa
     *    - Saldo Cuenta (saldo >= monto)
     *    - Pin Autorización (contra el pin de la cuenta o password del usuario asociado)
     *    - Validar Telefono/Ref (no vacío y longitud válida)
     * 3. Construye el objeto del cuadro central:
     *    - IdServicio, IdProducto, Telefono/Referencia, horaLocal, MontoPago, UPC.
     */
    @Transactional(readOnly = true)
    public PagoServicioCuentaResponse validarPagoServicio(ValidarPagoCuentaRequest request) {
        if (request.getMonto() == null || request.getMonto() <= 0) {
            throw new BadRequestException("El monto a pagar debe ser mayor a 0.");
        }
        validarEscalaMonetaria(request.getMonto());
        if (request.getIdServicio() == null || request.getIdServicio() <= 0) {
            throw new BadRequestException("El idServicio es obligatorio y debe ser válido.");
        }
        if (request.getIdProducto() == null || request.getIdProducto() <= 0) {
            throw new BadRequestException("El idProducto es obligatorio y debe ser válido.");
        }

        // Validación 1 (arriba en el diagrama): Validar Telefono/Ref
        String telefonoORef = validarTelefonoOReferencia(request);

        Cuenta cuenta = obtenerCuenta(request.getNumeroCuenta());

        // Validación 2 (abajo centro en el diagrama): Cuenta Activa
        validarCuentaActiva(cuenta);

        // Validación 3 (abajo izquierda en el diagrama): Saldo Cuenta
        validarSaldoCuenta(cuenta, request.getMonto());

        // Validación 4 (abajo derecha en el diagrama): Pin Autorización
        validarPinAutorizacion(cuenta, request.getPinAutorizacion());

        // Construir datos para el cuadro central del diagrama
        PagarServicioGestoPagoRequest datosCentral = construirDatosCuadroCentral(request, telefonoORef);

        return PagoServicioCuentaResponse.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .cuentaActiva(true)
                .saldoSuficiente(true)
                .pinAutorizado(true)
                .referenciaValida(true)
                .saldoAnterior(cuenta.getSaldo())
                .monto(request.getMonto())
                .nuevoSaldo(cuenta.getSaldo() - request.getMonto())
                .tipoFront(datosCentral.getTipoFront())
                .datosPagoCentral(datosCentral)
                .build();
    }

    /**
     * Ejecuta el flujo completo de izquierda a derecha del diagrama:
     * 1. Controla concurrencia por cuenta y previene cargos duplicados por idempotencia (numeroCuenta + upc).
     * 2. Valida Cuenta Activa, Saldo Cuenta, Pin Autorización y Telefono/Ref.
     * 3. Retiene/descuenta preventivamente el saldo y llama a api-gesto-pago (Firmar -> Token -> sendTx).
     * 4. Si es APROBADA confirma el descuento; si es FALLIDA/RECHAZADA revierte el saldo;
     *    si es EN_PROCESO (timeout) mantiene el saldo retenido hasta conciliar con confirmTx.
     */
    @Transactional
    public PagoServicioCuentaResponse pagarServicioConCuenta(ValidarPagoCuentaRequest request) {
        if (request.getNumeroCuenta() == null) {
            throw new BadRequestException("El número de cuenta es obligatorio.");
        }
        ReentrantLock lock = locksPorCuenta.computeIfAbsent(request.getNumeroCuenta(), k -> new ReentrantLock());
        lock.lock();
        try {
            boolean tieneUpcExplicito = request.getUpc() != null && !request.getUpc().isBlank();
            String claveIdempotencia = tieneUpcExplicito
                    ? claveIdempotencia(request.getNumeroCuenta(), derivarUpc(request.getUpc().trim()))
                    : null;

            if (claveIdempotencia != null) {
                PagoServicioCuentaResponse previo = pagosPorIdempotencia.get(claveIdempotencia);
                if (previo != null && previo.getResultadoPagoGestoPago() != null) {
                    String estadoPrevio = previo.getResultadoPagoGestoPago().getEstado();
                    if (!"FALLIDA".equalsIgnoreCase(estadoPrevio) && !"RECHAZADA".equalsIgnoreCase(estadoPrevio)) {
                        log.info("Solicitud duplicada detectada para clave={}, devolviendo resultado previo ({})",
                                claveIdempotencia, estadoPrevio);
                        return previo;
                    }
                }
            }

            PagoServicioCuentaResponse validacion = validarPagoServicio(request);
            Cuenta cuenta = obtenerCuentaParaActualizacion(request.getNumeroCuenta());
            validarCuentaActiva(cuenta);
            validarSaldoCuenta(cuenta, request.getMonto());
            validarPinAutorizacion(cuenta, request.getPinAutorizacion());

            Double saldoAnterior = cuenta.getSaldo();
            Double nuevoSaldo = redondearMoneda(saldoAnterior - request.getMonto());
            String upcOperacion = validacion.getDatosPagoCentral().getUpc();
            String claveEfectiva = claveIdempotencia(cuenta.getNumeroCuenta(), upcOperacion);

            // Estrategia: retener/descontar saldo antes de enviar al proveedor externo
            cuenta.setSaldo(nuevoSaldo);

            PagarServicioGestoPagoResponse respuestaGestoPago = null;
            if (apiGestoPagoClient != null) {
                try {
                    respuestaGestoPago = apiGestoPagoClient.pagarServicio(validacion.getDatosPagoCentral());
                    if (respuestaGestoPago != null
                            && ("FALLIDA".equalsIgnoreCase(respuestaGestoPago.getEstado())
                            || "RECHAZADA".equalsIgnoreCase(respuestaGestoPago.getEstado()))) {
                        // Revertir saldo porque el proveedor rechazó definitivamente la operación
                        cuenta.setSaldo(saldoAnterior);
                        String msg = respuestaGestoPago.getMensaje() != null
                                ? respuestaGestoPago.getMensaje()
                                : "El proveedor rechazó el pago del servicio.";
                        throw new BadRequestException("Pago rechazado por GestoPago: " + msg);
                    }
                } catch (BadRequestException e) {
                    cuenta.setSaldo(saldoAnterior);
                    throw e;
                } catch (RestClientResponseException e) {
                    // Error 4xx de validación (ej. producto no existe en catálogo o monto manipulado): revertir saldo
                    cuenta.setSaldo(saldoAnterior);
                    throw new BadRequestException("Operación rechazada en validación de catálogo o parámetros del proveedor.");
                } catch (Exception e) {
                    // Timeout o fallo de comunicación incierto: NO asumir que la transacción no se procesó.
                    // Se mantiene el saldo retenido y la operación queda EN_PROCESO para conciliación posterior.
                    log.warn("Resultado incierto al comunicar con api-gesto-pago para upc={} ({}). Se mantiene EN_PROCESO para conciliación.",
                            upcOperacion, e.getClass().getSimpleName());
                    respuestaGestoPago = PagarServicioGestoPagoResponse.builder()
                            .idServicio(validacion.getDatosPagoCentral().getIdServicio())
                            .idProducto(validacion.getDatosPagoCentral().getIdProducto())
                            .telefono(validacion.getDatosPagoCentral().getTelefono())
                            .referencia(validacion.getDatosPagoCentral().getReferencia())
                            .horaLocal(validacion.getDatosPagoCentral().getHoraLocal())
                            .montoPago(validacion.getDatosPagoCentral().getMontoPago().toPlainString())
                            .upc(upcOperacion)
                            .estado("EN_PROCESO")
                            .codigoMensaje("82")
                            .mensaje("Tiempo de espera agotado; pendiente de conciliación con el proveedor")
                            .build();
                }
            }

            cuentaRepository.save(cuenta);

            validacion.setSaldoAnterior(saldoAnterior);
            validacion.setNuevoSaldo(cuenta.getSaldo());
            validacion.setResultadoPagoGestoPago(respuestaGestoPago);

            pagosPorIdempotencia.put(claveEfectiva, validacion);
            String estadoRegistro = respuestaGestoPago != null && respuestaGestoPago.getEstado() != null
                    ? respuestaGestoPago.getEstado()
                    : "APROBADA";
            registrarMovimiento(cuenta.getNumeroCuenta(),
                    String.format("Pago servicio %d/%d por $%.2f [upc=%s, estado=%s]",
                            request.getIdServicio(), request.getIdProducto(), request.getMonto(),
                            upcOperacion, estadoRegistro));
            return validacion;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Concilia una operación cuyo estado quedó incierto (EN_PROCESO) por timeout.
     * Consulta confirmTx a través de api-gesto-pago:
     * - Si el proveedor confirma APROBADA (01/06), el descuento retenido queda definitivo.
     * - Si el proveedor confirma FALLIDA (70 - no aplicada), se reintegra el saldo a la cuenta del cliente.
     */
    @Transactional
    public PagoServicioCuentaResponse conciliarPagoServicio(ValidarPagoCuentaRequest request) {
        if (request.getNumeroCuenta() == null) {
            throw new BadRequestException("El número de cuenta es obligatorio para conciliar.");
        }
        if (request.getUpc() == null || request.getUpc().isBlank()) {
            throw new BadRequestException("El upc de la operación es obligatorio para conciliar.");
        }
        ReentrantLock lock = locksPorCuenta.computeIfAbsent(request.getNumeroCuenta(), k -> new ReentrantLock());
        lock.lock();
        try {
            String upcLimpio = derivarUpc(request.getUpc().trim());
            String clave = claveIdempotencia(request.getNumeroCuenta(), upcLimpio);
            PagoServicioCuentaResponse previo = pagosPorIdempotencia.get(clave);

            Cuenta cuenta = obtenerCuentaParaActualizacion(request.getNumeroCuenta());
            validarCuentaActiva(cuenta);

            String telefonoORef = validarTelefonoOReferencia(request);
            request.setUpc(upcLimpio);
            PagarServicioGestoPagoRequest datosCentral = previo != null && previo.getDatosPagoCentral() != null
                    ? previo.getDatosPagoCentral()
                    : construirDatosCuadroCentral(request, telefonoORef);

            if (apiGestoPagoClient == null) {
                throw new BadRequestException("Cliente de conciliación GestoPago no disponible.");
            }

            PagarServicioGestoPagoResponse confirmacion = apiGestoPagoClient.confirmarServicio(datosCentral);
            String nuevoEstado = confirmacion != null && confirmacion.getEstado() != null
                    ? confirmacion.getEstado()
                    : "EN_PROCESO";

            Double saldoAntesConciliacion = cuenta.getSaldo();
            boolean estabaEnProceso = previo != null
                    && previo.getResultadoPagoGestoPago() != null
                    && "EN_PROCESO".equalsIgnoreCase(previo.getResultadoPagoGestoPago().getEstado());

            if (("FALLIDA".equalsIgnoreCase(nuevoEstado) || "RECHAZADA".equalsIgnoreCase(nuevoEstado))
                    && estabaEnProceso) {
                // El proveedor confirmó que NO se aplicó (código 70): reversar/reembolsar el saldo retenido
                Double montoReembolso = previo.getMonto() != null ? previo.getMonto() : request.getMonto();
                cuenta.setSaldo(redondearMoneda(cuenta.getSaldo() + montoReembolso));
                cuentaRepository.save(cuenta);
                registrarMovimiento(cuenta.getNumeroCuenta(),
                        String.format("Reverso por conciliación fallida $%.2f [upc=%s]", montoReembolso, upcLimpio));
            } else if ("APROBADA".equalsIgnoreCase(nuevoEstado)) {
                registrarMovimiento(cuenta.getNumeroCuenta(),
                        String.format("Conciliación exitosa confirmada [upc=%s, aut=%s]",
                                upcLimpio, confirmacion != null ? confirmacion.getNumAutorizacion() : ""));
            }

            PagoServicioCuentaResponse respuesta = PagoServicioCuentaResponse.builder()
                    .numeroCuenta(cuenta.getNumeroCuenta())
                    .cuentaActiva(true)
                    .saldoSuficiente(true)
                    .pinAutorizado(true)
                    .referenciaValida(true)
                    .saldoAnterior(previo != null ? previo.getSaldoAnterior() : saldoAntesConciliacion)
                    .monto(previo != null ? previo.getMonto() : request.getMonto())
                    .nuevoSaldo(cuenta.getSaldo())
                    .tipoFront(datosCentral.getTipoFront())
                    .datosPagoCentral(datosCentral)
                    .resultadoPagoGestoPago(confirmacion)
                    .build();

            pagosPorIdempotencia.put(clave, respuesta);
            return respuesta;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Map<String, Object> pagar(UUID numeroCuenta, Double monto) {
        if (monto == null || monto <= 0) {
            throw new BadRequestException("El monto a pagar debe ser mayor a 0.");
        }
        validarEscalaMonetaria(monto);

        ReentrantLock lock = locksPorCuenta.computeIfAbsent(numeroCuenta, k -> new ReentrantLock());
        lock.lock();
        try {
            Cuenta cuenta = obtenerCuentaParaActualizacion(numeroCuenta);

            validarCuentaActiva(cuenta);
            validarSaldoCuenta(cuenta, monto);

            cuenta.setSaldo(redondearMoneda(cuenta.getSaldo() - monto));
            cuentaRepository.save(cuenta);
            registrarMovimiento(numeroCuenta, String.format("Cargo directo por $%.2f", monto));

            return Map.of(
                "mensaje", "Pago realizado exitosamente",
                "nuevoSaldo", cuenta.getSaldo()
            );
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Map<String, Object> pagar(ValidarPagoCuentaRequest request) {
        PagoServicioCuentaResponse resultado = pagarServicioConCuenta(request);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", "Pago realizado exitosamente");
        respuesta.put("numeroCuenta", resultado.getNumeroCuenta());
        respuesta.put("cuentaActiva", resultado.getCuentaActiva());
        respuesta.put("saldoSuficiente", resultado.getSaldoSuficiente());
        respuesta.put("pinAutorizado", resultado.getPinAutorizado());
        respuesta.put("referenciaValida", resultado.getReferenciaValida());
        respuesta.put("saldoAnterior", resultado.getSaldoAnterior());
        respuesta.put("monto", resultado.getMonto());
        respuesta.put("nuevoSaldo", resultado.getNuevoSaldo());
        respuesta.put("datosPagoCentral", resultado.getDatosPagoCentral());
        if (resultado.getResultadoPagoGestoPago() != null) {
            respuesta.put("resultadoPagoGestoPago", resultado.getResultadoPagoGestoPago());
        }
        return respuesta;
    }

    @Transactional
    public Map<String, Object> actualizarPin(UUID numeroCuenta, String nuevoPin) {
        if (nuevoPin == null || !nuevoPin.trim().matches("^\\d{4,6}$")) {
            throw new BadRequestException("El PIN de autorización debe contener entre 4 y 6 dígitos numéricos.");
        }
        Cuenta cuenta = obtenerCuenta(numeroCuenta);
        validarCuentaActiva(cuenta);
        cuenta.setPinAutorizacion(nuevoPin.trim());
        cuentaRepository.save(cuenta);
        return Map.of(
                "numeroCuenta", cuenta.getNumeroCuenta(),
                "mensaje", "PIN de autorización actualizado exitosamente"
        );
    }

    private String validarTelefonoOReferencia(ValidarPagoCuentaRequest request) {
        String valor = request.obtenerTelefonoOReferencia();
        if (valor == null || valor.isBlank()) {
            throw new BadRequestException("Validación fallida: El teléfono o referencia es obligatorio.");
        }
        if (valor.length() < 4 || valor.length() > 100) {
            throw new BadRequestException("Validación fallida: El teléfono o referencia debe tener entre 4 y 100 caracteres.");
        }
        Short tipoFront = request.getTipoFront() != null ? request.getTipoFront() : (short) 2;
        if (tipoFront == 1 && !valor.matches("^#?\\d{10}$")) {
            throw new BadRequestException("Validación fallida: Para recargas telefónicas (tipoFront=1), el teléfono debe tener 10 dígitos.");
        }
        return valor;
    }

    private void validarCuentaActiva(Cuenta cuenta) {
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new CuentaInactivaException("Operacion denegada: La cuenta esta inactiva y no puede realizar pagos.");
        }
        if (cuenta.getCliente() != null && Boolean.FALSE.equals(cuenta.getCliente().getActivo())) {
            throw new CuentaInactivaException("Operacion denegada: El titular de la cuenta se encuentra inactivo.");
        }
    }

    private void validarSaldoCuenta(Cuenta cuenta, Double monto) {
        if (cuenta.getSaldo() == null || cuenta.getSaldo() < monto) {
            throw new BadRequestException("Saldo insuficiente para realizar el pago.");
        }
    }

    private void validarPinAutorizacion(Cuenta cuenta, String pinEnviado) {
        if (pinEnviado == null || pinEnviado.isBlank()) {
            throw new CredencialesInvalidasException("El PIN de autorización es obligatorio para autorizar el pago.");
        }
        String pinLimpio = pinEnviado.trim();
        String pinCuenta = (cuenta.getPinAutorizacion() != null && !cuenta.getPinAutorizacion().isBlank())
                ? cuenta.getPinAutorizacion().trim()
                : "1234";

        if (pinCuenta.equals(pinLimpio)) {
            return;
        }

        if (passwordEncoder != null && passwordEncoder.matches(pinLimpio, pinCuenta)) {
            return;
        }

        // También permite validar contra la contraseña del usuario dueño de la cuenta si se envía como autorización
        Cliente cliente = cuenta.getCliente();
        if (cliente != null && cliente.getUsuario() != null) {
            Usuario usuario = cliente.getUsuario();
            if (passwordEncoder != null && usuario.getPassword() != null
                    && passwordEncoder.matches(pinLimpio, usuario.getPassword())) {
                return;
            }
        }

        throw new CredencialesInvalidasException("PIN de autorización inválido para la cuenta especificada.");
    }

    private PagarServicioGestoPagoRequest construirDatosCuadroCentral(ValidarPagoCuentaRequest request, String telefonoORef) {
        Short tipoFront = request.getTipoFront() != null ? request.getTipoFront() : (short) 2;
        String tel = (request.getTelefono() != null && !request.getTelefono().isBlank())
                ? request.getTelefono().trim()
                : (tipoFront == 1 ? telefonoORef : "1111111111");
        String ref = (request.getReferencia() != null && !request.getReferencia().isBlank())
                ? request.getReferencia().trim()
                : telefonoORef;

        String horaLocal = (request.getHoraLocal() != null && !request.getHoraLocal().isBlank())
                ? request.getHoraLocal().trim()
                : HORA_LOCAL_FORMATTER.format(Instant.now());

        String upc = (request.getUpc() != null && !request.getUpc().isBlank())
                ? derivarUpc(request.getUpc().trim())
                : derivarUpc(UUID.randomUUID().toString());

        BigDecimal montoPago = BigDecimal.valueOf(request.getMonto()).setScale(2, RoundingMode.HALF_UP);

        return PagarServicioGestoPagoRequest.builder()
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .telefono(tel)
                .referencia(ref)
                .horaLocal(horaLocal)
                .montoPago(montoPago)
                .upc(upc)
                .tipoFront(tipoFront)
                .build();
    }

    private String derivarUpc(String base) {
        if (base.length() >= 3 && base.length() <= 15) {
            return base;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 15);
        } catch (Exception e) {
            return base.substring(0, Math.min(base.length(), 15));
        }
    }

    @Transactional
    public Map<String, Object> recargar(UUID numeroCuenta, Double monto) {
        if (monto == null || monto <= 0) {
            throw new BadRequestException("El monto a recargar debe ser mayor a 0.");
        }
        validarEscalaMonetaria(monto);

        ReentrantLock lock = locksPorCuenta.computeIfAbsent(numeroCuenta, k -> new ReentrantLock());
        lock.lock();
        try {
            Cuenta cuenta = obtenerCuentaParaActualizacion(numeroCuenta);

            if (!Boolean.TRUE.equals(cuenta.getActiva())) {
                throw new CuentaInactivaException("Operacion denegada: La cuenta esta inactiva y no puede recibir recargas.");
            }

            cuenta.setSaldo(redondearMoneda(cuenta.getSaldo() + monto));
            cuentaRepository.save(cuenta);
            registrarMovimiento(numeroCuenta, String.format("Recarga de saldo por $%.2f", monto));

            return Map.of(
                "mensaje", "Recarga exitosa",
                "nuevoSaldo", cuenta.getSaldo()
            );
        } finally {
            lock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerHistorial(UUID numeroCuenta) {
        Cuenta cuenta = obtenerCuenta(numeroCuenta);
        List<String> movimientos = historialMovimientos.getOrDefault(numeroCuenta, List.of(
                "Transaccion 1 - Abono de apertura",
                "Transaccion 2 - Consulta de saldo"
        ));

        return Map.of(
            "numeroCuenta", numeroCuenta,
            "estatus", Boolean.TRUE.equals(cuenta.getActiva()) ? "ACTIVA" : "INACTIVA",
            "saldo", cuenta.getSaldo(),
            "historial", movimientos
        );
    }

    private String claveIdempotencia(UUID numeroCuenta, String upc) {
        return numeroCuenta.toString() + ":" + upc;
    }

    private Double redondearMoneda(Double valor) {
        if (valor == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void validarEscalaMonetaria(Double monto) {
        BigDecimal bd = BigDecimal.valueOf(monto);
        if (bd.scale() > 2) {
            throw new BadRequestException("El monto no puede tener más de 2 decimales.");
        }
    }

    private void registrarMovimiento(UUID numeroCuenta, String detalle) {
        historialMovimientos.compute(numeroCuenta, (k, lista) -> {
            List<String> copia = lista == null ? new ArrayList<>() : new ArrayList<>(lista);
            copia.add(detalle);
            return copia;
        });
    }
}
