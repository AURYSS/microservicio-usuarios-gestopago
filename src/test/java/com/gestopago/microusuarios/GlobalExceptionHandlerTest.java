package com.gestopago.microusuarios;

import com.gestopago.microusuarios.dto.ErrorResponse;
import com.gestopago.microusuarios.exception.GlobalExceptionHandler;
import com.gestopago.microusuarios.exception.UsuarioNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleUsuarioNoEncontrado() {
        UsuarioNoEncontradoException ex = new UsuarioNoEncontradoException("Usuario no encontrado con ID: 99");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isExito());
        assertEquals("Usuario no encontrado con ID: 99", response.getBody().getMensaje());
        assertEquals(404, response.getBody().getCodigoEstado());
        assertNotNull(response.getBody().getTimestamp());
    }
}
