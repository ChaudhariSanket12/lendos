package com.lendos.identity.repository;

import com.lendos.identity.entity.Tenant;
import com.lendos.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenant(String email, Tenant tenant);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByTenant(Tenant tenant);

    boolean existsByEmailAndTenant(String email, Tenant tenant);

    boolean existsByTenant_IdAndEmailIgnoreCase(UUID tenantId, String email);
}
