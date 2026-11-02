package com.example.testtracker.Folder;

import com.example.testtracker.AccountLogin.AccountLogin;
import com.example.testtracker.TestCase.TestCaseRepository;
import com.example.testtracker.TestCase.TestCaseService;
import com.example.testtracker.auth.AccountLoginPrincipal;
import com.example.testtracker.auth.AccountUserDetailsService;
import com.example.testtracker.config.SecurityConfig;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@Import({SecurityConfig.class, FolderService.class, TestCaseService.class})
class FolderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    @MockitoBean
    private AccountUserDetailsService userDetailsService;

    @MockitoBean
    private FolderRepository repository;

    @MockitoBean
    private FolderTestCaseRepository folderTestCaseRepository;

    @MockitoBean
    private TestCaseRepository testCaseRepository;

    private final UUID accountId = UUID.randomUUID();
    private final UUID loginId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();
    private final UUID testCaseId = UUID.randomUUID();

    private AccountLoginPrincipal principal() {
        var login = new AccountLogin(accountId, "tester@example.com", "encoded-password");
        ReflectionTestUtils.setField(login, "id", loginId);
        return new AccountLoginPrincipal(login);
    }

    private Folder folder(UUID id, UUID ownerAccountId, UUID parentId, String name) {
        Folder folder = new Folder();
        ReflectionTestUtils.setField(folder, "id", id);
        folder.setAccountId(ownerAccountId);
        folder.setParentId(parentId);
        folder.setName(name);
        return folder;
    }

    private void saveValidFolder() {
        when(repository.saveAndFlush(any(Folder.class))).thenAnswer(invocation -> {
            Folder saved = invocation.getArgument(0);
            var violations = validator.validate(saved);
            assertTrue(violations.isEmpty(), () -> "Invalid entity: " + violations);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", folderId);
            }
            return saved;
        });
    }

    @Test
    void listingOnlyReturnsSignedInAccountsFolders() throws Exception {
        Folder ownFolder = folder(folderId, accountId, null, "Smoke tests");
        Folder otherAccountsFolder = folder(UUID.randomUUID(), UUID.randomUUID(), null, "Other");
        when(repository.findAll()).thenReturn(List.of(ownFolder, otherAccountsFolder));
        when(repository.findByAccountId(eq(accountId), any(Sort.class))).thenReturn(List.of(ownFolder));

        mockMvc.perform(get("/api/folders").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(folderId.toString()));
    }

    @Test
    void listingStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void fetchingOwnFolderByIdSucceeds() throws Exception {
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Smoke tests")));

        mockMvc.perform(get("/api/folders/" + folderId).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folderId.toString()));
    }

    @Test
    void fetchingAnotherAccountsFolderByIdReturnsNotFound() throws Exception {
        // The folder exists, but it belongs to a different account
        when(repository.findById(folderId))
                .thenReturn(Optional.of(folder(folderId, UUID.randomUUID(), null, "Smoke tests")));

        mockMvc.perform(get("/api/folders/" + folderId).with(user(principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void creationUsesSignedInAccountAndCreatorInsteadOfClientSuppliedIds() throws Exception {
        saveValidFolder();

        mockMvc.perform(post("/api/folders")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Smoke tests  ",
                                  "accountId": "%s",
                                  "createdByLoginId": "%s"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/folders/" + folderId))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.createdByLoginId").value(loginId.toString()))
                .andExpect(jsonPath("$.name").value("Smoke tests"))
                .andExpect(jsonPath("$.parentId").isEmpty());
    }

    @Test
    void creationUnderOwnFolderSucceeds() throws Exception {
        UUID parentId = UUID.randomUUID();
        when(repository.findByIdAndAccountId(parentId, accountId))
                .thenReturn(Optional.of(folder(parentId, accountId, null, "Parent")));
        saveValidFolder();

        mockMvc.perform(post("/api/folders")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Child\", \"parentId\": \"%s\"}".formatted(parentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(parentId.toString()));
    }

    @Test
    void creationUnderAnotherAccountsFolderReturnsNotFound() throws Exception {
        // The parent exists, but it belongs to a different account
        UUID parentId = UUID.randomUUID();
        when(repository.findById(parentId))
                .thenReturn(Optional.of(folder(parentId, UUID.randomUUID(), null, "Parent")));

        mockMvc.perform(post("/api/folders")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Child\", \"parentId\": \"%s\"}".formatted(parentId)))
                .andExpect(status().isNotFound());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void creationWithDuplicateSiblingNameReturnsConflict() throws Exception {
        when(repository.saveAndFlush(any(Folder.class)))
                .thenThrow(new DataIntegrityViolationException("folders_sibling_name_unique"));

        mockMvc.perform(post("/api/folders")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Smoke tests\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void creationRejectsBlankNameBeforeSaving() throws Exception {
        mockMvc.perform(post("/api/folders")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void creationStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/folders")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Smoke tests\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void creationStillRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Smoke tests\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void renamingKeepsOriginalAccountAndCreatorInsteadOfClientSuppliedIds() throws Exception {
        // Another login in the same account created the folder
        UUID originalCreatorId = UUID.randomUUID();
        Folder existing = folder(folderId, accountId, null, "Old name");
        existing.setCreatedByLoginId(originalCreatorId);
        when(repository.findByIdAndAccountId(folderId, accountId)).thenReturn(Optional.of(existing));
        saveValidFolder();

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  New name  ",
                                  "accountId": "%s",
                                  "createdByLoginId": "%s"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.createdByLoginId").value(originalCreatorId.toString()));
    }

    @Test
    void renamingToDuplicateSiblingNameReturnsConflict() throws Exception {
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Old name")));
        when(repository.saveAndFlush(any(Folder.class)))
                .thenThrow(new DataIntegrityViolationException("folders_sibling_name_unique"));

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Taken name\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void movingFolderUnderAnotherOwnFolderSucceeds() throws Exception {
        UUID newParentId = UUID.randomUUID();
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Folder")));
        when(repository.findByIdAndAccountId(newParentId, accountId))
                .thenReturn(Optional.of(folder(newParentId, accountId, null, "New parent")));
        saveValidFolder();

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Folder\", \"parentId\": \"%s\"}".formatted(newParentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(newParentId.toString()));
    }

    @Test
    void movingFolderToTopLevelSucceeds() throws Exception {
        UUID oldParentId = UUID.randomUUID();
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, oldParentId, "Folder")));
        saveValidFolder();

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Folder\", \"parentId\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").isEmpty());
    }

    @Test
    void movingFolderUnderAnotherAccountsFolderReturnsNotFound() throws Exception {
        // The new parent exists, but it belongs to a different account
        UUID newParentId = UUID.randomUUID();
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Folder")));
        when(repository.findById(newParentId))
                .thenReturn(Optional.of(folder(newParentId, UUID.randomUUID(), null, "New parent")));

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Folder\", \"parentId\": \"%s\"}".formatted(newParentId)))
                .andExpect(status().isNotFound());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void movingFolderIntoItselfIsRejected() throws Exception {
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Folder")));

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Folder\", \"parentId\": \"%s\"}".formatted(folderId)))
                .andExpect(status().isBadRequest());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void movingFolderIntoItsOwnSubfolderIsRejected() throws Exception {
        // folder -> child -> grandchild, then try to move folder under grandchild
        UUID childId = UUID.randomUUID();
        UUID grandchildId = UUID.randomUUID();
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Folder")));
        when(repository.findByIdAndAccountId(childId, accountId))
                .thenReturn(Optional.of(folder(childId, accountId, folderId, "Child")));
        when(repository.findByIdAndAccountId(grandchildId, accountId))
                .thenReturn(Optional.of(folder(grandchildId, accountId, childId, "Grandchild")));

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Folder\", \"parentId\": \"%s\"}".formatted(grandchildId)))
                .andExpect(status().isBadRequest());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updatingAnotherAccountsFolderReturnsNotFound() throws Exception {
        // The folder exists, but it belongs to a different account
        when(repository.findById(folderId))
                .thenReturn(Optional.of(folder(folderId, UUID.randomUUID(), null, "Folder")));

        mockMvc.perform(put("/api/folders/" + folderId)
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New name\"}"))
                .andExpect(status().isNotFound());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deletingOwnFolderSucceeds() throws Exception {
        Folder folder = folder(folderId, accountId, null, "Smoke tests");
        when(repository.findByIdAndAccountId(folderId, accountId)).thenReturn(Optional.of(folder));

        mockMvc.perform(delete("/api/folders/" + folderId).with(user(principal())).with(csrf()))
                .andExpect(status().isNoContent());
        verify(repository).delete(folder);
    }

    @Test
    void deletingAnotherAccountsFolderReturnsNotFound() throws Exception {
        // The folder exists, but it belongs to a different account
        when(repository.findById(folderId))
                .thenReturn(Optional.of(folder(folderId, UUID.randomUUID(), null, "Smoke tests")));

        mockMvc.perform(delete("/api/folders/" + folderId).with(user(principal())).with(csrf()))
                .andExpect(status().isNotFound());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletionStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(delete("/api/folders/" + folderId).with(user(principal())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    private String testCaseLinkPath() {
        return "/api/folders/" + folderId + "/testcases/" + testCaseId;
    }

    private void ownFolderAndTestCaseExist() {
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Smoke tests")));
        when(testCaseRepository.existsByIdAndAccountId(testCaseId, accountId)).thenReturn(true);
    }

    @Test
    void addingOwnTestCaseToOwnFolderSavesLinkForSignedInAccount() throws Exception {
        ownFolderAndTestCaseExist();

        mockMvc.perform(put(testCaseLinkPath()).with(user(principal())).with(csrf()))
                .andExpect(status().isNoContent());

        ArgumentCaptor<FolderTestCase> saved = ArgumentCaptor.forClass(FolderTestCase.class);
        verify(folderTestCaseRepository).save(saved.capture());
        assertEquals(new FolderTestCaseId(folderId, testCaseId), saved.getValue().getId());
        assertEquals(accountId, saved.getValue().getAccountId());
    }

    @Test
    void addingTestCaseAlreadyInFolderDoesNotSaveAgain() throws Exception {
        ownFolderAndTestCaseExist();
        when(folderTestCaseRepository.existsById(new FolderTestCaseId(folderId, testCaseId))).thenReturn(true);

        mockMvc.perform(put(testCaseLinkPath()).with(user(principal())).with(csrf()))
                .andExpect(status().isNoContent());
        verify(folderTestCaseRepository, never()).save(any());
    }

    @Test
    void addingAnotherAccountsTestCaseReturnsNotFound() throws Exception {
        // The test case exists, but it belongs to a different account
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Smoke tests")));
        when(testCaseRepository.existsById(testCaseId)).thenReturn(true);

        mockMvc.perform(put(testCaseLinkPath()).with(user(principal())).with(csrf()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(folderTestCaseRepository);
    }

    @Test
    void addingTestCaseToAnotherAccountsFolderReturnsNotFound() throws Exception {
        // The folder exists, but it belongs to a different account
        when(repository.findById(folderId))
                .thenReturn(Optional.of(folder(folderId, UUID.randomUUID(), null, "Smoke tests")));
        when(testCaseRepository.existsByIdAndAccountId(testCaseId, accountId)).thenReturn(true);

        mockMvc.perform(put(testCaseLinkPath()).with(user(principal())).with(csrf()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(folderTestCaseRepository);
    }

    @Test
    void addingTestCaseStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(put(testCaseLinkPath()).with(user(principal())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(folderTestCaseRepository);
    }

    @Test
    void removingTestCaseFromOwnFolderDeletesOnlyTheLink() throws Exception {
        when(repository.findByIdAndAccountId(folderId, accountId))
                .thenReturn(Optional.of(folder(folderId, accountId, null, "Smoke tests")));

        mockMvc.perform(delete(testCaseLinkPath()).with(user(principal())).with(csrf()))
                .andExpect(status().isNoContent());
        verify(folderTestCaseRepository).deleteById(new FolderTestCaseId(folderId, testCaseId));
        verifyNoInteractions(testCaseRepository);
    }

    @Test
    void removingTestCaseFromAnotherAccountsFolderReturnsNotFound() throws Exception {
        // The folder exists, but it belongs to a different account
        when(repository.findById(folderId))
                .thenReturn(Optional.of(folder(folderId, UUID.randomUUID(), null, "Smoke tests")));

        mockMvc.perform(delete(testCaseLinkPath()).with(user(principal())).with(csrf()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(folderTestCaseRepository);
    }
}
