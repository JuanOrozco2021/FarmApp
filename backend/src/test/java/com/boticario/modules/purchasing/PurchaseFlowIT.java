package com.boticario.modules.purchasing;

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
 * Prueba de integracion end-to-end del modulo de compras contra PostgreSQL
 * real: crear una compra en BORRADOR con conversion de unidades (se compra
 * en CAJAS, el stock queda en PASTILLAs), recibirla (debe abrir el lote,
 * mover el kardex y actualizar el costo de compra vigente), devolver
 * parcialmente un item ya recibido, anular un borrador y las reglas de
 * negocio que protegen cada transicion de estado.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PurchaseFlowIT {

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

    private long crearProveedor(String nombre, String taxId) throws Exception {
        String body = """
                { "name": "%s", "taxId": "%s", "phone": "3000000000", "email": "compras@proveedor.com", "address": "Calle 1" }
                """.formatted(nombre, taxId);
        String response = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    /** Producto controla lote y vencimiento, con una presentacion CAJA de factor 100 ademas de la base. */
    private long crearProductoConCaja(String nombre, String barcodeBase, String barcodeCaja) throws Exception {
        String createBody = """
                {
                  "name": "%s",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true, "controlsLot": true, "controlsExpiration": true,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 500, "barcode": "%s" }
                }
                """.formatted(nombre, barcodeBase);
        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(createResponse).get("id").asLong();

        String cajaBody = """
                { "unitCode": "CAJA", "conversionFactor": 100, "salePrice": 45000, "barcode": "%s" }
                """.formatted(barcodeCaja);
        mockMvc.perform(post("/api/products/" + productId + "/presentations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(cajaBody))
                .andExpect(status().isCreated());

        return productId;
    }

    private long presentacionCajaId(long productId) throws Exception {
        String response = mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        JsonNode presentations = objectMapper.readTree(response).get("presentations");
        for (JsonNode p : presentations) {
            if ("CAJA".equals(p.get("unitCode").asText())) {
                return p.get("id").asLong();
            }
        }
        throw new IllegalStateException("Presentacion CAJA no encontrada");
    }

    @Test
    void flujoCompletoDeCompraConvierteUnidadesYActualizaInventarioYCosto() throws Exception {
        long supplierId = crearProveedor("Droguerias del Norte", "800111222-3");
        long productId = crearProductoConCaja("Producto Compra Completa", "999300000001", "999300000002");
        long presentationId = presentacionCajaId(productId);

        String createPurchase = """
                { "supplierId": %d, "invoiceNumber": "FAC-001", "purchaseDate": "%s", "notes": "compra de prueba",
                  "items": [ { "presentationId": %d, "quantityPresentation": 2, "unitCost": 40000,
                               "lotCode": "LOTE-COMPRA-1", "expirationDate": "%s" } ] }
                """.formatted(supplierId, LocalDate.now(), presentationId, LocalDate.now().plusMonths(6));

        String createResponse = mockMvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createPurchase))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BORRADOR"))
                .andExpect(jsonPath("$.totalAmount").value(80000))
                .andExpect(jsonPath("$.items[0].quantityBase").value(200))
                .andReturn().getResponse().getContentAsString();
        long purchaseId = objectMapper.readTree(createResponse).get("id").asLong();

        // Recibir la compra: abre el lote, mueve el kardex y fija el costo de compra vigente.
        mockMvc.perform(post("/api/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECIBIDA"))
                .andExpect(jsonPath("$.items[0].movementId").isNumber());

        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityBase").value(200));

        String productResponse = mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cajaPresentation = null;
        for (JsonNode p : objectMapper.readTree(productResponse).get("presentations")) {
            if ("CAJA".equals(p.get("unitCode").asText())) {
                cajaPresentation = p;
            }
        }
        org.assertj.core.api.Assertions.assertThat(cajaPresentation).isNotNull();
        org.assertj.core.api.Assertions.assertThat(cajaPresentation.get("purchasePrice").asDouble()).isEqualTo(40000.0);

        // No se puede recibir dos veces la misma compra.
        mockMvc.perform(post("/api/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void devolucionAProveedorDescuentaDelLoteOriginalYRespetaElDisponible() throws Exception {
        long supplierId = crearProveedor("Distribuidora Sur", "800222333-4");
        long productId = crearProductoConCaja("Producto Con Devolucion", "999300000003", "999300000004");
        long presentationId = presentacionCajaId(productId);

        String createPurchase = """
                { "supplierId": %d, "items": [ { "presentationId": %d, "quantityPresentation": 1, "unitCost": 40000,
                               "lotCode": "LOTE-DEV-1", "expirationDate": "%s" } ] }
                """.formatted(supplierId, presentationId, LocalDate.now().plusMonths(6));

        String createResponse = mockMvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createPurchase))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createResponse);
        long purchaseId = created.get("id").asLong();

        String receiveResponse = mockMvc.perform(post("/api/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long itemId = objectMapper.readTree(receiveResponse).get("items").get(0).get("id").asLong();

        // Stock tras recibir: 1 caja x 100 = 100 pastillas.
        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(100));

        // Devolver 30 unidades base al proveedor.
        String returnBody = """
                { "quantityBase": 30, "reason": "unidades danadas en el transporte" }
                """;
        mockMvc.perform(post("/api/purchases/" + purchaseId + "/items/" + itemId + "/return")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(returnBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].returnedQuantityBase").value(30));

        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(70));

        // No se puede devolver mas de lo que queda disponible del item (quedan 70 de los 100 originales).
        String exceso = """
                { "quantityBase": 80, "reason": "intento de devolucion excesiva" }
                """;
        mockMvc.perform(post("/api/purchases/" + purchaseId + "/items/" + itemId + "/return")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(exceso))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void anularUnaCompraEnBorradorNoAfectaInventario() throws Exception {
        long supplierId = crearProveedor("Proveedor A Anular", "800333444-5");
        long productId = crearProductoConCaja("Producto Compra Anulada", "999300000005", "999300000006");
        long presentationId = presentacionCajaId(productId);

        String createPurchase = """
                { "supplierId": %d, "items": [ { "presentationId": %d, "quantityPresentation": 1, "unitCost": 40000,
                               "lotCode": "LOTE-ANULADA", "expirationDate": "%s" } ] }
                """.formatted(supplierId, presentationId, LocalDate.now().plusMonths(6));

        String createResponse = mockMvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createPurchase))
                .andReturn().getResponse().getContentAsString();
        long purchaseId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/purchases/" + purchaseId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULADA"));

        mockMvc.perform(get("/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.quantityBase").value(0));

        // Una compra anulada no se puede recibir.
        mockMvc.perform(post("/api/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void recibirUnaCompraSinLoteParaProductoQueControlaLoteEsRechazada() throws Exception {
        long supplierId = crearProveedor("Proveedor Sin Lote", "800444555-6");
        long productId = crearProductoConCaja("Producto Sin Lote En Compra", "999300000007", "999300000008");
        long presentationId = presentacionCajaId(productId);

        // Sin lotCode ni expirationDate, para un producto que controla ambos.
        String createPurchase = """
                { "supplierId": %d, "items": [ { "presentationId": %d, "quantityPresentation": 1, "unitCost": 40000 } ] }
                """.formatted(supplierId, presentationId);

        String createResponse = mockMvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createPurchase))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long purchaseId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/api/purchases/" + purchaseId + "/receive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void noSePuedeRegistrarUnaCompraAUnProveedorInactivo() throws Exception {
        long supplierId = crearProveedor("Proveedor Que Se Desactiva", "800555666-7");
        long productId = crearProductoConCaja("Producto Proveedor Inactivo", "999300000009", "999300000010");
        long presentationId = presentacionCajaId(productId);

        mockMvc.perform(patch("/api/suppliers/" + supplierId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        String createPurchase = """
                { "supplierId": %d, "items": [ { "presentationId": %d, "quantityPresentation": 1, "unitCost": 40000,
                               "lotCode": "LOTE-X", "expirationDate": "%s" } ] }
                """.formatted(supplierId, presentationId, LocalDate.now().plusMonths(6));

        mockMvc.perform(post("/api/purchases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createPurchase))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private record LoginPayload(String username, String password) {
    }
}
