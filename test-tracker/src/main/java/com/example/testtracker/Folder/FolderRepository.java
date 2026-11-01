package com.example.testtracker.Folder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * FolderRepository
 */
public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByAccountId(UUID accountId, Sort sort);

    Optional<Folder> findByIdAndAccountId(UUID id, UUID accountId);
}
