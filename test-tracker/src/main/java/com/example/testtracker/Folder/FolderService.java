package com.example.testtracker.Folder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.testtracker.TestCase.TestCaseService;

@Service
@Transactional(readOnly = true)
public class FolderService {
    private final FolderRepository repository;
    private final FolderTestCaseRepository folderTestCaseRepository;
    private final TestCaseService testCaseService;

    public FolderService(
            FolderRepository repository,
            FolderTestCaseRepository folderTestCaseRepository,
            TestCaseService testCaseService) {
        this.repository = repository;
        this.folderTestCaseRepository = folderTestCaseRepository;
        this.testCaseService = testCaseService;
    }

    public List<Folder> findAll(UUID accountId) {
        return repository.findByAccountId(accountId, Sort.by("name", "id"));
    }

    public Folder findById(UUID id, UUID accountId) {
        return repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
    }

    @Transactional
    public Folder createFolder(FolderRequest request, UUID accountId, UUID createdByLoginId) {
        requireParent(request.parentId(), accountId);

        Folder folder = new Folder();
        folder.setAccountId(accountId);
        folder.setCreatedByLoginId(createdByLoginId);
        folder.setParentId(request.parentId());
        folder.setName(request.name().trim());

        return save(folder);
    }

    @Transactional
    public Folder updateFolder(UUID id, FolderRequest request, UUID accountId) {
        Folder folder = findById(id, accountId);

        if (!Objects.equals(folder.getParentId(), request.parentId())) {
            requireParent(request.parentId(), accountId);
            requireNotOwnDescendant(id, request.parentId(), accountId);
        }

        folder.setName(request.name().trim());
        folder.setParentId(request.parentId());

        return save(folder);
    }

    // The database cascade removes subfolders and test case links; the test cases themselves are kept
    @Transactional
    public void deleteFolder(UUID id, UUID accountId) {
        repository.delete(findById(id, accountId));
    }

    // Adding a test case that is already in the folder does nothing, so the request is safe to repeat
    @Transactional
    public void addTestCase(UUID folderId, UUID testCaseId, UUID accountId) {
        findById(folderId, accountId);
        if (!testCaseService.existsForAccount(testCaseId, accountId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case not found");
        }

        if (!folderTestCaseRepository.existsById(new FolderTestCaseId(folderId, testCaseId))) {
            folderTestCaseRepository.save(new FolderTestCase(folderId, testCaseId, accountId));
        }
    }

    // Only the link is removed; the test case itself is kept
    @Transactional
    public void removeTestCase(UUID folderId, UUID testCaseId, UUID accountId) {
        findById(folderId, accountId);
        folderTestCaseRepository.deleteById(new FolderTestCaseId(folderId, testCaseId));
    }

    private void requireParent(UUID parentId, UUID accountId) {
        if (parentId != null && repository.findByIdAndAccountId(parentId, accountId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found");
        }
    }

    // The database only stops a folder being its own parent, so walk up from the new parent to catch longer cycles
    private void requireNotOwnDescendant(UUID folderId, UUID newParentId, UUID accountId) {
        UUID ancestorId = newParentId;
        while (ancestorId != null) {
            if (ancestorId.equals(folderId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "A folder cannot be moved into itself or one of its subfolders");
            }
            ancestorId = repository.findByIdAndAccountId(ancestorId, accountId)
                    .map(Folder::getParentId)
                    .orElse(null);
        }
    }

    // Flush now so a duplicate sibling name fails here as a 409 instead of at commit as a 500
    private Folder save(Folder folder) {
        try {
            return repository.saveAndFlush(folder);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder with this name already exists here", e);
        }
    }
}
