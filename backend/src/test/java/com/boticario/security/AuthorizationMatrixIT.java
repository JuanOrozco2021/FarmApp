package com.boticario.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorizacion por rol (Fase 10) — la prueba mas valiosa que se
 * agrega en esta fase final: para cada rol del sistema, confirma que
 * puede o no puede llamar un endpoint representativo de cada permiso
 * "de escritura" principal. Nunca se habia probado esto de forma
 * centralizada en las 9 fases anteriores (cada *FlowIT prueba como mucho
 * uno o dos casos de rechazo puntuales).
 * <p>
 * IMPORTANTE: esta prueba fue la que habria detectado de inmediato el
 * hallazgo critico de esta fase — que faltaba {@code @EnableMethodSecurity}
 * en SecurityConfig, por lo que NINGUN @PreAuthorize se aplicaba antes de
 * corregirlo (cualquier usuario autenticado podia llamar cualquier
 * endpoint sin importar su rol).
 * <p>
 * Estrategia para no depender de datos reales: los endpoints con id se
 * llaman con un id que casi seguro no existe (999999999). Si la
 * autorizacion pasa, el metodo de negocio corre y normalmente responde
 * 404/400/200 segun el caso — lo unico que esta prueba afirma para un rol
 * "permitido" es que la respuesta NO sea 403; para un rol "denegado",
 * que sea exactamente 403 con codigo ACCESS_DENIED, antes de que el
 * cuerpo de la solicitud importe.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationMatrixIT {

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

    private final Map<String, String> tokensByRole = new HashMap<>();

    @BeforeAll
    void crearUnUsuarioPorRolYAutenticarlos() throws Exception {
        String adminToken = login("admin", "Admin123!");
        tokensByRole.put("ADMIN", adminToken);

        tokensByRole.put("FARMACEUTICO", crearUsuarioYLoguear(adminToken, "farmaceutico.matriz", "FARMACEUTICO"));
        tokensByRole.put("CAJERO", crearUsuarioYLoguear(adminToken, "cajero.matriz", "CAJERO"));
        tokensByRole.put("INYECTOLOGIA", crearUsuarioYLoguear(adminToken, "inyectologia.matriz", "INYECTOLOGIA"));
    }

    private String crearUsuarioYLoguear(String adminToken, String username, String roleCode) throws Exception {
        String body = """
                { "username": "%s", "email": "%s@boticario.test", "password": "Matriz1234!",
                  "fullName": "Usuario Matriz %s", "roleCodes": ["%s"] }
                """.formatted(username, username, roleCode, roleCode);
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        return login(username, "Matriz1234!");
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(username, password));
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    static Stream<Arguments> casos() {
        String razonBody = "{ \"reason\": \"prueba de matriz de autorizacion\" }";
        String ajusteBody = """
                { "productId": 999999999, "adjustmentType": "POSITIVO", "quantityBase": 1, "reason": "prueba de matriz" }
                """;
        String settingsBody = "{ \"pharmacyName\": \"Boticario Prueba Matriz\", \"expirationAlertDays\": 30 }";

        return Stream.of(
                // USER_MANAGE
                caso("ADMIN", "patch", "/api/users/999999999/active?active=true", null, false),
                caso("FARMACEUTICO", "patch", "/api/users/999999999/active?active=true", null, true),
                caso("CAJERO", "patch", "/api/users/999999999/active?active=true", null, true),
                caso("INYECTOLOGIA", "patch", "/api/users/999999999/active?active=true", null, true),

                // PRODUCT_MANAGE (categorias, mismo permiso que productos/presentaciones)
                caso("FARMACEUTICO", "post", "/api/categories", "{ \"name\": \"Categoria Matriz FARMACEUTICO\" }", false),
                caso("CAJERO", "post", "/api/categories", "{ \"name\": \"Categoria Matriz CAJERO\" }", true),
                caso("INYECTOLOGIA", "post", "/api/categories", "{ \"name\": \"Categoria Matriz INYECTOLOGIA\" }", true),

                // CUSTOMER_MANAGE: lo tienen ADMIN, FARMACEUTICO y CAJERO; NO lo tiene INYECTOLOGIA
                caso("FARMACEUTICO", "patch", "/api/customers/999999999/status?active=true", null, false),
                caso("CAJERO", "patch", "/api/customers/999999999/status?active=true", null, false),
                caso("INYECTOLOGIA", "patch", "/api/customers/999999999/status?active=true", null, true),

                // SALE_CANCEL: lo tienen ADMIN y FARMACEUTICO; CAJERO (quien registra) NO
                caso("FARMACEUTICO", "post", "/api/sales/999999999/cancel", razonBody, false),
                caso("CAJERO", "post", "/api/sales/999999999/cancel", razonBody, true),
                caso("INYECTOLOGIA", "post", "/api/sales/999999999/cancel", razonBody, true),

                // INJECTION_CANCEL: lo tienen ADMIN y FARMACEUTICO; INYECTOLOGIA (quien aplica) NO
                caso("FARMACEUTICO", "post", "/api/injections/999999999/cancel", razonBody, false),
                caso("INYECTOLOGIA", "post", "/api/injections/999999999/cancel", razonBody, true),
                caso("CAJERO", "post", "/api/injections/999999999/cancel", razonBody, true),

                // PURCHASE_MANAGE
                caso("FARMACEUTICO", "post", "/api/purchases/999999999/cancel", null, false),
                caso("CAJERO", "post", "/api/purchases/999999999/cancel", null, true),
                caso("INYECTOLOGIA", "post", "/api/purchases/999999999/cancel", null, true),

                // INVENTORY_ADJUST
                caso("FARMACEUTICO", "post", "/api/inventory/adjustments", ajusteBody, false),
                caso("CAJERO", "post", "/api/inventory/adjustments", ajusteBody, true),
                caso("INYECTOLOGIA", "post", "/api/inventory/adjustments", ajusteBody, true),

                // SETTINGS_MANAGE
                caso("FARMACEUTICO", "put", "/api/settings", settingsBody, false),
                caso("CAJERO", "put", "/api/settings", settingsBody, true),
                caso("INYECTOLOGIA", "put", "/api/settings", settingsBody, true),

                // AUDIT_VIEW: solo ADMIN
                caso("ADMIN", "get", "/api/audit-logs", null, false),
                caso("FARMACEUTICO", "get", "/api/audit-logs", null, true),
                caso("CAJERO", "get", "/api/audit-logs", null, true),
                caso("INYECTOLOGIA", "get", "/api/audit-logs", null, true)
        );
    }

    private static Arguments caso(String rol, String metodo, String path, String body, boolean debeSerDenegado) {
        return Arguments.of(rol, metodo, path, body, debeSerDenegado);
    }

    @ParameterizedTest(name = "[{index}] {0} {1} {2} -> denegado={3}")
    @MethodSource("casos")
    void verificaAccesoSegunRol(String rol, String metodo, String path, String body, boolean debeSerDenegado) throws Exception {
        String token = tokensByRole.get(rol);

        MockHttpServletRequestBuilder request = switch (metodo) {
            case "get" -> get(path);
            case "post" -> post(path);
            case "patch" -> patch(path);
            case "put" -> put(path);
            default -> throw new IllegalArgumentException("Metodo HTTP no soportado en la matriz: " + metodo);
        };

        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }

        var result = mockMvc.perform(request).andReturn();
        int actualStatus = result.getResponse().getStatus();

        if (debeSerDenegado) {
            org.assertj.core.api.Assertions.assertThat(actualStatus)
                    .as("%s %s %s deberia estar denegado para %s pero respondio %d", metodo, path, body, rol, actualStatus)
                    .isEqualTo(403);
        } else {
            org.assertj.core.api.Assertions.assertThat(actualStatus)
                    .as("%s %s %s no deberia estar denegado para %s (paso la autorizacion) pero respondio 403", metodo, path, body, rol)
                    .isNotEqualTo(403);
        }
    }

    private record LoginPayload(String username, String password) {
    }
}
