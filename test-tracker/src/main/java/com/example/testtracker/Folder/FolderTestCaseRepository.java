package com.example.testtracker.Folder;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * FolderTestCaseRepository
 */
public interface FolderTestCaseRepository extends JpaRepository<FolderTestCase, FolderTestCaseId> {
    List<FolderTestCase> findByIdTestCaseIdAndAccountId(UUID testCaseId, UUID accountId);
}
