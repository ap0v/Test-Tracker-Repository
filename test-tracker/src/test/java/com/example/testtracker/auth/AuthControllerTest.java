package com.example.testtracker.auth;

import com.example.testtracker.Account.Account;
import com.example.testtracker.Account.AccountRepository;
import com.example.testtracker.AccountLogin.AccountLogin;
import com.example.testtracker.AccountLogin.AccountLoginRepository;
import com.example.testtracker.config.SecurityConfig;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RegistrationService.class})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AccountUserDetailsService userDetailsService;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private AccountLoginRepository accountLoginRepository;

    @Test
    void anonymousClientCanFetchTokenAndUseItToLogin() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        var csrf = JsonMapper.builder().build()
                .readValue(result.getResponse().getContentAsString(), CsrfResponse.class);
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);

        when(userDetailsService.loadUserByUsername("tester@example.com"))
                .thenReturn(User.withUsername("tester@example.com")
                        .password(passwordEncoder.encode("test-password"))
                        .authorities("ROLE_USER")
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .header(csrf.headerName(), csrf.token())
                        .param("email", "tester@example.com")
                        .param("password", "test-password"))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("email", "tester@example.com")
                        .param("password", "test-password"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationRequests")
    void invalidRegistrationRequestIsRejected(RegisterRequest request) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountRepository, accountLoginRepository);
    }

    static Stream<RegisterRequest> invalidRegistrationRequests() {
        return Stream.of(
                new RegisterRequest(" ", "tester@example.com", "test-password"),
                new RegisterRequest("Test Account", "", "test-password"),
                new RegisterRequest("Test Account", "invalid-email", "test-password"),
                new RegisterRequest("Test Account", "tester@example.com", ""),
                new RegisterRequest("Test Account", "tester@example.com", null),
                new RegisterRequest("Test Account", "tester@example.com", "short"));
    }

    @ParameterizedTest
    @MethodSource("validRegistrationPasswords")
    void anonymousRegistrationWithValidInputCreatesAccount(String password) throws Exception {
        UUID accountId = UUID.randomUUID();
        Account savedAccount = mock(Account.class);
        when(savedAccount.getId()).thenReturn(accountId);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        var result = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        var token = JsonMapper.builder().build()
                .readValue(result.getResponse().getContentAsString(), CsrfResponse.class);
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);

        var request = new RegisterRequest(" Test Account ", "Tester@Example.COM", password);
        mockMvc.perform(post("/api/auth/register")
                        .session(session)
                        .header(token.headerName(), token.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isCreated());
        verify(accountLoginRepository).findByNormalizedEmail("tester@example.com");

        var accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals("Test Account", accountCaptor.getValue().getName());

        var loginCaptor = ArgumentCaptor.forClass(AccountLogin.class);
        verify(accountLoginRepository).save(loginCaptor.capture());
        AccountLogin login = loginCaptor.getValue();
        assertEquals(accountId, login.getAccountId());
        assertEquals("tester@example.com", login.getEmail());
        assertTrue(passwordEncoder.matches(password, login.getPasswordHash()));
    }

    static Stream<String> validRegistrationPasswords() {
        return Stream.of(" test-password ", "a".repeat(72), "a".repeat(68) + "😀");
    }

    @Test
    void duplicateEmailReturnsConflictWithoutSavingAccount() throws Exception {
        when(accountLoginRepository.findByNormalizedEmail("tester@example.com"))
                .thenReturn(Optional.of(mock(AccountLogin.class)));

        var request = new RegisterRequest("Test Account", "Tester@Example.COM", "test-password");
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isConflict());

        verifyNoInteractions(accountRepository);
        verify(accountLoginRepository, never()).save(any(AccountLogin.class));
    }

    @ParameterizedTest
    @MethodSource("overlongRegistrationPasswords")
    void passwordExceeding72Utf8BytesIsRejectedWithoutSaving(String password) throws Exception {
        var request = new RegisterRequest("Test Account", "tester@example.com", password);
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountRepository);
        verify(accountLoginRepository, never()).save(any(AccountLogin.class));
    }

    static Stream<String> overlongRegistrationPasswords() {
        return Stream.of("a".repeat(73), "a".repeat(69) + "😀");
    }

    @Test
    void registrationWithoutCsrfTokenIsForbidden() throws Exception {
        var request = new RegisterRequest("Test Account", "tester@example.com", "test-password");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accountRepository, accountLoginRepository);
    }
}
