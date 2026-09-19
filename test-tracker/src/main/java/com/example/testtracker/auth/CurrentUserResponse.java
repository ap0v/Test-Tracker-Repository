package com.example.testtracker.auth;

import java.util.UUID;

public record CurrentUserResponse(
        UUID loginId,
        UUID accountId,
        String email
) {}