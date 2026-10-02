package com.boticario.modules.inventory;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integracion end-to-end de inventario contra PostgreSQL real,
 * a nivel HTTP: ajustes positivos y negativos, la regla de que todo ajuste
 * negativo/merma/vencimiento debe indicar el lote afectado (caso de prueba
 * obligatorio del encargo), y las alertas de stock bajo y de vencimiento.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class InventoryAdjustmentIT {

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

    private long crearProducto(String nombre, String barcode, String minStockBase) throws Exception {
        String createBody = """
                {
                  "name": "%s",
                  "baseUnitCode": "PASTILLA",
                  "minStockBase": %s,
                  "requiresPrescription": false,
                  "fractionable": true,
                  "controlsLot": true,
                  "controlsExpiration": true,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 300, "barcode": "%s" }
                }
                """.formatted(nombre, minStockBase, barcode);

        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void ajustePositivoYNegativoActualizanStockYKardexCorrectamente() throws Exception {
        long productId = crearProducto("Producto Ajustable", "999100000001", "10");

        // Ajuste positivo: entra un lote de 100 unidades.
        String positivo = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": 100,
                  "reason": "carga inicial de inventario", "lotCode": "LOTE-A", "expirationDate": "%s" }
                """.formatted(productId, LocalDate.now().plusMonths(6));

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positivo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentType").value("POSITIVO"));

        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityBase").value(100))
                .andExpect(jsonPath("$.lowStock").value(false));

        String lotsResponse = mockMvc.perform(get("/api/inventory/products/" + productId + "/lots")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long lotId = objectMapper.readTree(lotsResponse).get(0).get("id").asLong();

        // Ajuste negativo dirigido a ese lote (merma de 30 unidades).
        String negativo = """
                { "productId": %d, "adjustmentType": "MERMA", "quantityBase": 30,
                  "reason": "unidades danadas en bodega", "lotId": %d }
                """.formatted(productId, lotId);

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentType").value("MERMA"));

        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityBase").value(70));

        // El kardex debe reflejar exactamente los dos movimientos, mas reciente primero.
        mockMvc.perform(get("/api/inventory/products/" + productId + "/kardex")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].movementType").value("MERMA"))
                .andExpect(jsonPath("$.content[0].quantityBase").value(-30))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(70))
                .andExpect(jsonPath("$.content[1].movementType").value("AJUSTE_POSITIVO"));
    }

    @Test
    void ajusteNegativoSinLoteEsRechazadoConConflicto() throws Exception {
        long productId = crearProducto("Producto Sin Lote Indicado", "999100000002", "0");

        String positivo = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": 50,
                  "reason": "carga inicial", "lotCode": "LOTE-B", "expirationDate": "%s" }
                """.formatted(productId, LocalDate.now().plusMonths(3));
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(positivo))
                .andExpect(status().isCreated());

        // Un ajuste NEGATIVO sin lotId debe rechazarse: el sistema nunca elige por FEFO
        // en un ajuste manual, quien ajusta ya debe saber que lote esta afectando.
        String negativoSinLote = """
                { "productId": %d, "adjustmentType": "NEGATIVO", "quantityBase": 10, "reason": "conteo fisico" }
                """.formatted(productId);

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(negativoSinLote))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void unAjusteQueExcedeElStockDelLoteEsRechazadoYNoAlteraElStock() throws Exception {
        long productId = crearProducto("Producto Stock Limitado", "999100000003", "0");

        String positivo = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": 20,
                  "reason": "carga inicial", "lotCode": "LOTE-C", "expirationDate": "%s" }
                """.formatted(productId, LocalDate.now().plusMonths(1));
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(positivo))
                .andExpect(status().isCreated());

        String lotsResponse = mockMvc.perform(get("/api/inventory/products/" + productId + "/lots")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        long lotId = objectMapper.readTree(lotsResponse).get(0).get("id").asLong();

        String negativoExcesivo = """
                { "productId": %d, "adjustmentType": "VENCIMIENTO", "quantityBase": 999, "reason": "vencio todo", "lotId": %d }
                """.formatted(productId, lotId);

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(negativoExcesivo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        // El stock nunca debe quedar negativo ni alterado por la operacion rechazada.
        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityBase").value(20));
    }

    @Test
    void alertaDeStockBajoIncluyeProductoPorDebajoDelMinimo() throws Exception {
        long productId = crearProducto("Producto Stock Bajo", "999100000004", "50");

        String positivo = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": 20,
                  "reason": "carga inicial insuficiente", "lotCode": "LOTE-D", "expirationDate": "%s" }
                """.formatted(productId, LocalDate.now().plusMonths(2));
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(positivo))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/alerts/low-stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == " + productId + ")]").isNotEmpty());
    }

    @Test
    void alertasDeVencimientoDistinguenLotesVencidosDeProximosAVencer() throws Exception {
        long productId = crearProducto("Producto Con Vencimientos", "999100000005", "0");

        String loteVencido = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": 15,
                  "reason": "lote proximo a vencer", "lotCode": "LOTE-VENCIDO", "expirationDate": "%s" }
                """.formatted(productId, LocalDate.now().minusDays(1));
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(loteVencido))
                .andExpect(status().isCreated());

        String loteProntoAVencer = """
                { "productId": %d, "adjustmentType": "POSITIVO", "quantityBase": 25,
                  "reason": "lote dentro de la ventana de alerta", "lotCode": "LOTE-PRONTO", "expirationDate": "%s" }
                """.formatted(productId, LocalDate.now().plusDays(5));
        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(loteProntoAVencer))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/alerts/expired-lots")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.lotCode == 'LOTE-VENCIDO')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.lotCode == 'LOTE-PRONTO')]").isEmpty());

        mockMvc.perform(get("/api/inventory/alerts/expiring-lots")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.lotCode == 'LOTE-PRONTO')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.lotCode == 'LOTE-VENCIDO')]").isEmpty());
    }

    private record LoginPayload(String username, String password) {
    }
}
