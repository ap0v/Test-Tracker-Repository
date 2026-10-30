package com.example.testtracker.Folder;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * FolderRepository
 */
public interface FolderRepository extends JpaRepository<Folder, UUID> {
    Page<Folder> findByAccountId(UUID accountId, Pageable pageable);

    Optional<Folder> findByIdAndAccountId(UUID id, UUID accountId);
}
