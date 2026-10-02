package com.boticario.modules.reports;

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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integracion end-to-end de los reportes del Dashboard (Fase 9)
 * contra PostgreSQL real: el resumen de ventas excluye las anuladas y
 * calcula bien el ticket promedio y el desglose por forma de pago, el
 * ranking de productos mas vendidos ordena por ingresos y respeta el
 * limite, y el resumen de alertas de inventario responde de forma
 * consistente (la logica de cada alerta ya se prueba a fondo en
 * InventoryAdjustmentIT — aqui solo se verifica que el endpoint agrega
 * bien los conteos).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ReportFlowIT {

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
        String body = objectMapper.writeValueAsString(new LoginPayload("admin", "Admin123!"));
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(response).get("accessToken").asText();
    }

    private record ProductoCreado(long productId, long presentationId) {
    }

    private ProductoCreado crearProducto(String nombre, String barcode, String salePrice) throws Exception {
        String createBody = """
                {
                  "name": "%s",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true,
                  "controlsLot": false, "controlsExpiration": false,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": %s, "barcode": "%s" }
                }
                """.formatted(nombre, salePrice, barcode);
        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        return new ProductoCreado(created.get("id").asLong(), created.get("presentations").get(0).get("id").asLong());
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

    private long registrarVenta(long presentationId, String cantidad, String metodoPago, String montoPago) throws Exception {
        String body = """
                { "items": [ { "presentationId": %d, "quantityPresentation": %s } ],
                  "payments": [ { "paymentMethod": "%s", "amount": %s } ] }
                """.formatted(presentationId, cantidad, metodoPago, montoPago);
        String response = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void elResumenDeVentasExcluyeLasAnuladasYCalculaTicketPromedioYFormasDePago() throws Exception {
        Instant from = Instant.now().minusSeconds(5);

        ProductoCreado productoA = crearProducto("Reporte Producto A", "999600000001", "1000");
        ProductoCreado productoB = crearProducto("Reporte Producto B", "999600000002", "2000");
        agregarStockSinLote(productoA.productId(), "50");
        agregarStockSinLote(productoB.productId(), "50");

        registrarVenta(productoA.presentationId(), "2", "EFECTIVO", "2000");
        registrarVenta(productoB.presentationId(), "1", "TARJETA", "2000");
        long ventaAnulada = registrarVenta(productoA.presentationId(), "1", "EFECTIVO", "1000");

        mockMvc.perform(post("/api/sales/" + ventaAnulada + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"prueba de reporte\" }"))
                .andExpect(status().isOk());

        Instant to = Instant.now().plusSeconds(5);

        String response = mockMvc.perform(get("/api/reports/sales-summary")
                        .param("from", from.toString()).param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(2))
                .andExpect(jsonPath("$.totalRevenue").value(4000))
                .andExpect(jsonPath("$.averageTicket").value(2000))
                .andReturn().getResponse().getContentAsString();

        JsonNode summary = objectMapper.readTree(response);
        JsonNode breakdown = summary.get("paymentBreakdown");
        assertThat(breakdown.isArray()).isTrue();

        double efectivo = 0;
        double tarjeta = 0;
        for (JsonNode row : breakdown) {
            if (row.get("paymentMethod").asText().equals("EFECTIVO")) efectivo = row.get("amount").asDouble();
            if (row.get("paymentMethod").asText().equals("TARJETA")) tarjeta = row.get("amount").asDouble();
        }
        assertThat(efectivo).isEqualTo(2000.0);
        assertThat(tarjeta).isEqualTo(2000.0);
    }

    @Test
    void elRankingDeProductosOrdenaPorIngresosYRespetaElLimite() throws Exception {
        Instant from = Instant.now().minusSeconds(5);

        ProductoCreado productoA = crearProducto("Ranking Producto A", "999600000003", "1000");
        ProductoCreado productoB = crearProducto("Ranking Producto B", "999600000004", "3000");
        ProductoCreado productoC = crearProducto("Ranking Producto C", "999600000005", "500");
        agregarStockSinLote(productoA.productId(), "50");
        agregarStockSinLote(productoB.productId(), "50");
        agregarStockSinLote(productoC.productId(), "50");

        registrarVenta(productoA.presentationId(), "5", "EFECTIVO", "5000"); // ingresos 5000
        registrarVenta(productoB.presentationId(), "1", "EFECTIVO", "3000"); // ingresos 3000
        registrarVenta(productoC.presentationId(), "1", "EFECTIVO", "500");  // ingresos 500

        Instant to = Instant.now().plusSeconds(5);

        mockMvc.perform(get("/api/reports/top-products")
                        .param("from", from.toString()).param("to", to.toString()).param("limit", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Ranking Producto A"))
                .andExpect(jsonPath("$[0].revenue").value(5000))
                .andExpect(jsonPath("$[1].productName").value("Ranking Producto B"))
                .andExpect(jsonPath("$[1].revenue").value(3000));
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() throws Exception {
        Instant now = Instant.now();
        mockMvc.perform(get("/api/reports/sales-summary")
                        .param("from", now.toString()).param("to", now.minusSeconds(60).toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void elResumenDeAlertasDeInventarioRespondeConContadoresNoNegativos() throws Exception {
        mockMvc.perform(get("/api/reports/inventory-alerts-summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.expiringSoonCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.expiredCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)));
    }

    private record LoginPayload(String username, String password) {
    }
}
