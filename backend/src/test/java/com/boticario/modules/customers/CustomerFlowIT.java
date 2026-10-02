package com.boticario.modules.customers;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integracion end-to-end del modulo de clientes (Fase 7) contra
 * PostgreSQL real: alta con documento, rechazo de documento duplicado,
 * rechazo de numero sin tipo de documento, una venta ligada a un cliente
 * real que queda en su historial (GET /api/sales?customerId=), rechazo de
 * venta contra un cliente inactivo, y el enriquecimiento de proveedores
 * (contactName/paymentTermsDays/notes).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CustomerFlowIT {

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

    private long crearCliente(String nombre, String documentNumber) throws Exception {
        String body = """
                { "documentType": "CC", "documentNumber": "%s", "name": "%s", "phone": "3000000000" }
                """.formatted(documentNumber, nombre);
        String response = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private record ProductoCreado(long productId, long presentationId) {
    }

    private ProductoCreado crearProducto(String nombre, String barcode) throws Exception {
        String createBody = """
                {
                  "name": "%s",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true,
                  "controlsLot": false, "controlsExpiration": false,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 500, "barcode": "%s" }
                }
                """.formatted(nombre, barcode);
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

    @Test
    void creaClienteYLoEncuentraPorNombreOPorDocumento() throws Exception {
        crearCliente("Maria Perez", "1000111222");

        mockMvc.perform(get("/api/customers").param("search", "Maria Perez")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].documentNumber").value("1000111222"));

        mockMvc.perform(get("/api/customers").param("search", "1000111222")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Maria Perez"));
    }

    @Test
    void rechazaDocumentoDuplicadoYNumeroSinTipoDeDocumento() throws Exception {
        crearCliente("Cliente Uno", "2000333444");

        String duplicado = """
                { "documentType": "CC", "documentNumber": "2000333444", "name": "Otro Cliente" }
                """;
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(duplicado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        String sinTipo = """
                { "documentNumber": "9999999999", "name": "Cliente Sin Tipo" }
                """;
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(sinTipo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void unaVentaLigadaAUnClienteQuedaEnSuHistorialYCopiaElNombre() throws Exception {
        long clienteId = crearCliente("Cliente Historial", "3000555666");
        ProductoCreado producto = crearProducto("Producto Cliente Historial", "999500000001");
        agregarStockSinLote(producto.productId(), "10");

        String ventaBody = """
                { "customerId": %d,
                  "items": [ { "presentationId": %d, "quantityPresentation": 2 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 1000 } ] }
                """.formatted(clienteId, producto.presentationId());

        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(ventaBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(clienteId))
                .andExpect(jsonPath("$.customerName").value("Cliente Historial"));

        mockMvc.perform(get("/api/sales").param("customerId", String.valueOf(clienteId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value("Cliente Historial"));
    }

    @Test
    void rechazaUnaVentaContraUnClienteInactivo() throws Exception {
        long clienteId = crearCliente("Cliente Inactivo", "4000777888");
        mockMvc.perform(patch("/api/customers/" + clienteId + "/status").param("active", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        ProductoCreado producto = crearProducto("Producto Cliente Inactivo", "999500000002");
        agregarStockSinLote(producto.productId(), "10");

        String ventaBody = """
                { "customerId": %d,
                  "items": [ { "presentationId": %d, "quantityPresentation": 1 } ],
                  "payments": [ { "paymentMethod": "EFECTIVO", "amount": 500 } ] }
                """.formatted(clienteId, producto.presentationId());

        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(ventaBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void actualizaUnProveedorConLosDatosDeContactoEnriquecidos() throws Exception {
        String createBody = """
                { "name": "Proveedor Enriquecido", "taxId": "900999888-1", "phone": "3010000000",
                  "email": "contacto@proveedor.com", "address": "Calle 10" }
                """;
        String response = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long supplierId = objectMapper.readTree(response).get("id").asLong();

        String updateBody = """
                { "name": "Proveedor Enriquecido", "taxId": "900999888-1", "phone": "3010000000",
                  "email": "contacto@proveedor.com", "address": "Calle 10",
                  "contactName": "Ana Gomez", "paymentTermsDays": 30, "notes": "Paga contraentrega en la primera compra" }
                """;
        mockMvc.perform(put("/api/suppliers/" + supplierId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("Ana Gomez"))
                .andExpect(jsonPath("$.paymentTermsDays").value(30))
                .andExpect(jsonPath("$.notes").value("Paga contraentrega en la primera compra"));
    }

    private record LoginPayload(String username, String password) {
    }
}
