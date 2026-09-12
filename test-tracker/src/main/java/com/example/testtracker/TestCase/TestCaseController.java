package com.example.testtracker.TestCase;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping ("/api/testcases")
public class TestCaseController {
    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping
    public ResponseEntity<TestCase> createTestCase(@Valid @RequestBody CreateTestCaseRequest request) {
        TestCase saved = testCaseService.createTestCase(request);
        return ResponseEntity.created(URI.create("/api/testcases/" + saved.getId())).body(saved);
    }

    @GetMapping
    public PagedModel<TestCase> getAllTestCases(
            @PageableDefault(
                size = 20,
                sort = {"createdAt", "id"},
                direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return new PagedModel<>(testCaseService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCase> getTestCaseById(@PathVariable("id") UUID id) {
        return testCaseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
