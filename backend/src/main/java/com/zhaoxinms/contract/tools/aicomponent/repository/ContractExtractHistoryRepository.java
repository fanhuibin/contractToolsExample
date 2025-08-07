package com.zhaoxinms.contract.tools.aicomponent.repository;

import com.zhaoxinms.contract.tools.aicomponent.model.ContractExtractHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractExtractHistoryRepository extends JpaRepository<ContractExtractHistory, Long> {

    List<ContractExtractHistory> findByUserIdOrderByExtractTimeDesc(String userId);

}

