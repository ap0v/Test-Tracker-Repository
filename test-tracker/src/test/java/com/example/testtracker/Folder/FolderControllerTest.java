package com.example.testtracker.Folder;

import com.example.testtracker.AccountLogin.AccountLogin;
import com.example.testtracker.auth.AccountLoginPrincipal;
import com.example.testtracker.auth.AccountUserDetailsService;
import com.example.testtracker.config.SecurityConfig;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
@Import({SecurityConfig.class, FolderService.class})
class FolderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    @MockitoBean
    private AccountUserDetailsService userDetailsService;

    @MockitoBean
    private FolderRepository repository;

    private final UUID accountId = UUID.randomUUID();
    private final UUID loginId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();

    private AccountLoginPrincipal principal() {
        var login = new AccountLogin(accountId, "tester@example.com", "encoded-password");
        ReflectionTestUtils.setField(login, "id", loginId);
        return new AccountLoginPrincipal(login);
    }

    private void saveValidFolder() {
        when(repository.saveAndFlush(any(Folder.class))).thenAnswer(invocation -> {
            Folder saved = invocation.getArgument(0);
            var violations = validator.validate(saved);
            assertTrue(violations.isEmpty(), () -> "Invalid entity: " + violations);
            ReflectionTestUtils.setField(saved, "id", folderId);
            return saved;
        });
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
}
