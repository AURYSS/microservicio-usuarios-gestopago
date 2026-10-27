package com.gestopago.microusuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@RestController
public class MicroUsuariosApplication {
	public static void main(String[] args) {
		SpringApplication.run(MicroUsuariosApplication.class, args);
	}

	@RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.HEAD})
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of("status", "UP", "service", "micro-usuarios"));
	}
}
