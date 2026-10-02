package com.boticario.modules.purchasing.infrastructure;

import com.boticario.modules.purchasing.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @Query(value = "SELECT nextval('purchase_code_seq')", nativeQuery = true)
    Long nextPurchaseCodeSequence();
}
