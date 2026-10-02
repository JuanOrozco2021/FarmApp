package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.Barcode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarcodeRepository extends JpaRepository<Barcode, Long> {
    Optional<Barcode> findByCodeAndActiveTrue(String code);
    boolean existsByCodeAndActiveTrue(String code);
}
