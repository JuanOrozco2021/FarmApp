package com.boticario.modules.injections;

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
 * Prueba de integracion end-to-end del modulo de inyectologia (Fase 8)
 * contra PostgreSQL real: decision confirmada de que el medicamento
 * siempre lo trae el paciente (nunca se toca inventario), registro con
 * cliente registrado (copia el nombre) y con paciente de texto libre,
 * rechazo cuando no hay ni cliente ni nombre, rechazo contra cliente
 * inactivo, rechazo de una referencia a venta inexistente, y la regla de
 * que INYECTOLOGIA puede registrar pero no anular su propio cobro.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class InjectionFlowIT {

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

    private long crearCliente(String nombre, String documentNumber) throws Exception {
        String body = """
                { "documentType": "CC", "documentNumber": "%s", "name": "%s" }
                """.formatted(documentNumber, nombre);
        String response = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void registraUnServicioConClienteRegistradoYCopiaElNombre() throws Exception {
        long clienteId = crearCliente("Paciente Con Cliente", "5000111222");

        String body = """
                { "customerId": %d, "medicationName": "Diclofenaco 75mg", "dose": "1 ampolla",
                  "applicationSite": "gluteo derecho", "fee": 8000, "paymentMethod": "EFECTIVO" }
                """.formatted(clienteId);

        mockMvc.perform(post("/api/injections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(clienteId))
                .andExpect(jsonPath("$.patientName").value("Paciente Con Cliente"))
                .andExpect(jsonPath("$.status").value("ACTIVA"))
                .andExpect(jsonPath("$.injectionCode").isNotEmpty());
    }

    @Test
    void registraUnServicioConPacienteDeTextoLibreSinCliente() throws Exception {
        String body = """
                { "patientName": "Juan Sin Registrar", "medicationName": "Vitamina B12",
                  "fee": 5000, "paymentMethod": "TARJETA" }
                """;

        mockMvc.perform(post("/api/injections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andExpect(jsonPath("$.patientName").value("Juan Sin Registrar"));
    }

    @Test
    void rechazaUnServicioSinClienteNiNombreDePaciente() throws Exception {
        String body = """
                { "medicationName": "Vitamina B12", "fee": 5000, "paymentMethod": "EFECTIVO" }
                """;

        mockMvc.perform(post("/api/injections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void rechazaUnServicioContraUnClienteInactivo() throws Exception {
        long clienteId = crearCliente("Paciente Inactivo", "5000333444");
        mockMvc.perform(patch("/api/customers/" + clienteId + "/status").param("active", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String body = """
                { "customerId": %d, "medicationName": "Vitamina B12", "fee": 5000, "paymentMethod": "EFECTIVO" }
                """.formatted(clienteId);

        mockMvc.perform(post("/api/injections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void rechazaUnaReferenciaAUnaVentaInexistente() throws Exception {
        String body = """
                { "patientName": "Paciente Con Venta Falsa", "medicationName": "Vitamina B12",
                  "relatedSaleId": 999999, "fee": 5000, "paymentMethod": "EFECTIVO" }
                """;

        mockMvc.perform(post("/api/injections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void inyectologiaPuedeRegistrarPeroNoAnularSuPropioCobro() throws Exception {
        String createInyectologia = """
                { "username": "inyector.prueba", "email": "inyector.prueba@boticario.test", "password": "Inyector123!",
                  "fullName": "Inyector De Prueba", "roleCodes": ["INYECTOLOGIA"] }
                """;
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createInyectologia))
                .andExpect(status().isCreated());

        String inyectologiaToken = login("inyector.prueba", "Inyector123!");

        String body = """
                { "patientName": "Paciente De Inyector", "medicationName": "Diclofenaco",
                  "fee": 8000, "paymentMethod": "EFECTIVO" }
                """;

        String response = mockMvc.perform(post("/api/injections")
                        .header("Authorization", "Bearer " + inyectologiaToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long recordId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/injections/" + recordId + "/cancel")
                        .header("Authorization", "Bearer " + inyectologiaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"intento no autorizado\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/injections/" + recordId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"cobro duplicado por error\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULADA"));

        mockMvc.perform(post("/api/injections/" + recordId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"reason\": \"segundo intento\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private record LoginPayload(String username, String password) {
    }
}
