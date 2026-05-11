package com.lendos.document.repository;

import com.lendos.borrower.entity.Borrower;
import com.lendos.document.entity.BorrowerDocument;
import com.lendos.document.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowerDocumentRepository extends JpaRepository<BorrowerDocument, UUID> {

    Optional<BorrowerDocument> findByBorrowerAndDocumentType(Borrower borrower, DocumentType documentType);

    List<BorrowerDocument> findByBorrower(Borrower borrower);

    List<BorrowerDocument> findByBorrowerOrderByCreatedAtDesc(Borrower borrower);

    void deleteByBorrowerAndDocumentType(Borrower borrower, DocumentType documentType);
}
