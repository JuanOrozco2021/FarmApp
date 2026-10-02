package com.boticario.modules.catalog.application;

import com.boticario.common.audit.Audited;
import com.boticario.common.exception.NotFoundException;
import com.boticario.common.pagination.PageResponse;
import com.boticario.modules.catalog.api.dto.*;
import com.boticario.modules.catalog.domain.*;
import com.boticario.modules.catalog.infrastructure.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPresentationRepository presentationRepository;
    private final ActiveIngredientRepository activeIngredientRepository;
    private final ProductCategoryRepository categoryRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final UnitRepository unitRepository;
    private final PresentationService presentationService;

    public ProductService(ProductRepository productRepository,
                           ProductPresentationRepository presentationRepository,
                           ActiveIngredientRepository activeIngredientRepository,
                           ProductCategoryRepository categoryRepository,
                           LaboratoryRepository laboratoryRepository,
                           UnitRepository unitRepository,
                           PresentationService presentationService) {
        this.productRepository = productRepository;
        this.presentationRepository = presentationRepository;
        this.activeIngredientRepository = activeIngredientRepository;
        this.categoryRepository = categoryRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.unitRepository = unitRepository;
        this.presentationService = presentationService;
    }

    @Transactional
    @Audited(action = "PRODUCT_CREATE", entity = "Product")
    public ProductDTO create(CreateProductRequest request) {
        Unit baseUnit = unitRepository.findByCode(request.baseUnitCode())
                .orElseThrow(() -> new NotFoundException("Unit", request.baseUnitCode()));

        Product product = new Product();
        product.setInternalCode(generateInternalCode());
        product.setName(request.name());
        product.setConcentration(request.concentration());
        product.setPharmaceuticalForm(request.pharmaceuticalForm());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setBaseUnit(baseUnit);
        product.setMinStockBase(request.minStockBase() != null ? request.minStockBase() : BigDecimal.ZERO);
        product.setMaxStockBase(request.maxStockBase());
        product.setRequiresPrescription(request.requiresPrescription());
        product.setFractionable(request.fractionable());
        product.setControlsLot(request.controlsLot());
        product.setControlsExpiration(request.controlsExpiration());
        product.setStatus(ProductStatus.ACTIVE);

        if (request.activeIngredientId() != null) {
            product.setActiveIngredient(activeIngredientRepository.findById(request.activeIngredientId())
                    .orElseThrow(() -> new NotFoundException("ActiveIngredient", request.activeIngredientId())));
        }
        if (request.categoryId() != null) {
            product.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("ProductCategory", request.categoryId())));
        }
        if (request.laboratoryId() != null) {
            product.setLaboratory(laboratoryRepository.findById(request.laboratoryId())
                    .orElseThrow(() -> new NotFoundException("Laboratory", request.laboratoryId())));
        }

        product = productRepository.save(product);

        // Todo producto nace con su presentacion base (factor 1 en la unidad base) —
        // regla del encargo: siempre debe existir una forma vendible por defecto.
        presentationService.createPresentation(product, request.basePresentation(), true);

        return findById(product.getId());
    }

    @Transactional
    @Audited(action = "PRODUCT_UPDATE", entity = "Product")
    public ProductDTO update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));

        product.setName(request.name());
        product.setConcentration(request.concentration());
        product.setPharmaceuticalForm(request.pharmaceuticalForm());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setMinStockBase(request.minStockBase() != null ? request.minStockBase() : product.getMinStockBase());
        product.setMaxStockBase(request.maxStockBase());
        product.setRequiresPrescription(request.requiresPrescription());
        product.setFractionable(request.fractionable());
        product.setControlsLot(request.controlsLot());
        product.setControlsExpiration(request.controlsExpiration());

        if (request.activeIngredientId() != null) {
            product.setActiveIngredient(activeIngredientRepository.findById(request.activeIngredientId())
                    .orElseThrow(() -> new NotFoundException("ActiveIngredient", request.activeIngredientId())));
        }
        if (request.categoryId() != null) {
            product.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("ProductCategory", request.categoryId())));
        }
        if (request.laboratoryId() != null) {
            product.setLaboratory(laboratoryRepository.findById(request.laboratoryId())
                    .orElseThrow(() -> new NotFoundException("Laboratory", request.laboratoryId())));
        }

        productRepository.save(product);
        return findById(id);
    }

    @Transactional
    @Audited(action = "PRODUCT_STATUS_CHANGE", entity = "Product")
    public ProductDTO setStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));
        // Nunca se borra fisicamente un producto (regla 16 del encargo): solo cambia de estado.
        product.setStatus(status);
        productRepository.save(product);
        return findById(id);
    }

    @Transactional
    public PresentationDTO addPresentation(Long productId, PresentationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product", productId));
        return presentationService.addPresentation(product, request);
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product", id));

        List<PresentationDTO> presentations = presentationRepository.findByProductIdAndActiveTrue(id).stream()
                .map(PresentationDTO::from)
                .toList();

        return ProductDTO.from(product, presentations);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDTO> search(String search, Long categoryId, Long laboratoryId,
                                                    Long activeIngredientId, ProductStatus status, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.nameContains(search))
                .and(ProductSpecifications.hasCategory(categoryId))
                .and(ProductSpecifications.hasLaboratory(laboratoryId))
                .and(ProductSpecifications.hasActiveIngredient(activeIngredientId))
                .and(ProductSpecifications.hasStatus(status));

        var page = productRepository.findAll(spec, pageable).map(product -> {
            BigDecimal basePrice = presentationRepository.findByProductIdAndActiveTrue(product.getId()).stream()
                    .filter(ProductPresentation::isBasePresentation)
                    .map(ProductPresentation::getSalePrice)
                    .findFirst()
                    .orElse(null);
            return ProductSummaryDTO.from(product, basePrice);
        });

        return PageResponse.from(page);
    }

    private String generateInternalCode() {
        long seq = productRepository.nextInternalCodeSequence();
        return "PRD-" + String.format("%06d", seq);
    }
}
