package com.example.testtracker.Folder;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

/**
 * 
 * FolderTestCase
 */
@Entity
@Table(name = "folder_test_cases")
public class FolderTestCase {
    @EmbeddedId
    private FolderTestCaseId id;

    @NotNull
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected FolderTestCase() {
    }

    public FolderTestCase(UUID folderId, UUID testCaseId, UUID accountId) {
        this.id = new FolderTestCaseId(folderId, testCaseId);
        this.accountId = accountId;
    }

    public FolderTestCaseId getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
