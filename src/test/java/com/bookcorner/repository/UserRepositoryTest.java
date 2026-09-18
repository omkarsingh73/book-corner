package com.bookcorner.repository;

import com.bookcorner.config.AbstractPostgresRepositoryTest;
import com.bookcorner.entity.member.RoleEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.repository.member.RoleRepository;
import com.bookcorner.repository.member.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRepository Integration Tests (PostgreSQL Testcontainers)")
class UserRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private RoleEntity customerRole;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        customerRole = roleRepository.save(RoleEntity.builder()
                .roleCode("ROLE_CUSTOMER")
                .roleName("Customer")
                .description("Registered Customer Role")
                .build());

        testUser = userRepository.save(UserEntity.builder()
                .email("test.reader@example.com")
                .passwordHash("$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
                .firstName("Alice")
                .lastName("Walker")
                .accountStatus("ACTIVE")
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build());
    }

    @Test
    @DisplayName("Should find user by email with eagerly loaded roles")
    void shouldFindUserByEmail() {
        Optional<UserEntity> found = userRepository.findByEmail("test.reader@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Alice");
        assertThat(found.get().getLastName()).isEqualTo("Walker");
        assertThat(found.get().getRoles()).hasSize(1);
        assertThat(found.get().getRoles().iterator().next().getRoleCode()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should return true when user exists by email")
    void shouldReturnTrueWhenUserExistsByEmail() {
        boolean exists = userRepository.existsByEmail("test.reader@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should paginate users by account status")
    void shouldPaginateUsersByAccountStatus() {
        Page<UserEntity> activeUsers = userRepository.findByAccountStatus("ACTIVE", PageRequest.of(0, 10));

        assertThat(activeUsers).isNotEmpty();
        assertThat(activeUsers.getTotalElements()).isEqualTo(1);
        assertThat(activeUsers.getContent().get(0).getEmail()).isEqualTo("test.reader@example.com");
    }

    @Test
    @DisplayName("Should record successful login and reset failed attempts counter")
    void shouldRecordSuccessfulLogin() {
        Instant now = Instant.now();
        userRepository.recordSuccessfulLogin(testUser.getId(), now);

        Optional<UserEntity> updated = userRepository.findById(testUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getFailedLoginAttempts()).isZero();
        assertThat(updated.get().getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("Should increment failed login attempts counter")
    void shouldIncrementFailedLoginAttempts() {
        userRepository.incrementFailedLoginAttempts(testUser.getId());
        userRepository.incrementFailedLoginAttempts(testUser.getId());

        Optional<UserEntity> updated = userRepository.findById(testUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should enforce soft-delete filter and ignore deleted users in queries")
    void shouldFilterSoftDeletedUsers() {
        testUser.setIsDeleted(true);
        testUser.setDeletedAt(Instant.now());
        userRepository.save(testUser);

        Optional<UserEntity> found = userRepository.findByEmail("test.reader@example.com");
        assertThat(found).isEmpty();
        assertThat(userRepository.existsByEmail("test.reader@example.com")).isFalse();
    }
}
