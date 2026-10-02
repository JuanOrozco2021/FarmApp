package com.boticario.modules.catalog.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.BusinessRuleException;
import com.boticario.common.exception.DuplicateResourceException;
import com.boticario.common.exception.NotFoundException;
import com.boticario.modules.catalog.api.dto.*;
import com.boticario.modules.catalog.domain.*;
import com.boticario.modules.catalog.infrastructure.BarcodeRepository;
import com.boticario.modules.catalog.infrastructure.PriceHistoryRepository;
import com.boticario.modules.catalog.infrastructure.ProductPresentationRepository;
import com.boticario.modules.catalog.infrastructure.UnitRepository;
import com.boticario.common.audit.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Toda alta/edicion de presentacion, codigo de barras y precio pasa por aqui
 * — es el unico lugar que escribe en product_presentations, barcodes y
 * price_history, para que las reglas (factor > 0, codigo unico, historial
 * de precio) no se dupliquen en otros servicios.
 */
@Service
public class PresentationService {

    private final ProductPresentationRepository presentationRepository;
    private final BarcodeRepository barcodeRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final UnitRepository unitRepository;
    private final CurrentUserProvider currentUserProvider;

    public PresentationService(ProductPresentationRepository presentationRepository,
                                BarcodeRepository barcodeRepository,
                                PriceHistoryRepository priceHistoryRepository,
                                UnitRepository unitRepository,
                                CurrentUserProvider currentUserProvider) {
        this.presentationRepository = presentationRepository;
        this.barcodeRepository = barcodeRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.unitRepository = unitRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ProductPresentation createPresentation(Product product, PresentationRequest request, boolean isBase) {
        Unit unit = unitRepository.findByCode(request.unitCode())
                .orElseThrow(() -> new NotFoundException("Unit", request.unitCode()));

        if (isBase && request.conversionFactor().compareTo(BigDecimal.ONE) != 0) {
            throw new BusinessRuleException("La presentacion base debe tener factor de conversion igual a 1");
        }

        ProductPresentation presentation = new ProductPresentation();
        presentation.setProduct(product);
        presentation.setUnit(unit);
        presentation.setConversionFactor(request.conversionFactor());
        presentation.setSalePrice(request.salePrice());
        presentation.setPurchasePrice(request.purchasePrice());
        presentation.setBasePresentation(isBase);
        presentation.setActive(true);

        presentation = presentationRepository.save(presentation);

        if (request.barcode() != null && !request.barcode().isBlank()) {
            attachBarcode(presentation, request.barcode(), true);
        }

        return presentation;
    }

    @Transactional
    @Audited(action = "PRESENTATION_CREATE", entity = "ProductPresentation")
    public PresentationDTO addPresentation(Product product, PresentationRequest request) {
        ProductPresentation presentation = createPresentation(product, request, false);
        return PresentationDTO.from(presentation);
    }

    @Transactional
    public void attachBarcode(ProductPresentation presentation, String code, boolean primary) {
        if (barcodeRepository.existsByCodeAndActiveTrue(code)) {
            throw new DuplicateResourceException("Ya existe una presentacion activa con el codigo de barras " + code);
        }

        Barcode barcode = new Barcode();
        barcode.setPresentation(presentation);
        barcode.setCode(code);
        barcode.setPrimary(primary);
        barcode.setActive(true);
        barcodeRepository.save(barcode);
    }

    @Transactional
    @Audited(action = "BARCODE_CREATE", entity = "ProductBarcode")
    public BarcodeDTO addBarcode(Long presentationId, String code, boolean primary) {
        ProductPresentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new NotFoundException("ProductPresentation", presentationId));
        attachBarcode(presentation, code, primary);
        return presentation.getBarcodes().stream()
                .filter(b -> b.getCode().equals(code))
                .findFirst()
                .map(BarcodeDTO::from)
                .orElseThrow();
    }

    @Transactional
    @Audited(action = "BARCODE_DEACTIVATE", entity = "ProductBarcode")
    public void deactivateBarcode(Long barcodeId) {
        Barcode barcode = barcodeRepository.findById(barcodeId)
                .orElseThrow(() -> new NotFoundException("Barcode", barcodeId));
        barcode.setActive(false);
        barcodeRepository.save(barcode);
    }

    @Transactional
    @Audited(action = "PRESENTATION_PRICE_UPDATE", entity = "ProductPresentation")
    public PresentationDTO updatePrice(Long presentationId, UpdatePriceRequest request) {
        ProductPresentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new NotFoundException("ProductPresentation", presentationId));

        BigDecimal oldPrice = presentation.getSalePrice();
        BigDecimal newPrice = request.newPrice();

        if (oldPrice.compareTo(newPrice) != 0) {
            PriceHistory history = new PriceHistory();
            history.setPresentation(presentation);
            history.setOldPrice(oldPrice);
            history.setNewPrice(newPrice);
            history.setChangedBy(currentUserProvider.currentUsername());
            history.setReason(request.reason());
            priceHistoryRepository.save(history);

            presentation.setSalePrice(newPrice);
            presentationRepository.save(presentation);
        }

        return PresentationDTO.from(presentation);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryDTO> getPriceHistory(Long presentationId) {
        return priceHistoryRepository.findByPresentationIdOrderByChangedAtDesc(presentationId).stream()
                .map(PriceHistoryDTO::from)
                .toList();
    }

    /** Resuelve un codigo escaneado a producto + presentacion + factor + precio (regla 27 del encargo). */
    @Transactional(readOnly = true)
    public BarcodeResolutionDTO resolveBarcode(String code) {
        Barcode barcode = barcodeRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new NotFoundException("Barcode", code));

        ProductPresentation presentation = barcode.getPresentation();
        Product product = presentation.getProduct();

        return new BarcodeResolutionDTO(
                code,
                product.getId(),
                product.getName(),
                presentation.getId(),
                presentation.getUnit().getCode(),
                presentation.getConversionFactor(),
                presentation.getSalePrice(),
                product.isRequiresPrescription(),
                product.isFractionable()
        );
    }

}
