package com.example.testtracker.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Account name is required")
    String accountName,

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    String email,

    @Size(min = 8, message = "Password must be at least 8 characters long")
    @NotBlank(message = "Password is required")
    String password
) {} 
