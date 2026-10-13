package com.gestopago.microusuarios;

import com.gestopago.microusuarios.dto.GenericResponse;
import com.gestopago.microusuarios.security.ValidadorCampos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorCamposTest {

    private ValidadorCampos validadorCampos;

    @BeforeEach
    void setUp() {
        validadorCampos = new ValidadorCampos();
    }

    @Test
    void testDtoValidationConErrores() {
        Object target = new Object();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, "clienteDTO");
        errors.addError(new FieldError("clienteDTO", "correo", "El correo es obligatorio"));
        errors.addError(new FieldError("clienteDTO", "curp", "La CURP debe tener 18 caracteres"));

        ResponseEntity<GenericResponse> response = validadorCampos.dtoValidation(errors);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCodigo());
        assertTrue(response.getBody().getMensaje().contains("Datos no validos"));
        assertTrue(response.getBody().getMensaje().contains("correo"));
        assertTrue(response.getBody().getMensaje().contains("curp"));
    }
}
