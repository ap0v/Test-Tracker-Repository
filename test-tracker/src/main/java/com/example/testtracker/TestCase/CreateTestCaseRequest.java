package com.example.testtracker.TestCase;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTestCaseRequest(
    @NotBlank(message = "Title is required")
    String title,
    String description,
    String preconditions,
    String expectedResults,
    TestCasePriority priority,
    TestCaseStatus status,
    List<@NotBlank String> tags,
    // Folders to file the new test case in; leave out or send an empty list for none
    List<@NotNull UUID> folderIds
) {
}
