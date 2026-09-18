package com.bookcorner.repository.member;

import com.bookcorner.entity.member.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for RoleEntity.
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    /**
     * Find role by code (e.g. ROLE_CUSTOMER, ROLE_STORE_ADMIN).
     */
    Optional<RoleEntity> findByRoleCode(String roleCode);
}
