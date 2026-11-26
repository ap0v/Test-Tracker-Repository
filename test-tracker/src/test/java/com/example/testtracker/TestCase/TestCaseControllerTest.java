package com.example.testtracker.TestCase;

import com.example.testtracker.AccountLogin.AccountLogin;
import com.example.testtracker.Folder.FolderRepository;
import com.example.testtracker.Folder.FolderTestCase;
import com.example.testtracker.Folder.FolderTestCaseId;
import com.example.testtracker.Folder.FolderTestCaseRepository;
import com.example.testtracker.auth.AccountLoginPrincipal;
import com.example.testtracker.auth.AccountUserDetailsService;
import com.example.testtracker.config.SecurityConfig;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestCaseController.class)
@Import({SecurityConfig.class, TestCaseService.class})
class TestCaseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    @MockitoBean
    private AccountUserDetailsService userDetailsService;

    @MockitoBean
    private TestCaseRepository repository;

    @MockitoBean
    private FolderRepository folderRepository;

    @MockitoBean
    private FolderTestCaseRepository folderTestCaseRepository;

    private final UUID accountId = UUID.randomUUID();
    private final UUID loginId = UUID.randomUUID();
    private final UUID testCaseId = UUID.randomUUID();

    private AccountLoginPrincipal principal() {
        var login = new AccountLogin(accountId, "tester@example.com", "encoded-password");
        ReflectionTestUtils.setField(login, "id", loginId);
        return new AccountLoginPrincipal(login);
    }

    private void saveValidTestCase() {
        when(repository.save(any(TestCase.class))).thenAnswer(invocation -> {
            TestCase saved = invocation.getArgument(0);
            var violations = validator.validate(saved);
            assertTrue(violations.isEmpty(), () -> "Invalid entity: " + violations);
            ReflectionTestUtils.setField(saved, "id", testCaseId);
            return saved;
        });
    }

    @Test
    void creationUsesSignedInAccountAndCreatorInsteadOfClientSuppliedIds() throws Exception {
        saveValidTestCase();

        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Create a test case",
                                  "priority": "HIGH",
                                  "status": "READY",
                                  "tags": ["smoke", "login"],
                                  "accountId": "%s",
                                  "createdByLoginId": "%s"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/testcases/" + testCaseId))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.createdByLoginId").value(loginId.toString()))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.tags[0]").value("smoke"))
                .andExpect(jsonPath("$.tags[1]").value("login"));
    }

    @Test
    void creationAllowsOmittedTagsAndUsesDefaults() throws Exception {
        saveValidTestCase();

        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Minimal test case\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tags").isEmpty());
    }

    @Test
    void creationRejectsBlankTagBeforeSaving() throws Exception {
        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test case\", \"tags\": [\"test\", \"\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void creationFilesTestCaseInEachRequestedFolderOnce() throws Exception {
        saveValidTestCase();
        UUID firstFolderId = UUID.randomUUID();
        UUID secondFolderId = UUID.randomUUID();
        when(folderRepository.countByAccountIdAndIdIn(accountId, Set.of(firstFolderId, secondFolderId)))
                .thenReturn(2L);

        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Filed test case",
                                  "folderIds": ["%s", "%s", "%s"]
                                }
                                """.formatted(firstFolderId, secondFolderId, firstFolderId)))
                .andExpect(status().isCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FolderTestCase>> links = ArgumentCaptor.forClass(List.class);
        verify(folderTestCaseRepository).saveAll(links.capture());
        List<FolderTestCase> savedLinks = links.getValue();
        assertEquals(2, savedLinks.size());
        assertEquals(new FolderTestCaseId(firstFolderId, testCaseId), savedLinks.get(0).getId());
        assertEquals(new FolderTestCaseId(secondFolderId, testCaseId), savedLinks.get(1).getId());
        assertTrue(savedLinks.stream().allMatch(link -> accountId.equals(link.getAccountId())));
    }

    @Test
    void creationWithAnotherAccountsFolderReturnsNotFoundAndSavesNothing() throws Exception {
        UUID ownFolderId = UUID.randomUUID();
        UUID otherAccountsFolderId = UUID.randomUUID();
        // Only one of the two folders belongs to the signed-in account
        when(folderRepository.countByAccountIdAndIdIn(accountId, Set.of(ownFolderId, otherAccountsFolderId)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Filed test case",
                                  "folderIds": ["%s", "%s"]
                                }
                                """.formatted(ownFolderId, otherAccountsFolderId)))
                .andExpect(status().isNotFound());
        verify(repository, never()).save(any());
        verifyNoInteractions(folderTestCaseRepository);
    }

    @Test
    void creationRejectsNullFolderIdBeforeSaving() throws Exception {
        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test case\", \"folderIds\": [null]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, folderRepository, folderTestCaseRepository);
    }

    @Test
    void creationStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/testcases")
                        .with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test case\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void creationStillRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/testcases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test case\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    private TestCase testCase(UUID id, UUID ownerAccountId) {
        TestCase testCase = new TestCase();
        ReflectionTestUtils.setField(testCase, "id", id);
        testCase.setAccountId(ownerAccountId);
        testCase.setTitle("Test case");
        return testCase;
    }

    @Test
    void listingOnlyReturnsSignedInAccountsTestCases() throws Exception {
        TestCase ownTestCase = testCase(testCaseId, accountId);
        TestCase otherAccountsTestCase = testCase(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownTestCase, otherAccountsTestCase)));
        when(repository.findByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownTestCase)));

        mockMvc.perform(get("/api/testcases").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(testCaseId.toString()));
    }

    @Test
    void listingWithFolderIdOnlyReturnsTestCasesInThatFolder() throws Exception {
        UUID folderId = UUID.randomUUID();
        TestCase inFolder = testCase(testCaseId, accountId);
        TestCase notInFolder = testCase(UUID.randomUUID(), accountId);
        when(repository.findByAccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inFolder, notInFolder)));
        when(repository.findByAccountIdInFolder(eq(accountId), eq(folderId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inFolder)));

        mockMvc.perform(get("/api/testcases")
                        .param("folderId", folderId.toString())
                        .with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(testCaseId.toString()));
    }

    @Test
    void listingWithInvalidFolderIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/testcases")
                        .param("folderId", "not-a-uuid")
                        .with(user(principal())))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void fetchingOwnTestCaseByIdSucceeds() throws Exception {
        when(repository.findByIdAndAccountId(testCaseId, accountId))
                .thenReturn(Optional.of(testCase(testCaseId, accountId)));

        mockMvc.perform(get("/api/testcases/" + testCaseId).with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCaseId.toString()));
    }

    @Test
    void fetchingAnotherAccountsTestCaseByIdReturnsNotFound() throws Exception {
        // The test case exists, but it belongs to a different account
        when(repository.findById(testCaseId))
                .thenReturn(Optional.of(testCase(testCaseId, UUID.randomUUID())));

        mockMvc.perform(get("/api/testcases/" + testCaseId).with(user(principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/testcases"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void deletingOwnTestCaseSucceeds() throws Exception {
        TestCase testCase = testCase(testCaseId, accountId);
        when(repository.findByIdAndAccountId(testCaseId, accountId)).thenReturn(Optional.of(testCase));

        mockMvc.perform(delete("/api/testcases/" + testCaseId).with(user(principal())).with(csrf()))
                .andExpect(status().isNoContent());
        verify(repository).delete(testCase);
    }

    @Test
    void deletingAnotherAccountsTestCaseReturnsNotFound() throws Exception {
        // The test case exists, but it belongs to a different account
        when(repository.findById(testCaseId))
                .thenReturn(Optional.of(testCase(testCaseId, UUID.randomUUID())));

        mockMvc.perform(delete("/api/testcases/" + testCaseId).with(user(principal())).with(csrf()))
                .andExpect(status().isNotFound());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletionStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(delete("/api/testcases/" + testCaseId).with(user(principal())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(repository);
    }

    @Test
    void deletionStillRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/testcases/" + testCaseId).with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }
}
