package com.example.testtracker.TestCase;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Service
@Transactional(readOnly = true)
public class TestCaseService {
    private final TestCaseRepository repository;

    public TestCaseService(TestCaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TestCase createTestCase(@NotNull @Valid CreateTestCaseRequest request) {
        TestCase testCase = new TestCase();

        testCase.setTitle(request.title());
        testCase.setDescription(request.description());
        testCase.setPreconditions(request.preconditions());
        testCase.setExpectedResults(request.expectedResults());

        if (request.priority() != null) {
            testCase.setPriority(request.priority());
        }

        if (request.status() != null) {
            testCase.setStatus(request.status());
        }

        testCase.setTags(
            request.tags() == null ? new String[0] : request.tags().toArray(String[]::new)
        );

        return repository.save(testCase);
    }

    public Page<TestCase> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<TestCase> findById(UUID id) {
        return repository.findById(id);
    }
}
