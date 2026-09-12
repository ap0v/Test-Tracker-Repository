package com.example.testtracker.TestCase;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public record CreateTestCaseRequest(
    @NotBlank(message = "Title is required")
    String title,
    String description,
    String preconditions,
    String expectedResults,
    TestCasePriority priority,
    TestCaseStatus status,
    List<@NotBlank String> tags
) {
}
