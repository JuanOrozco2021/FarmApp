package com.boticario.security;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion del endurecimiento de seguridad de la Fase 10:
 * confirma que las cabeceras HTTP configuradas explicitamente en
 * SecurityConfig realmente llegan en la respuesta, y que un error no
 * controlado nunca filtra detalle interno (stack trace, nombre de clase
 * de excepcion) en el cuerpo de la respuesta al cliente.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersIT {

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

    @Test
    void lasRespuestasIncluyenLasCabecerasDeSeguridadBasicas() throws Exception {
        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void unaRequestSeguraIncluyeStrictTransportSecurity() throws Exception {
        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + adminToken).secure(true))
                .andExpect(status().isOk())
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    void unCuerpoDeJsonMalformadoNuncaFiltraDetalleInternoAlCliente() throws Exception {
        // Un JSON invalido dispara una excepcion no controlada por los
        // @ExceptionHandler especificos (no es NotFound/BusinessRule/etc.) y
        // cae en el manejador generico — el cuerpo de respuesta jamas debe
        // traer un stack trace ni el nombre de una clase de excepcion Java.
        String respuesta = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json valido"))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(respuesta).doesNotContain("Exception");
        org.assertj.core.api.Assertions.assertThat(respuesta).doesNotContain("java.lang");
        org.assertj.core.api.Assertions.assertThat(respuesta).doesNotContain("com.boticario");
    }

    private record LoginPayload(String username, String password) {
    }
}
