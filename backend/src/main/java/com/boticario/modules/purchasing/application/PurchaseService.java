package com.boticario.modules.purchasing.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.audit.CurrentUserProvider;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.catalog.domain.Product;
import com.boticario.modules.catalog.domain.ProductPresentation;
import com.boticario.modules.catalog.domain.UnitConversion;
import com.boticario.modules.catalog.infrastructure.ProductPresentationRepository;
import com.boticario.modules.inventory.application.LotAllocation;
import com.boticario.modules.inventory.application.StockAllocationService;
import com.boticario.modules.inventory.domain.InventoryMovement;
import com.boticario.modules.inventory.domain.MovementType;
import com.boticario.modules.inventory.infrastructure.InventoryMovementRepository;
import com.boticario.modules.purchasing.api.dto.CreatePurchaseRequest;
import com.boticario.modules.purchasing.api.dto.PurchaseDTO;
import com.boticario.modules.purchasing.api.dto.PurchaseItemRequest;
import com.boticario.modules.purchasing.api.dto.PurchaseSummaryDTO;
import com.boticario.modules.purchasing.api.dto.ReturnItemRequest;
import com.boticario.modules.purchasing.domain.Purchase;
import com.boticario.modules.purchasing.domain.PurchaseItem;
import com.boticario.modules.purchasing.domain.PurchaseStatus;
import com.boticario.modules.purchasing.domain.Supplier;
import com.boticario.modules.purchasing.infrastructure.PurchaseRepository;
import com.boticario.modules.purchasing.infrastructure.PurchaseSpecifications;
import com.boticario.modules.purchasing.infrastructure.SupplierRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ciclo de vida de una compra: BORRADOR (editable, sin efecto en
 * inventario) -> RECIBIDA (cada item genera su propio movimiento COMPRA
 * via StockAllocationService, dentro de esta misma transaccion) o
 * ANULADA (solo desde BORRADOR). Una compra ya RECIBIDA no se puede
 * anular ni editar — para revertir stock ya ingresado existe la
 * devolucion a proveedor (returnItem), que queda registrada como
 * DEVOLUCION_COMPRA en el kardex en vez de simplemente desaparecer.
 */
@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductPresentationRepository presentationRepository;
    private final InventoryMovementRepository movementRepository;
    private final StockAllocationService stockAllocationService;
    private final CurrentUserProvider currentUserProvider;

    public PurchaseService(PurchaseRepository purchaseRepository,
                            SupplierRepository supplierRepository,
                            ProductPresentationRepository presentationRepository,
                            InventoryMovementRepository movementRepository,
                            StockAllocationService stockAllocationService,
                            CurrentUserProvider currentUserProvider) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.presentationRepository = presentationRepository;
        this.movementRepository = movementRepository;
        this.stockAllocationService = stockAllocationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseSummaryDTO> search(String search, Long supplierId, PurchaseStatus status, Pageable pageable) {
        Specification<Purchase> spec = Specification.where(PurchaseSpecifications.codeOrInvoiceContains(search))
                .and(PurchaseSpecifications.hasSupplier(supplierId))
                .and(PurchaseSpecifications.hasStatus(status));
        return PageResponse.from(purchaseRepository.findAll(spec, pageable).map(PurchaseSummaryDTO::from));
    }

    @Transactional(readOnly = true)
    public PurchaseDTO findById(Long id) {
        return PurchaseDTO.from(loadPurchase(id));
    }

    @Transactional
    @Audited(action = "PURCHASE_CREATE", entity = "Purchase")
    public PurchaseDTO create(CreatePurchaseRequest request) {
        Supplier supplier = loadActiveSupplier(request.supplierId());

        Purchase purchase = new Purchase();
        purchase.setPurchaseCode(generateCode());
        purchase.setSupplier(supplier);
        applyHeader(purchase, request);
        replaceItems(purchase, request.items());

        return PurchaseDTO.from(purchaseRepository.save(purchase));
    }

    @Transactional
    @Audited(action = "PURCHASE_UPDATE", entity = "Purchase")
    public PurchaseDTO update(Long id, CreatePurchaseRequest request) {
        Purchase purchase = loadPurchase(id);
        requireStatus(purchase, PurchaseStatus.BORRADOR,
                "Solo se pueden editar compras en estado BORRADOR");

        purchase.setSupplier(loadActiveSupplier(request.supplierId()));
        applyHeader(purchase, request);
        replaceItems(purchase, request.items());

        return PurchaseDTO.from(purchaseRepository.save(purchase));
    }

    @Transactional
    @Audited(action = "PURCHASE_CANCEL", entity = "Purchase")
    public PurchaseDTO cancel(Long id) {
        Purchase purchase = loadPurchase(id);
        requireStatus(purchase, PurchaseStatus.BORRADOR,
                "Solo se pueden anular compras en estado BORRADOR; una compra ya recibida se revierte con una devolucion a proveedor");

        purchase.setStatus(PurchaseStatus.ANULADA);
        return PurchaseDTO.from(purchaseRepository.save(purchase));
    }

    /**
     * Recibir una compra es la unica accion de este modulo que toca
     * inventario: por cada item, en la MISMA transaccion, se llama a
     * StockAllocationService.increaseStock (abre su propio lote o suma al
     * existente, deja el movimiento COMPRA en el kardex) y se actualiza el
     * costo de compra vigente de la presentacion.
     */
    @Transactional
    @Audited(action = "PURCHASE_RECEIVE", entity = "Purchase")
    public PurchaseDTO receive(Long id) {
        Purchase purchase = loadPurchase(id);
        requireStatus(purchase, PurchaseStatus.BORRADOR,
                "Esta compra ya fue recibida o anulada");

        Long userId = currentUserProvider.currentUserId().orElse(null);

        for (PurchaseItem item : purchase.getItems()) {
            ProductPresentation presentation = item.getPresentation();
            Product product = presentation.getProduct();
            validateLotAndExpiration(product, item);

            LotAllocation allocation = stockAllocationService.increaseStock(
                    product.getId(), item.getLotCode(), item.getExpirationDate(), item.getQuantityBase(),
                    MovementType.COMPRA, "PURCHASE", purchase.getId(), userId,
                    "Compra " + purchase.getPurchaseCode());

            item.setMovementId(allocation.movementId());

            // Costo de compra vigente de la presentacion = el de la ultima
            // compra recibida (no lleva historial propio: eso es el rol de
            // purchase_items, que ya queda auditado con fecha y proveedor).
            presentation.setPurchasePrice(item.getUnitCost());
            presentationRepository.save(presentation);
        }

        purchase.setStatus(PurchaseStatus.RECIBIDA);
        purchase.setReceivedAt(Instant.now());
        purchase.setReceivedBy(userId);

        return PurchaseDTO.from(purchaseRepository.save(purchase));
    }

    /**
     * Devolucion (parcial o total) de un item ya recibido, hacia el mismo
     * lote que la recepcion origino. No se le pide al usuario elegir el
     * lote: ya se sabe exactamente cual es porque el item quedo ligado a
     * su movimiento de entrada.
     */
    @Transactional
    @Audited(action = "PURCHASE_ITEM_RETURN", entity = "Purchase")
    public PurchaseDTO returnItem(Long purchaseId, Long itemId, ReturnItemRequest request) {
        Purchase purchase = loadPurchase(purchaseId);
        requireStatus(purchase, PurchaseStatus.RECIBIDA,
                "Solo se pueden devolver items de compras ya recibidas");

        PurchaseItem item = purchase.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("PurchaseItem", itemId));

        var available = item.getQuantityBase().subtract(item.getReturnedQuantityBase());
        if (request.quantityBase().compareTo(available) > 0) {
            throw new BusinessRuleException(
                    "No se puede devolver mas de lo disponible para este item (disponible: " + available + ")");
        }

        InventoryMovement originalMovement = movementRepository.findById(item.getMovementId())
                .orElseThrow(() -> new NotFoundException("InventoryMovement", item.getMovementId()));
        Long lotId = originalMovement.getLot() != null ? originalMovement.getLot().getId() : null;
        if (lotId == null) {
            throw new BusinessRuleException(
                    "No se puede determinar el lote de origen de este item para devolverlo");
        }

        Long userId = currentUserProvider.currentUserId().orElse(null);
        Long productId = item.getPresentation().getProduct().getId();

        stockAllocationService.decreaseStockFromLot(productId, lotId, request.quantityBase(),
                MovementType.DEVOLUCION_COMPRA, "PURCHASE_RETURN", purchase.getId(), userId, request.reason());

        item.setReturnedQuantityBase(item.getReturnedQuantityBase().add(request.quantityBase()));

        return PurchaseDTO.from(purchaseRepository.save(purchase));
    }

    private void applyHeader(Purchase purchase, CreatePurchaseRequest request) {
        purchase.setInvoiceNumber(request.invoiceNumber());
        purchase.setPurchaseDate(request.purchaseDate() != null ? request.purchaseDate() : purchase.getPurchaseDate());
        purchase.setNotes(request.notes());
    }

    private void replaceItems(Purchase purchase, List<PurchaseItemRequest> requests) {
        purchase.getItems().clear();

        List<PurchaseItem> items = new ArrayList<>();
        for (PurchaseItemRequest itemRequest : requests) {
            ProductPresentation presentation = presentationRepository.findById(itemRequest.presentationId())
                    .orElseThrow(() -> new NotFoundException("ProductPresentation", itemRequest.presentationId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setPresentation(presentation);
            item.setQuantityPresentation(itemRequest.quantityPresentation());
            item.setQuantityBase(UnitConversion.toBaseUnits(presentation, itemRequest.quantityPresentation()));
            item.setUnitCost(itemRequest.unitCost());
            item.setLotCode(itemRequest.lotCode());
            item.setExpirationDate(itemRequest.expirationDate());
            items.add(item);
        }

        purchase.getItems().addAll(items);
        purchase.recalculateTotal();
    }

    private void validateLotAndExpiration(Product product, PurchaseItem item) {
        if (product.isControlsLot() && (item.getLotCode() == null || item.getLotCode().isBlank())) {
            throw new BusinessRuleException(
                    "El producto \"" + product.getName() + "\" controla lote: indica el codigo de lote antes de recibir la compra");
        }
        if (product.isControlsExpiration() && item.getExpirationDate() == null) {
            throw new BusinessRuleException(
                    "El producto \"" + product.getName() + "\" controla vencimiento: indica la fecha antes de recibir la compra");
        }
    }

    private Supplier loadActiveSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier", supplierId));
        if (!supplier.isActive()) {
            throw new BusinessRuleException("No se puede registrar una compra a un proveedor inactivo");
        }
        return supplier;
    }

    private Purchase loadPurchase(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase", id));
    }

    private void requireStatus(Purchase purchase, PurchaseStatus expected, String errorMessage) {
        if (purchase.getStatus() != expected) {
            throw new BusinessRuleException(errorMessage);
        }
    }

    private String generateCode() {
        long seq = purchaseRepository.nextPurchaseCodeSequence();
        return "COM-" + String.format("%06d", seq);
    }
}
