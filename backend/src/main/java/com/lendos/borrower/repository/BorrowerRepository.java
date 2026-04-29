package com.lendos.borrower.repository;

import com.lendos.borrower.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {

    List<Borrower> findAllByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    List<Borrower> findAllByTenant_IdAndStatusOrderByCreatedAtDesc(
            UUID tenantId,
            Borrower.BorrowerStatus status
    );

    Optional<Borrower> findByIdAndTenant_Id(UUID borrowerId, UUID tenantId);

    Optional<Borrower> findByTenant_IdAndUser_Id(UUID tenantId, UUID userId);

    boolean existsByTenant_IdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenant_IdAndPhone(UUID tenantId, String phone);
}
