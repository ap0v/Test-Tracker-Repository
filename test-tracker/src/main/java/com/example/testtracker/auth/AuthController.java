package com.example.testtracker.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController 
@RequestMapping ("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal AccountLoginPrincipal principal) {
        return new CurrentUserResponse(principal.getLoginId(), principal.getAccountId(), principal.getUsername());
    }

    @GetMapping("/csrf")
    public CsrfResponse getCsrfToken(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }
}
