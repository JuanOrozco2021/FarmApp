package com.boticario.modules.audit;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integracion del aspecto de auditoria AOP (Fase 10): confirma
 * que una escritura exitosa deja una fila en audit_logs con la accion, el
 * id de entidad y el resultado correctos, que una escritura que FALLA
 * (regla de negocio o duplicado) nunca deja una fila a medias, y que
 * GET /api/audit-logs esta protegido por AUDIT_VIEW (solo ADMIN).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuditFlowIT {

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

    private JsonNode buscarAuditoria(String entityName, String action) throws Exception {
        String response = mockMvc.perform(get("/api/audit-logs")
                        .param("entityName", entityName)
                        .param("action", action)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    @Test
    void unaCreacionExitosaQuedaRegistradaEnAuditoriaConElResultado() throws Exception {
        String body = """
                { "name": "Categoria Auditada Fase 10" }
                """;
        String response = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long categoriaId = objectMapper.readTree(response).get("id").asLong();

        JsonNode page = buscarAuditoria("ProductCategory", "CATALOG_CATEGORY_CREATE");
        JsonNode content = page.get("content");

        JsonNode fila = null;
        for (JsonNode candidata : content) {
            if (candidata.get("entityId").asText().equals(String.valueOf(categoriaId))) {
                fila = candidata;
                break;
            }
        }
        assertThat(fila).as("debe existir una fila de auditoria para la categoria creada").isNotNull();
        assertThat(fila.get("action").asText()).isEqualTo("CATALOG_CATEGORY_CREATE");
        assertThat(fila.get("username").asText()).isEqualTo("admin");
        assertThat(fila.get("afterJson").asText()).contains("Categoria Auditada Fase 10");
    }

    @Test
    void unaCreacionQueFallaPorDuplicadoNoDejaFilaDeAuditoria() throws Exception {
        String body = """
                { "name": "Categoria Duplicada Fase 10" }
                """;
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // El segundo intento con el mismo nombre falla (regla de duplicado) y
        // NUNCA debe generar una segunda fila de auditoria para esta accion.
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        JsonNode page = buscarAuditoria("ProductCategory", "CATALOG_CATEGORY_CREATE");
        long coincidencias = 0;
        for (JsonNode fila : page.get("content")) {
            if (fila.get("afterJson").asText().contains("Categoria Duplicada Fase 10")) {
                coincidencias++;
            }
        }
        assertThat(coincidencias).as("solo la creacion exitosa debe quedar auditada, no el intento fallido").isEqualTo(1);
    }

    @Test
    void unCambioDeEstadoQuedaAuditadoConLaSolicitudComoBeforeJson() throws Exception {
        String createCustomer = """
                { "name": "Cliente Para Auditoria" }
                """;
        String customerResponse = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createCustomer))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long customerId = objectMapper.readTree(customerResponse).get("id").asLong();

        mockMvc.perform(patch("/api/customers/" + customerId + "/status").param("active", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        JsonNode page = buscarAuditoria("Customer", "CUSTOMER_STATUS_CHANGE");
        JsonNode fila = null;
        for (JsonNode candidata : page.get("content")) {
            if (candidata.get("entityId").asText().equals(String.valueOf(customerId))) {
                fila = candidata;
                break;
            }
        }
        assertThat(fila).as("debe existir una fila de auditoria para el cambio de estado").isNotNull();
        assertThat(fila.get("beforeJson").asText()).contains("false");
    }

    @Test
    void consultarAuditoriaRequierePermisoAuditView() throws Exception {
        String createCajero = """
                { "username": "cajero.auditoria", "email": "cajero.auditoria@boticario.test", "password": "Cajero123!",
                  "fullName": "Cajero Sin Permiso De Auditoria", "roleCodes": ["CAJERO"] }
                """;
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createCajero))
                .andExpect(status().isCreated());

        String cajeroToken = login("cajero.auditoria", "Cajero123!");

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + cajeroToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private record LoginPayload(String username, String password) {
    }
}
