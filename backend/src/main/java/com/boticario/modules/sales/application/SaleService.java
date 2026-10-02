package com.boticario.modules.sales.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.audit.CurrentUserProvider;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.customers.domain.Customer;
import com.boticario.modules.customers.infrastructure.CustomerRepository;
import com.boticario.modules.catalog.domain.ProductPresentation;
import com.boticario.modules.catalog.domain.ProductStatus;
import com.boticario.modules.catalog.domain.UnitConversion;
import com.boticario.modules.catalog.infrastructure.ProductPresentationRepository;
import com.boticario.modules.inventory.application.LotAllocation;
import com.boticario.modules.inventory.application.StockAllocationService;
import com.boticario.modules.inventory.domain.MovementType;
import com.boticario.modules.inventory.infrastructure.InventoryLotRepository;
import com.boticario.modules.sales.api.dto.CancelSaleRequest;
import com.boticario.modules.sales.api.dto.CreateSaleRequest;
import com.boticario.modules.sales.api.dto.SaleDTO;
import com.boticario.modules.sales.api.dto.SaleItemRequest;
import com.boticario.modules.sales.api.dto.SalePaymentRequest;
import com.boticario.modules.sales.api.dto.SaleSummaryDTO;
import com.boticario.modules.sales.domain.PaymentMethod;
import com.boticario.modules.sales.domain.Sale;
import com.boticario.modules.sales.domain.SaleItem;
import com.boticario.modules.sales.domain.SaleItemLot;
import com.boticario.modules.sales.domain.SalePayment;
import com.boticario.modules.sales.domain.SaleStatus;
import com.boticario.modules.sales.infrastructure.SaleRepository;
import com.boticario.modules.sales.infrastructure.SaleSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Registro de ventas en el POS. A diferencia de compras, aqui no hay
 * BORRADOR: la venta se cobra y descarga inventario en la misma
 * transaccion. El descuento de stock siempre es automatico por FEFO
 * (decreaseStockFefo) — el cajero nunca elige el lote — y cada lote
 * realmente consumido queda en sale_item_lots para poder revertir la
 * venta con precision si se anula (increaseStockToLot, un movimiento
 * DEVOLUCION_VENTA por lote, nunca se reescribe el original).
 */
@Service
public class SaleService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SaleRepository saleRepository;
    private final ProductPresentationRepository presentationRepository;
    private final InventoryLotRepository lotRepository;
    private final StockAllocationService stockAllocationService;
    private final CurrentUserProvider currentUserProvider;
    private final CustomerRepository customerRepository;

    public SaleService(SaleRepository saleRepository,
                        ProductPresentationRepository presentationRepository,
                        InventoryLotRepository lotRepository,
                        StockAllocationService stockAllocationService,
                        CurrentUserProvider currentUserProvider,
                        CustomerRepository customerRepository) {
        this.saleRepository = saleRepository;
        this.presentationRepository = presentationRepository;
        this.lotRepository = lotRepository;
        this.stockAllocationService = stockAllocationService;
        this.currentUserProvider = currentUserProvider;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryDTO> search(String search, SaleStatus status, Long customerId, Pageable pageable) {
        Specification<Sale> spec = Specification.where(SaleSpecifications.codeContains(search))
                .and(SaleSpecifications.hasStatus(status))
                .and(SaleSpecifications.belongsToCustomer(customerId));
        return PageResponse.from(saleRepository.findAll(spec, pageable).map(SaleSummaryDTO::from));
    }

    @Transactional(readOnly = true)
    public SaleDTO findById(Long id) {
        return SaleDTO.from(loadSale(id));
    }

    @Transactional
    @Audited(action = "SALE_REGISTER", entity = "Sale")
    public SaleDTO create(CreateSaleRequest request) {
        Long userId = currentUserProvider.currentUserId().orElse(null);
        boolean canApplyDiscount = currentUserProvider.hasAuthority("SALE_APPLY_DISCOUNT");

        Sale sale = new Sale();
        sale.setSaleCode(generateCode());
        applyCustomer(sale, request);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            ProductPresentation presentation = presentationRepository.findById(itemRequest.presentationId())
                    .orElseThrow(() -> new NotFoundException("ProductPresentation", itemRequest.presentationId()));
            if (!presentation.isActive()) {
                throw new BusinessRuleException("La presentacion seleccionada ya no esta activa");
            }
            if (presentation.getProduct().getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessRuleException(
                        "El producto \"" + presentation.getProduct().getName() + "\" esta inactivo y no se puede vender");
            }

            BigDecimal discountPercentage = itemRequest.discountPercentage() != null
                    ? itemRequest.discountPercentage() : BigDecimal.ZERO;
            if (discountPercentage.signum() > 0 && !canApplyDiscount) {
                throw new BusinessRuleException("No tienes permiso para aplicar descuentos en una venta");
            }

            BigDecimal grossLine = itemRequest.quantityPresentation().multiply(presentation.getSalePrice());
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    discountPercentage.divide(HUNDRED, 6, RoundingMode.HALF_UP));
            BigDecimal lineTotal = grossLine.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = grossLine.setScale(2, RoundingMode.HALF_UP).subtract(lineTotal);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setPresentation(presentation);
            item.setQuantityPresentation(itemRequest.quantityPresentation());
            item.setQuantityBase(UnitConversion.toBaseUnits(presentation, itemRequest.quantityPresentation()));
            item.setUnitPrice(presentation.getSalePrice());
            item.setDiscountPercentage(discountPercentage);
            item.setLineTotal(lineTotal);
            item.setRequiresPrescription(presentation.getProduct().isRequiresPrescription());
            sale.getItems().add(item);

            subtotal = subtotal.add(grossLine);
            discountTotal = discountTotal.add(discountAmount);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountTotal).setScale(2, RoundingMode.HALF_UP);

        sale.setSubtotal(subtotal);
        sale.setDiscountTotal(discountTotal);
        sale.setTotal(total);

        applyPayments(sale, request.payments(), total);

        sale = saleRepository.save(sale);

        for (SaleItem item : sale.getItems()) {
            Long productId = item.getPresentation().getProduct().getId();
            List<LotAllocation> allocations = stockAllocationService.decreaseStockFefo(
                    productId, item.getQuantityBase(), MovementType.VENTA, "SALE", sale.getId(), userId,
                    "Venta " + sale.getSaleCode());

            for (LotAllocation allocation : allocations) {
                SaleItemLot saleItemLot = new SaleItemLot();
                saleItemLot.setSaleItem(item);
                saleItemLot.setLot(lotRepository.getReferenceById(allocation.lotId()));
                saleItemLot.setQuantityBase(allocation.quantityBase());
                saleItemLot.setMovementId(allocation.movementId());
                item.getLots().add(saleItemLot);
            }
        }

        return SaleDTO.from(sale);
    }

    @Transactional
    @Audited(action = "SALE_CANCEL", entity = "Sale")
    public SaleDTO cancel(Long id, CancelSaleRequest request) {
        Sale sale = loadSale(id);
        if (sale.getStatus() != SaleStatus.ACTIVA) {
            throw new BusinessRuleException("Esta venta ya fue anulada");
        }

        Long userId = currentUserProvider.currentUserId().orElse(null);

        for (SaleItem item : sale.getItems()) {
            Long productId = item.getPresentation().getProduct().getId();
            for (SaleItemLot saleItemLot : item.getLots()) {
                stockAllocationService.increaseStockToLot(
                        productId, saleItemLot.getLot().getId(), saleItemLot.getQuantityBase(),
                        MovementType.DEVOLUCION_VENTA, "SALE_CANCEL", sale.getId(), userId,
                        "Anulacion de venta " + sale.getSaleCode());
            }
        }

        sale.setStatus(SaleStatus.ANULADA);
        sale.setCancelledAt(Instant.now());
        sale.setCancelledReason(request.reason());
        sale.setCancelledBy(userId);

        return SaleDTO.from(saleRepository.save(sale));
    }

    /**
     * Solo el efectivo puede generar vuelto: los pagos electronicos nunca
     * pueden superar el total (no tendria sentido "dar cambio" en tarjeta).
     * Con eso garantizado, lo que sobre del efectivo despues de cubrir lo
     * que falta es exactamente el vuelto.
     */
    private void applyPayments(Sale sale, List<SalePaymentRequest> paymentRequests, BigDecimal total) {
        BigDecimal nonCash = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;

        for (SalePaymentRequest paymentRequest : paymentRequests) {
            SalePayment payment = new SalePayment();
            payment.setSale(sale);
            payment.setPaymentMethod(paymentRequest.paymentMethod());
            payment.setAmount(paymentRequest.amount());
            sale.getPayments().add(payment);

            if (paymentRequest.paymentMethod() == PaymentMethod.EFECTIVO) {
                cash = cash.add(paymentRequest.amount());
            } else {
                nonCash = nonCash.add(paymentRequest.amount());
            }
        }

        if (nonCash.compareTo(total) > 0) {
            throw new BusinessRuleException("Los pagos con tarjeta o transferencia no pueden superar el total de la venta");
        }

        BigDecimal remaining = total.subtract(nonCash);
        if (cash.compareTo(remaining) < 0) {
            throw new BusinessRuleException("El pago no cubre el total de la venta");
        }

        sale.setChangeAmount(cash.subtract(remaining));
    }

    /**
     * Si viene customerId, tiene prioridad: se valida que el cliente exista
     * y este activo, y su nombre se copia (snapshot) a customerName — igual
     * criterio de inmutabilidad que productName en SaleItem. Sin
     * customerId, se mantiene el comportamiento de la Fase 6 (texto libre).
     */
    private void applyCustomer(Sale sale, CreateSaleRequest request) {
        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer", request.customerId()));
            if (!customer.isActive()) {
                throw new BusinessRuleException("El cliente seleccionado esta inactivo");
            }
            sale.setCustomer(customer);
            sale.setCustomerName(customer.getName());
        } else {
            sale.setCustomerName(blankToNull(request.customerName()));
        }
    }

    private Sale loadSale(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sale", id));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String generateCode() {
        long seq = saleRepository.nextSaleCodeSequence();
        return "VTA-" + String.format("%06d", seq);
    }
}
