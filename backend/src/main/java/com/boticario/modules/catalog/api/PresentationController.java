package com.boticario.modules.catalog.api;

import com.boticario.modules.catalog.api.dto.*;
import com.boticario.modules.catalog.application.PresentationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presentations")
@PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
public class PresentationController {

    private final PresentationService presentationService;

    public PresentationController(PresentationService presentationService) {
        this.presentationService = presentationService;
    }

    @PatchMapping("/{id}/price")
    public PresentationDTO updatePrice(@PathVariable Long id, @Valid @RequestBody UpdatePriceRequest request) {
        return presentationService.updatePrice(id, request);
    }

    @GetMapping("/{id}/price-history")
    public List<PriceHistoryDTO> priceHistory(@PathVariable Long id) {
        return presentationService.getPriceHistory(id);
    }

    @PostMapping("/{id}/barcodes")
    @ResponseStatus(HttpStatus.CREATED)
    public BarcodeDTO addBarcode(@PathVariable Long id, @RequestBody BarcodeRequest request) {
        return presentationService.addBarcode(id, request.code(), request.primary());
    }

    @DeleteMapping("/barcodes/{barcodeId}")
    public void deactivateBarcode(@PathVariable Long barcodeId) {
        presentationService.deactivateBarcode(barcodeId);
    }

    public record BarcodeRequest(@NotBlank String code, boolean primary) {
    }
}
