package com.example.testtracker.Folder;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

public record FolderRequest(
    @NotBlank(message = "Name is required")
    String name,
    UUID parentId
) {
}
