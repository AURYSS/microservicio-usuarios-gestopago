package com.gestopago.microusuarios.exception;

import com.gestopago.microusuarios.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errores.put(fieldName, errorMessage);
        });

        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Falla en la validación de los datos de entrada.")
                .codigoEstado(HttpStatus.BAD_REQUEST.value())
                .errores(errores)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
        UsuarioNoEncontradoException.class, 
        ClienteNoEncontradoException.class, 
        CuentaNoEncontradaException.class, 
        ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje(ex.getMessage())
                .codigoEstado(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
        CorreoDuplicadoException.class, 
        CurpDuplicadaException.class, 
        RfcDuplicadoException.class, 
        ClienteYaRegistradoException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje(ex.getMessage())
                .codigoEstado(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({
        BadRequestException.class, 
        ContrasenaInvalidaException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje(ex.getMessage())
                .codigoEstado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
        UsuarioInactivoException.class, 
        CuentaInactivaException.class
    })
    public ResponseEntity<ErrorResponse> handleInactivo(RuntimeException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje(ex.getMessage())
                .codigoEstado(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
        BadCredentialsException.class,
        CredencialesInvalidasException.class
    })
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
        String msg = ex instanceof CredencialesInvalidasException
                ? ex.getMessage()
                : "Credenciales de autenticación inválidas.";
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje(msg)
                .codigoEstado(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledUser(DisabledException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("El usuario se encuentra inactivo en el sistema.")
                .codigoEstado(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Acceso denegado: No posee los permisos requeridos para ejecutar esta acción.")
                .codigoEstado(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errores.put(v.getPropertyPath().toString(), v.getMessage()));
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Restricción de validación incumplida.")
                .codigoEstado(HttpStatus.BAD_REQUEST.value())
                .errores(errores)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex) {
        log.warn("Solicitud con formato o parámetros inválidos: {}", ex.getClass().getSimpleName());
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Formato de solicitud o parámetros inválidos.")
                .codigoEstado(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Recurso no encontrado: " + ex.getResourcePath())
                .codigoEstado(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Método HTTP no permitido para este endpoint.")
                .codigoEstado(HttpStatus.METHOD_NOT_ALLOWED.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Error interno no controlado ({})", ex.getClass().getSimpleName());
        ErrorResponse response = ErrorResponse.builder()
                .exito(false)
                .mensaje("Ocurrió un error interno al procesar la solicitud. Intente nuevamente más tarde.")
                .codigoEstado(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
