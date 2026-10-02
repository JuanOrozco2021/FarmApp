package com.boticario.modules.inventory.application;

import com.boticario.common.exception.NotFoundException;
import com.boticario.modules.catalog.domain.InvalidQuantityException;
import com.boticario.modules.catalog.domain.Product;
import com.boticario.modules.catalog.infrastructure.ProductRepository;
import com.boticario.modules.inventory.domain.*;
import com.boticario.modules.inventory.infrastructure.InventoryLotRepository;
import com.boticario.modules.inventory.infrastructure.InventoryMovementRepository;
import com.boticario.modules.inventory.infrastructure.InventoryStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * UNICO servicio del sistema autorizado a escribir en inventory_stock,
 * inventory_lots e inventory_movements. Compras (Fase 5), ventas e
 * inyectologia (Fases 6/8) y los ajustes manuales de esta fase pasan
 * todos por aqui — asi la regla "nunca modificar stock sin registrar un
 * movimiento" y la estrategia de concurrencia (lock de la fila de stock
 * antes que nada) se cumplen en un solo lugar, no en cada modulo que
 * consume o repone inventario.
 *
 * Estrategia de concurrencia: toda operacion bloquea primero la fila de
 * inventory_stock del producto (SELECT ... FOR UPDATE via
 * findByProductIdForUpdate). Como absolutamente todas las mutaciones de
 * este producto pasan por ese mismo lock, dos operaciones simultaneas
 * sobre el mismo producto quedan serializadas aqui, sin bloquear el resto
 * del sistema. @Propagation.MANDATORY fuerza a que el llamador ya haya
 * abierto una transaccion (ventas/compras hacen todo su flujo en una sola
 * transaccion; no tendria sentido que este servicio abriera la suya propia).
 */
@Service
public class StockAllocationService {

    private final InventoryStockRepository stockRepository;
    private final InventoryLotRepository lotRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public StockAllocationService(InventoryStockRepository stockRepository,
                                   InventoryLotRepository lotRepository,
                                   InventoryMovementRepository movementRepository,
                                   ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.lotRepository = lotRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    /** Entrada de inventario: compras (Fase 5) y ajustes positivos/de esta fase. */
    @Transactional(propagation = Propagation.MANDATORY)
    public LotAllocation increaseStock(Long productId, String lotCode, LocalDate expirationDate,
                                        BigDecimal quantityBase, MovementType type,
                                        String referenceType, Long referenceId, Long userId, String notes) {
        requirePositive(quantityBase);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        InventoryStock stock = lockOrCreateStock(product);

        String effectiveLotCode = product.isControlsLot() && lotCode != null && !lotCode.isBlank()
                ? lotCode
                : InventoryLot.GENERAL_LOT_CODE;
        LocalDate effectiveExpiration = product.isControlsExpiration() ? expirationDate : null;

        InventoryLot lot = lotRepository.findByProductIdAndLotCode(productId, effectiveLotCode)
                .orElseGet(() -> {
                    InventoryLot newLot = new InventoryLot();
                    newLot.setProduct(product);
                    newLot.setLotCode(effectiveLotCode);
                    newLot.setExpirationDate(effectiveExpiration);
                    newLot.setQuantityBase(BigDecimal.ZERO);
                    return newLot;
                });
        lot.setQuantityBase(lot.getQuantityBase().add(quantityBase));
        if (lot.getExpirationDate() == null && effectiveExpiration != null) {
            lot.setExpirationDate(effectiveExpiration);
        }
        lot = lotRepository.save(lot);

        stock.setQuantityBase(stock.getQuantityBase().add(quantityBase));
        stock = stockRepository.save(stock);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setLot(lot);
        movement.setMovementType(type);
        movement.setQuantityBase(quantityBase);
        movement.setBalanceAfter(stock.getQuantityBase());
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setUserId(userId);
        movement.setNotes(notes);
        movement = movementRepository.save(movement);

        return new LotAllocation(lot.getId(), lot.getLotCode(), quantityBase, movement.getId());
    }

    /**
     * Salida de inventario con FEFO: ventas/inyectologia (Fases 6/8) y
     * ajustes negativos/merma/vencimiento de esta fase. Nunca deja el
     * stock en negativo — si no alcanza, revierte toda la operacion
     * lanzando InsufficientStockException (la transaccion del llamador
     * hace rollback completo).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<LotAllocation> decreaseStockFefo(Long productId, BigDecimal quantityBase, MovementType type,
                                                  String referenceType, Long referenceId, Long userId, String notes) {
        requirePositive(quantityBase);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        InventoryStock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InsufficientStockException(productId, quantityBase, BigDecimal.ZERO));

        if (stock.getQuantityBase().compareTo(quantityBase) < 0) {
            throw new InsufficientStockException(productId, quantityBase, stock.getQuantityBase());
        }

        List<InventoryLot> lots = lotRepository.findActiveLotsForUpdateFefo(productId);

        List<LotAllocation> allocations = new ArrayList<>();
        BigDecimal remaining = quantityBase;
        BigDecimal runningBalance = stock.getQuantityBase();

        for (InventoryLot lot : lots) {
            if (remaining.signum() == 0) {
                break;
            }
            BigDecimal consume = lot.getQuantityBase().min(remaining);
            if (consume.signum() <= 0) {
                continue;
            }

            lot.setQuantityBase(lot.getQuantityBase().subtract(consume));
            lotRepository.save(lot);

            runningBalance = runningBalance.subtract(consume);
            remaining = remaining.subtract(consume);

            InventoryMovement movement = new InventoryMovement();
            movement.setProduct(product);
            movement.setLot(lot);
            movement.setMovementType(type);
            movement.setQuantityBase(consume.negate());
            movement.setBalanceAfter(runningBalance);
            movement.setReferenceType(referenceType);
            movement.setReferenceId(referenceId);
            movement.setUserId(userId);
            movement.setNotes(notes);
            movement = movementRepository.save(movement);

            allocations.add(new LotAllocation(lot.getId(), lot.getLotCode(), consume, movement.getId()));
        }

        if (remaining.signum() > 0) {
            // Los lotes activos no alcanzaron a cubrir lo que inventory_stock decia disponible:
            // el agregado y el detalle por lote se desincronizaron. No debería ocurrir si todas
            // las escrituras pasan por este servicio — se aborta para no dejar el kardex inconsistente.
            throw new IllegalStateException(
                    "Inconsistencia de inventario: inventory_stock reporta mas stock del que existe en los lotes activos del producto "
                            + productId + ". Se requiere una auditoria manual.");
        }

        stock.setQuantityBase(stock.getQuantityBase().subtract(quantityBase));
        stockRepository.save(stock);

        return allocations;
    }

    /**
     * Salida dirigida a UN lote especifico, sin pasar por FEFO — para
     * mermas o vencimientos donde el usuario ya identifico exactamente
     * que lote se ve afectado (ej. "se vencieron 20 unidades del lote X").
     * Sigue bloqueando primero la fila de inventory_stock, igual que
     * decreaseStockFefo, para no romper la serializacion por producto.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LotAllocation decreaseStockFromLot(Long productId, Long lotId, BigDecimal quantityBase, MovementType type,
                                               String referenceType, Long referenceId, Long userId, String notes) {
        requirePositive(quantityBase);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        InventoryStock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InsufficientStockException(productId, quantityBase, BigDecimal.ZERO));

        if (stock.getQuantityBase().compareTo(quantityBase) < 0) {
            throw new InsufficientStockException(productId, quantityBase, stock.getQuantityBase());
        }

        InventoryLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new NotFoundException("InventoryLot", lotId));
        if (!lot.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("El lote " + lotId + " no pertenece al producto " + productId);
        }
        if (lot.getQuantityBase().compareTo(quantityBase) < 0) {
            throw new InsufficientStockException(productId, quantityBase, lot.getQuantityBase());
        }

        lot.setQuantityBase(lot.getQuantityBase().subtract(quantityBase));
        lotRepository.save(lot);

        stock.setQuantityBase(stock.getQuantityBase().subtract(quantityBase));
        stockRepository.save(stock);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setLot(lot);
        movement.setMovementType(type);
        movement.setQuantityBase(quantityBase.negate());
        movement.setBalanceAfter(stock.getQuantityBase());
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setUserId(userId);
        movement.setNotes(notes);
        movement = movementRepository.save(movement);

        return new LotAllocation(lot.getId(), lot.getLotCode(), quantityBase, movement.getId());
    }

    /**
     * Entrada dirigida a UN lote especifico que ya existe — el reverso de
     * decreaseStockFromLot. Se usa para anular una venta (o, en el futuro,
     * una devolucion de cliente): el lote a reponer ya se conoce con
     * certeza porque quedo registrado en sale_item_lots al momento de la
     * venta, asi que aqui tampoco se elige por FEFO ni se crea un lote
     * nuevo — solo se repone exactamente el que se consumio.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LotAllocation increaseStockToLot(Long productId, Long lotId, BigDecimal quantityBase, MovementType type,
                                             String referenceType, Long referenceId, Long userId, String notes) {
        requirePositive(quantityBase);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));

        InventoryStock stock = lockOrCreateStock(product);

        InventoryLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new NotFoundException("InventoryLot", lotId));
        if (!lot.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("El lote " + lotId + " no pertenece al producto " + productId);
        }

        lot.setQuantityBase(lot.getQuantityBase().add(quantityBase));
        lotRepository.save(lot);

        stock.setQuantityBase(stock.getQuantityBase().add(quantityBase));
        stock = stockRepository.save(stock);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(product);
        movement.setLot(lot);
        movement.setMovementType(type);
        movement.setQuantityBase(quantityBase);
        movement.setBalanceAfter(stock.getQuantityBase());
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setUserId(userId);
        movement.setNotes(notes);
        movement = movementRepository.save(movement);

        return new LotAllocation(lot.getId(), lot.getLotCode(), quantityBase, movement.getId());
    }

    private InventoryStock lockOrCreateStock(Product product) {
        return stockRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> stockRepository.save(new InventoryStock(product)));
    }

    private void requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidQuantityException(quantity);
        }
    }
}
