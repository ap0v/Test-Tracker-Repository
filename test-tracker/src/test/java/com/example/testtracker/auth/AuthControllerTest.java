package com.example.testtracker.auth;

import com.example.testtracker.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AccountUserDetailsService userDetailsService;

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
}
