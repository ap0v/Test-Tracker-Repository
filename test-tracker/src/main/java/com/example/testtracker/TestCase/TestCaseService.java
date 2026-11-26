package com.example.testtracker.TestCase;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.example.testtracker.Folder.FolderRepository;
import com.example.testtracker.Folder.FolderTestCase;
import com.example.testtracker.Folder.FolderTestCaseRepository;

@Service
@Transactional(readOnly = true)
public class TestCaseService {
    private final TestCaseRepository repository;
    private final FolderRepository folderRepository;
    private final FolderTestCaseRepository folderTestCaseRepository;

    public TestCaseService(
            TestCaseRepository repository,
            FolderRepository folderRepository,
            FolderTestCaseRepository folderTestCaseRepository) {
        this.repository = repository;
        this.folderRepository = folderRepository;
        this.folderTestCaseRepository = folderTestCaseRepository;
    }

    // The test case and its folder links are saved together, so a bad folder id leaves nothing behind
    @Transactional
    public TestCase createTestCase(
            @NotNull @Valid CreateTestCaseRequest request,
            UUID accountId,
            UUID createdByLoginId) {
        // A set, so listing the same folder twice still files the test case there only once
        Set<UUID> folderIds = request.folderIds() == null ? Set.of() : new LinkedHashSet<>(request.folderIds());
        if (!folderIds.isEmpty() && folderRepository.countByAccountIdAndIdIn(accountId, folderIds) != folderIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found");
        }

        TestCase testCase = new TestCase();

        testCase.setAccountId(accountId);
        testCase.setCreatedByLoginId(createdByLoginId);
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

        TestCase saved = repository.save(testCase);
        folderTestCaseRepository.saveAll(folderIds.stream()
                .map(folderId -> new FolderTestCase(folderId, saved.getId(), accountId))
                .toList());
        return saved;
    }

    @Transactional
    public void deleteTestCase(UUID id, UUID accountId) {
        TestCase testCase = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case not found"));
        repository.delete(testCase);
    }

    public Page<TestCase> findAll(UUID accountId, UUID folderId, Pageable pageable) {
        if (folderId == null) {
            return repository.findByAccountId(accountId, pageable);
        }
        return repository.findByAccountIdInFolder(accountId, folderId, pageable);
    }

    public Optional<TestCase> findById(UUID id, UUID accountId) {
        return repository.findByIdAndAccountId(id, accountId);
    }

    public boolean existsForAccount(UUID id, UUID accountId) {
        return repository.existsByIdAndAccountId(id, accountId);
    }
}
