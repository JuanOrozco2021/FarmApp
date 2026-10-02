package com.boticario.modules.inventory.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.audit.CurrentUserProvider;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.modules.inventory.api.dto.AdjustmentDTO;
import com.boticario.modules.inventory.api.dto.CreateAdjustmentRequest;
import com.boticario.modules.inventory.domain.InventoryAdjustment;
import com.boticario.modules.inventory.domain.InventoryMovement;
import com.boticario.modules.catalog.infrastructure.ProductRepository;
import com.boticario.modules.inventory.infrastructure.InventoryAdjustmentRepository;
import com.boticario.modules.inventory.infrastructure.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto de entrada de los ajustes manuales de inventario (regla 33.1 del
 * encargo: "nunca modificar stock sin registrar un movimiento" — aqui
 * tambien queda registrada la INTENCION del usuario, ademas del movimiento).
 *
 * Un ajuste positivo entra a un lote (nuevo o existente, o al lote GENERAL
 * si el producto no controla lote). Un ajuste negativo, merma o
 * vencimiento SIEMPRE debe indicar exactamente que lote se afecta — a
 * diferencia de una venta, aqui no tiene sentido dejar que el sistema
 * elija por FEFO: quien hace el ajuste ya sabe (por conteo fisico o por
 * revision de vencimientos) cual lote es.
 */
@Service
public class InventoryAdjustmentService {

    private final StockAllocationService stockAllocationService;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final CurrentUserProvider currentUserProvider;

    public InventoryAdjustmentService(StockAllocationService stockAllocationService,
                                       InventoryAdjustmentRepository adjustmentRepository,
                                       InventoryMovementRepository movementRepository,
                                       ProductRepository productRepository,
                                       CurrentUserProvider currentUserProvider) {
        this.stockAllocationService = stockAllocationService;
        this.adjustmentRepository = adjustmentRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    @Audited(action = "INVENTORY_ADJUSTMENT_CREATE", entity = "InventoryAdjustment")
    public AdjustmentDTO create(CreateAdjustmentRequest request) {
        Long userId = currentUserProvider.currentUserId().orElse(null);

        LotAllocation allocation;
        if (request.adjustmentType().isPositive()) {
            allocation = stockAllocationService.increaseStock(
                    request.productId(), request.lotCode(), request.expirationDate(), request.quantityBase(),
                    request.adjustmentType().toMovementType(), "INVENTORY_ADJUSTMENT", null, userId, request.reason());
        } else {
            if (request.lotId() == null) {
                throw new BusinessRuleException(
                        "Debes indicar el lote afectado para un ajuste de tipo " + request.adjustmentType());
            }
            allocation = stockAllocationService.decreaseStockFromLot(
                    request.productId(), request.lotId(), request.quantityBase(),
                    request.adjustmentType().toMovementType(), "INVENTORY_ADJUSTMENT", null, userId, request.reason());
        }

        InventoryMovement movement = movementRepository.findById(allocation.movementId())
                .orElseThrow(() -> new NotFoundException("InventoryMovement", allocation.movementId()));

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setProduct(productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product", request.productId())));
        adjustment.setAdjustmentType(request.adjustmentType());
        adjustment.setQuantityBase(request.quantityBase());
        adjustment.setReason(request.reason());
        adjustment.setMovement(movement);
        adjustment.setUserId(userId);

        return AdjustmentDTO.from(adjustmentRepository.save(adjustment));
    }
}
