package com.boticario.modules.injections.infrastructure;

import com.boticario.modules.injections.domain.InjectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface InjectionRecordRepository extends JpaRepository<InjectionRecord, Long>, JpaSpecificationExecutor<InjectionRecord> {

    @Query(value = "SELECT nextval('injection_code_seq')", nativeQuery = true)
    Long nextInjectionCodeSequence();
}
