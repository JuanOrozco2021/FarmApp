package com.boticario.modules.catalog;

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
 * Prueba de integracion end-to-end del catalogo contra PostgreSQL real:
 * crear producto con presentacion base, agregar una presentacion con
 * factor 10 y su propio codigo de barras, resolver ese codigo, actualizar
 * el precio (verificando que quede historial) y rechazar un codigo
 * de barras duplicado.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductCatalogIT {

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
    void creaProductoConPresentacionBaseYPermiteAgregarUnaPresentacionConFactorDiez() throws Exception {
        String createBody = """
                {
                  "name": "Acetaminofen 500 mg",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false,
                  "fractionable": true,
                  "controlsLot": true,
                  "controlsExpiration": true,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 300, "barcode": "999000000001" }
                }
                """;

        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internalCode").value(org.hamcrest.Matchers.startsWith("PRD-")))
                .andExpect(jsonPath("$.presentations.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode product = objectMapper.readTree(createResponse);
        long productId = product.get("id").asLong();

        // Escanear el codigo de la presentacion base debe resolver factor 1.
        mockMvc.perform(get("/api/products/barcode/999000000001").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversionFactor").value(1));

        // Agregar presentacion TABLETA (blister de 10) con su propio codigo.
        String presentationBody = """
                { "unitCode": "TABLETA", "conversionFactor": 10, "salePrice": 2800, "barcode": "999000000002" }
                """;

        mockMvc.perform(post("/api/products/" + productId + "/presentations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presentationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversionFactor").value(10));

        // Escanear el codigo de la tableta debe resolver factor 10 (10 pastillas al carrito).
        mockMvc.perform(get("/api/products/barcode/999000000002").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversionFactor").value(10))
                .andExpect(jsonPath("$.productId").value(productId));
    }

    @Test
    void rechazaUnCodigoDeBarrasYaUsadoEnOtraPresentacion() throws Exception {
        String firstProduct = """
                {
                  "name": "Producto A",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true, "controlsLot": true, "controlsExpiration": true,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 100, "barcode": "999000000010" }
                }
                """;
        mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(firstProduct))
                .andExpect(status().isCreated());

        String secondProductWithSameBarcode = """
                {
                  "name": "Producto B",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true, "controlsLot": true, "controlsExpiration": true,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 100, "barcode": "999000000010" }
                }
                """;

        mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(secondProductWithSameBarcode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void actualizarElPrecioDeUnaPresentacionDejaHistorial() throws Exception {
        String createBody = """
                {
                  "name": "Producto con historial",
                  "baseUnitCode": "PASTILLA",
                  "requiresPrescription": false, "fractionable": true, "controlsLot": true, "controlsExpiration": true,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 1000 }
                }
                """;

        String createResponse = mockMvc.perform(post("/api/products").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long presentationId = objectMapper.readTree(createResponse).get("presentations").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/presentations/" + presentationId + "/price")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"newPrice\": 1200, \"reason\": \"ajuste de proveedor\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salePrice").value(1200));

        mockMvc.perform(get("/api/presentations/" + presentationId + "/price-history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].oldPrice").value(1000))
                .andExpect(jsonPath("$[0].newPrice").value(1200));
    }

    private record LoginPayload(String username, String password) {
    }
}
