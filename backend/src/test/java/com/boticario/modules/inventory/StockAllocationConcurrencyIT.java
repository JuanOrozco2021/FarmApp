package com.boticario.modules.inventory;

import com.boticario.modules.inventory.application.LotAllocation;
import com.boticario.modules.inventory.domain.InsufficientStockException;
import com.boticario.modules.inventory.infrastructure.InventoryStockRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas obligatorias del encargo sobre el motor de StockAllocationService:
 * caso 5 (FEFO: se consume primero el lote que vence antes) y caso 4
 * (concurrencia: N operaciones simultaneas sobre el mismo producto nunca
 * pierden actualizaciones ni permiten que el stock quede negativo).
 *
 * Se prueba contra el servicio directamente (no via HTTP) porque FEFO
 * automatico (decreaseStockFefo) todavia no tiene un endpoint propio: lo
 * usaran ventas e inyectologia en fases futuras. Los ajustes manuales de
 * esta fase, en cambio, siempre exigen el lote explicito.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class StockAllocationConcurrencyIT {

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

    @Autowired
    private TxTestHelper txTestHelper;

    @Autowired
    private InventoryStockRepository stockRepository;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload("admin", "Admin123!"));
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(response).get("accessToken").asText();
    }

    private long crearProducto(String nombre, String barcode, boolean controlsLot) throws Exception {
        String createBody = """
                {
                  "name": "%s",
                  "baseUnitCode": "PASTILLA",
                  "minStockBase": 0,
                  "requiresPrescription": false,
                  "fractionable": true,
                  "controlsLot": %s,
                  "controlsExpiration": %s,
                  "basePresentation": { "unitCode": "PASTILLA", "conversionFactor": 1, "salePrice": 300, "barcode": "%s" }
                }
                """.formatted(nombre, controlsLot, controlsLot, barcode);

        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void decreaseStockFefoConsumePrimeroElLoteQueVenceAntes() throws Exception {
        long productId = crearProducto("Producto FEFO", "999200000001", true);

        // Lote que vence lejos, cargado primero...
        txTestHelper.increase(productId, "LOTE-LEJOS", LocalDate.now().plusMonths(6), new BigDecimal("50"));
        // ...pero el lote que vence pronto, cargado despues, debe consumirse primero.
        txTestHelper.increase(productId, "LOTE-CERCA", LocalDate.now().plusDays(10), new BigDecimal("30"));

        List<LotAllocation> allocations = txTestHelper.decreaseFefo(productId, new BigDecimal("40"));

        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).lotCode()).isEqualTo("LOTE-CERCA");
        assertThat(allocations.get(0).quantityBase()).isEqualByComparingTo("30");
        assertThat(allocations.get(1).lotCode()).isEqualTo("LOTE-LEJOS");
        assertThat(allocations.get(1).quantityBase()).isEqualByComparingTo("10");

        BigDecimal finalStock = stockRepository.findById(productId).orElseThrow().getQuantityBase();
        assertThat(finalStock).isEqualByComparingTo("40"); // 50 + 30 - 40
    }

    @Test
    void operacionesConcurrentesSobreElMismoProductoNoPierdenActualizaciones() throws Exception {
        long productId = crearProducto("Producto Concurrencia Suficiente", "999200000002", false);
        txTestHelper.increase(productId, null, null, new BigDecimal("1000"));

        int threads = 20;
        BigDecimal perThread = new BigDecimal("10");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, threads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    startGate.await();
                    txTestHelper.decreaseFefo(productId, perThread);
                    return null;
                })
                .toList();

        // submit() (no invokeAll) para que las tareas queden en cola SIN ejecutarse
        // todavia; solo al soltar el latch arrancan practicamente a la vez.
        List<Future<Void>> futures = tasks.stream().map(pool::submit).toList();
        startGate.countDown();
        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        BigDecimal expected = new BigDecimal("1000").subtract(perThread.multiply(new BigDecimal(threads)));
        BigDecimal finalStock = stockRepository.findById(productId).orElseThrow().getQuantityBase();
        // Si el lock pesimista fallara, algunas restas se perderian y el stock final
        // quedaria por encima de lo esperado (actualizacion perdida).
        assertThat(finalStock).isEqualByComparingTo(expected);
    }

    @Test
    void bajoSobredemandaConcurrenteElStockNuncaQuedaNegativoYElExcedenteFalla() throws Exception {
        long productId = crearProducto("Producto Concurrencia Insuficiente", "999200000003", false);
        txTestHelper.increase(productId, null, null, new BigDecimal("50"));

        int threads = 10;
        BigDecimal perThread = new BigDecimal("10"); // demanda total = 100, solo hay 50
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Callable<Void>> tasks = java.util.stream.IntStream.range(0, threads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    startGate.await();
                    try {
                        txTestHelper.decreaseFefo(productId, perThread);
                        succeeded.incrementAndGet();
                    } catch (InsufficientStockException ex) {
                        rejected.incrementAndGet();
                    }
                    return null;
                })
                .toList();

        List<Future<Void>> futures = tasks.stream().map(pool::submit).toList();
        startGate.countDown();
        for (Future<Void> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(succeeded.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(5);

        BigDecimal finalStock = stockRepository.findById(productId).orElseThrow().getQuantityBase();
        assertThat(finalStock).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(finalStock.signum()).isGreaterThanOrEqualTo(0);
    }

    private record LoginPayload(String username, String password) {
    }
}
