package com.example.testtracker.Folder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import com.example.testtracker.auth.AccountLoginPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/folders")
public class FolderController {
    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public List<Folder> getAllFolders(@AuthenticationPrincipal AccountLoginPrincipal principal) {
        return folderService.findAll(principal.getAccountId());
    }

    @GetMapping("/{id}")
    public Folder getFolderById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal AccountLoginPrincipal principal) {
        return folderService.findById(id, principal.getAccountId());
    }

    @PostMapping
    public ResponseEntity<Folder> createFolder(
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal AccountLoginPrincipal principal) {
        Folder saved = folderService.createFolder(request, principal.getAccountId(), principal.getLoginId());
        return ResponseEntity.created(URI.create("/api/folders/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Folder updateFolder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal AccountLoginPrincipal principal) {
        return folderService.updateFolder(id, request, principal.getAccountId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal AccountLoginPrincipal principal) {
        folderService.deleteFolder(id, principal.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{folderId}/testcases/{testCaseId}")
    public ResponseEntity<Void> addTestCase(
            @PathVariable("folderId") UUID folderId,
            @PathVariable("testCaseId") UUID testCaseId,
            @AuthenticationPrincipal AccountLoginPrincipal principal) {
        folderService.addTestCase(folderId, testCaseId, principal.getAccountId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{folderId}/testcases/{testCaseId}")
    public ResponseEntity<Void> removeTestCase(
            @PathVariable("folderId") UUID folderId,
            @PathVariable("testCaseId") UUID testCaseId,
            @AuthenticationPrincipal AccountLoginPrincipal principal) {
        folderService.removeTestCase(folderId, testCaseId, principal.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
