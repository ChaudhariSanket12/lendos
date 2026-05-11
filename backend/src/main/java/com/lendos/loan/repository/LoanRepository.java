package com.lendos.loan.repository;

import com.lendos.loan.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findAllByTenant_IdAndDeletedAtIsNullOrderByAppliedAtDescCreatedAtDesc(UUID tenantId);

    List<Loan> findAllByTenant_IdAndStatusAndDeletedAtIsNullOrderByAppliedAtDescCreatedAtDesc(
            UUID tenantId,
            Loan.LoanStatus status
    );

    List<Loan> findAllByTenant_IdAndBorrower_IdAndDeletedAtIsNullOrderByAppliedAtDescCreatedAtDesc(
            UUID tenantId,
            UUID borrowerId
    );

    Optional<Loan> findByIdAndTenant_Id(UUID loanId, UUID tenantId);

    Optional<Loan> findByIdAndTenant_IdAndBorrower_Id(UUID loanId, UUID tenantId, UUID borrowerId);
}
