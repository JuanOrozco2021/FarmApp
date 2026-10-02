package com.boticario.modules.inventory;

import com.boticario.modules.inventory.application.LotAllocation;
import com.boticario.modules.inventory.application.StockAllocationService;
import com.boticario.modules.inventory.domain.MovementType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ayudante SOLO para pruebas: StockAllocationService exige (a proposito)
 * que el llamador ya tenga una transaccion abierta (Propagation.MANDATORY),
 * porque en produccion siempre es una venta/compra/ajuste quien abre esa
 * transaccion. Este componente abre una transaccion nueva e independiente
 * por invocacion (REQUIRES_NEW) para poder ejercitar el motor de FEFO y
 * concurrencia directamente, simulando N operaciones concurrentes reales
 * (cada una en su propia transaccion, como ocurriria con N cajas vendiendo
 * al mismo tiempo).
 */
@Component
class TxTestHelper {

    private final StockAllocationService stockAllocationService;

    TxTestHelper(StockAllocationService stockAllocationService) {
        this.stockAllocationService = stockAllocationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LotAllocation increase(Long productId, String lotCode, LocalDate expirationDate, BigDecimal quantityBase) {
        return stockAllocationService.increaseStock(productId, lotCode, expirationDate, quantityBase,
                MovementType.COMPRA, "TEST", null, null, "seed de prueba");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<LotAllocation> decreaseFefo(Long productId, BigDecimal quantityBase) {
        return stockAllocationService.decreaseStockFefo(productId, quantityBase,
                MovementType.VENTA, "TEST", null, null, "consumo de prueba");
    }
}
