package com.gestopago.microusuarios.client;

import com.gestopago.microusuarios.dto.PagarServicioGestoPagoRequest;
import com.gestopago.microusuarios.dto.PagarServicioGestoPagoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Cliente HTTP para comunicarse con api-gesto-pago (parte central y derecha del diagrama).
 */
@Component
@Slf4j
public class ApiGestoPagoClient {

    private final RestClient restClient;
    private final String baseUrl;

    public ApiGestoPagoClient(String baseUrl) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Autowired
    public ApiGestoPagoClient(
            @Value("${api.gestopago.url:http://localhost:8080}") String baseUrl,
            @Value("${api.gestopago.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${api.gestopago.read-timeout-ms:15000}") int readTimeoutMs,
            RestClient.Builder restClientBuilder) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Envía los datos del cuadro central (idServicio, idProducto, telefono/referencia, horaLocal, montoPago, upc)
     * al endpoint POST /pagos/pagar-servicio de api-gesto-pago.
     */
    public PagarServicioGestoPagoResponse pagarServicio(PagarServicioGestoPagoRequest request) {
        log.info("Enviando solicitud a api-gesto-pago [{}/pagos/pagar-servicio] para idServicio={}, idProducto={}, upc={}",
                baseUrl, request.getIdServicio(), request.getIdProducto(), request.getUpc());
        return restClient.post()
                .uri("/pagos/pagar-servicio")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PagarServicioGestoPagoResponse.class);
    }

    /**
     * Concilia una transacción en estado incierto (EN_PROCESO / timeout) contra POST /pagos/confirmar-servicio.
     */
    public PagarServicioGestoPagoResponse confirmarServicio(PagarServicioGestoPagoRequest request) {
        log.info("Conciliando transaccion en api-gesto-pago [{}/pagos/confirmar-servicio] para upc={}",
                baseUrl, request.getUpc());
        return restClient.post()
                .uri("/pagos/confirmar-servicio")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PagarServicioGestoPagoResponse.class);
    }

    /**
     * Verifica una referencia contra api-gesto-pago cuando el producto es tipoFront == 4.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verificarReferencia(Integer idServicio, Integer idProducto, String referencia) {
        log.info("Verificando referencia en api-gesto-pago [{}/pagos/verificar-referencia] para idServicio={}, idProducto={}",
                baseUrl, idServicio, idProducto);
        return restClient.post()
                .uri("/pagos/verificar-referencia")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "idServicio", idServicio,
                        "idProducto", idProducto,
                        "referencia", referencia
                ))
                .retrieve()
                .body(Map.class);
    }
}
