package com.boticario.modules.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integracion end-to-end del POS contra PostgreSQL real: venta
 * con conversion de unidades y descuento de stock por FEFO repartido entre
 * dos lotes, calculo de vuelto con pago en efectivo, la regla de que un
 * CAJERO no puede aplicar descuento (caso confirmado en la Fase 1),
 * rechazo de stock insuficiente, anulacion de venta que repone exactamente
 * los lotes consumidos, y una prueba de concurrencia sobre el mismo
 * producto (varias ventas simultaneas nunca dejan el stock negativo ni
 * pierden actualizaciones).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SaleFlowIT {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("boticario_test")
            .withUsername("boticario")
            .withPassword("boticario");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = login("admin", "Admin123!");
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(username, password));
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private record ProductoCreado(long productId, long presentationId) {
    }

    private ProductoCreado crearProducto(String nombre, String barcode, boolean controlLote, String salePrice) throws Exception {
        String createBody = """
                {
                  "name": "%s",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true,
                  "controlsLot": %s, "controlsExpiration": %s,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": %s, "barcode": "%s" }
                }
                """.formatted(nombre, controlLote, controlLote, salePrice, barcode);

        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        long productId = created.get("id").asLong();
        long presentationId = created.get("presentations").get(0).get("id").asLong();
        return new ProductoCreado(productId, presentationId);
    }

    private void agregarStock(long productId, String lotCode, LocalDate expirationDate, String quantityBase) throws Exception {
        String body = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": %s,
                  "reason": "carga inicial de prueba", "lotCode": "%s", "expirationDate": "%s" }
                """.formatted(productId, quantityBase, lotCode, expirationDate);
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private void agregarStockSinLote(long productId, String quantityBase) throws Exception {
        String body = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": %s, "reason": "carga inicial de prueba" }
                """.formatted(productId, quantityBase);
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void ventaConDescuentoConsumeFefoEntreDosLotesYCalculaVuelto() throws Exception {
        ProductoCreado producto = crearProducto("Producto Venta FEFO", "999400000001", true, "1000");

        agregarStock(producto.productId(), "LOTE-CERCA", LocalDate.now().plusDays(10), "5");
        agregarStock(producto.productId(), "LOTE-LEJOS", LocalDate.now().plusMonths(6), "10");

        String saleBody = """
                { "customerName": "Cliente de prueba",
                  "items": [ { "presentationId": %d, "quantityPresentation": 8, "discountPercentage": 10 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 10000 } ] }
                """.formatted(producto.presentationId());

        String response = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(saleBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(8000))
                .andExpect(jsonPath("$.discountTotal").value(800))
                .andExpect(jsonPath("$.total").value(7200))
                .andExpect(jsonPath("$.changeAmount").value(2800))
                .andExpect(jsonPath("$.items[0].lots.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode sale = objectMapper.readTree(response);
        JsonNode lots = sale.get("items").get(0).get("lots");
        // FEFO: el lote que vence antes (LOTE-CERCA, 5 unidades) se consume primero y por completo.
        assertThat(lots.get(0).get("lotCode").asText()).isEqualTo("LOTE-CERCA");
        assertThat(lots.get(0).get("quantityBase").asDouble()).isEqualTo(5.0);
        assertThat(lots.get(1).get("lotCode").asText()).isEqualTo("LOTE-LEJOS");
        assertThat(lots.get(1).get("quantityBase").asDouble()).isEqualTo(3.0);

        mockMvc.perform(get("/api/inventory/products/" + producto.productId() + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(7)); // 15 - 8
    }

    @Test
    void unCajeroNoPuedeAplicarDescuentoPeroSiPuedeVenderSinEl() throws Exception {
        ProductoCreado producto = crearProducto("Producto Venta Cajero", "999400000002", false, "500");
        agregarStockSinLote(producto.productId(), "50");

        String createCajero = """
                { "username": "cajero.prueba", "email": "cajero.prueba@boticario.test", "password": "Cajero123!",
                  "fullName": "Cajero De Prueba", "roleCodes": ["CAJERO"] }
                """;
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createCajero))
                .andExpect(status().isCreated());

        String cajeroToken = login("cajero.prueba", "Cajero123!");

        String ventaConDescuento = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 2, "discountPercentage": 15 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 1000 } ] }
                """.formatted(producto.presentationId());

        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON).content(ventaConDescuento))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        String ventaSinDescuento = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 2 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 1000 } ] }
                """.formatted(producto.presentationId());

        String ventaResponse = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON).content(ventaSinDescuento))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1000))
                .andReturn().getResponse().getContentAsString();
        long ventaId = objectMapper.readTree(ventaResponse).get("id").asLong();

        // El mismo cajero no tiene permiso para anular ventas (le falta SALE_CANCEL).
        mockMvc.perform(post("/api/sales/" + ventaId + "/cancel")
                        .header("Authorization", "Bearer " + cajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"intento no autorizado\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void anularUnaVentaReponeExactamenteLosLotesConsumidos() throws Exception {
        ProductoCreado producto = crearProducto("Producto Venta Anulada", "999400000003", true, "2000");
        agregarStock(producto.productId(), "LOTE-UNICO", LocalDate.now().plusMonths(4), "20");

        String saleBody = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 6 } ],
                  "payments": [ { "paymentMethod": "TARJETA", "amount": 12000 } ] }
                """.formatted(producto.presentationId());

        String saleResponse = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(saleBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long saleId = objectMapper.readTree(saleResponse).get("id").asLong();

        mockMvc.perform(get("/api/inventory/products/" + producto.productId() + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(14)); // 20 - 6

        mockMvc.perform(post("/api/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"cobro duplicado por error\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULADA"));

        mockMvc.perform(get("/api/inventory/products/" + producto.productId() + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(20)); // repuesto por completo

        // No se puede anular dos veces.
        mockMvc.perform(post("/api/sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"segundo intento\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void ventaRechazaStockInsuficienteYPagosQueNoCuadranConElTotal() throws Exception {
        ProductoCreado producto = crearProducto("Producto Stock Insuficiente", "999400000004", false, "100");
        agregarStockSinLote(producto.productId(), "5");

        String ventaExcesiva = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 10 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 1000 } ] }
                """.formatted(producto.presentationId());
        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(ventaExcesiva))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        String tarjetaExcedeElTotal = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 1 } ],
                  "payments": [ { "paymentMethod": "TARJETA", "amount": 500 } ] }
                """.formatted(producto.presentationId());
        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(tarjetaExcedeElTotal))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        String efectivoInsuficiente = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 2 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 50 } ] }
                """.formatted(producto.presentationId());
        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(efectivoInsuficiente))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        // Stock intacto: ninguna de las ventas anteriores debio aplicarse.
        mockMvc.perform(get("/api/inventory/products/" + producto.productId() + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(5));
    }

    @Test
    void ventasConcurrentesSobreElMismoProductoNuncaDejanElStockNegativo() throws Exception {
        ProductoCreado producto = crearProducto("Producto Venta Concurrente", "999400000005", false, "1000");
        agregarStockSinLote(producto.productId(), "30");

        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        String saleBody = """
                { "items": [ { "presentationId": %d, "quantityPresentation": 10 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 10000 } ] }
                """.formatted(producto.presentationId());

        List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, threads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    int statusCode = mockMvc.perform(post("/api/sales")
                                    .header("Authorization", "Bearer " + adminToken)
                                    .contentType(MediaType.APPLICATION_JSON).content(saleBody))
                            .andReturn().getResponse().getStatus();
                    if (statusCode == 201) {
                        created.incrementAndGet();
                    } else {
                        rejected.incrementAndGet();
                    }
                    return null;
                })
                .toList();

        List<Future<Void>> futures = tasks.stream().map(pool::submit).toList();
        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(created.get()).isEqualTo(3);
        assertThat(rejected.get()).isEqualTo(3);

        mockMvc.perform(get("/api/inventory/products/" + producto.productId() + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(0));
    }

    private record LoginPayload(String username, String password) {
    }
}
