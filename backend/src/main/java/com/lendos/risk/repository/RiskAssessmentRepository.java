package com.lendos.risk.repository;

import com.lendos.risk.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findByLoan_Id(UUID loanId);
}
