package com.example.testtracker.TestCase;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * TestCaseRepository
 */
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    Page<TestCase> findByAccountId(UUID accountId, Pageable pageable);

    Optional<TestCase> findByIdAndAccountId(UUID id, UUID accountId);
}
