package com.example.testtracker.auth;

public record CsrfResponse(
        String headerName,
        String parameterName,
        String token
) {}
