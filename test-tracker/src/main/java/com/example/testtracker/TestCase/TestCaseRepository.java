package com.example.testtracker.TestCase;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * TestCaseRepository
 */
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    Page<TestCase> findByAccountId(UUID accountId, Pageable pageable);

    @Query("""
            select t from TestCase t
            where t.accountId = :accountId
              and exists (
                  select f from FolderTestCase f
                  where f.id.folderId = :folderId and f.id.testCaseId = t.id
              )
            """)
    Page<TestCase> findByAccountIdInFolder(
            @Param("accountId") UUID accountId,
            @Param("folderId") UUID folderId,
            Pageable pageable);

    Optional<TestCase> findByIdAndAccountId(UUID id, UUID accountId);

    boolean existsByIdAndAccountId(UUID id, UUID accountId);
}
