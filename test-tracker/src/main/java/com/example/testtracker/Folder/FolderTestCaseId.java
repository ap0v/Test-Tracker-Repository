package com.example.testtracker.Folder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record FolderTestCaseId(
    @Column(name = "folder_id", nullable = false) UUID folderId,
    @Column(name = "test_case_id", nullable = false) UUID testCaseId
) implements Serializable {
}
