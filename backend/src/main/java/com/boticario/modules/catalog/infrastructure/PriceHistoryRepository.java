package com.boticario.modules.catalog.infrastructure;

import com.boticario.modules.catalog.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByPresentationIdOrderByChangedAtDesc(Long presentationId);
}
