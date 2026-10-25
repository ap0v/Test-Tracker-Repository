import type { LoginCredentials } from '../types/loginCredentials'

export async function checkSession(signal?: AbortSignal): Promise<boolean> {
    const response = await fetch('/api/auth/me', {
        credentials: 'same-origin',
        cache: 'no-store',
        signal,
    });

    if (response.status === 401) {
        return false;
    }
    if (!response.ok) {
        throw new Error('Unable to check your session. Please try again.');
    }

    return true;
}

export async function login({ email, password }: LoginCredentials): Promise<void> {
    const csrfResponse = await fetch('/api/auth/csrf');
    if (!csrfResponse.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    const csrfData = await csrfResponse.json();
    const csrfHeaderName = csrfData.headerName;
    const csrfToken = csrfData.token;

    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeaderName]: csrfToken,
        },
        body: new URLSearchParams({ email, password }).toString(),
    });

    if (!response.ok) {
        throw new Error('Login failed');
    }
}

export async function logout(): Promise<void> {
    const csrfResponse = await fetch('/api/auth/csrf');
    if (!csrfResponse.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    const csrfData = await csrfResponse.json();
    const csrfHeaderName = csrfData.headerName;
    const csrfToken = csrfData.token;

    const response = await fetch('/api/auth/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeaderName]: csrfToken,
        },
    });

    if (!response.ok) {
        throw new Error('Logout failed');
    }
}

interface RegistrationCredentials extends LoginCredentials {
    accountName: string;
}

export async function register({
    accountName,
    email,
    password,
}: RegistrationCredentials): Promise<void> {
    const csrfResponse = await fetch('/api/auth/csrf');
    if (!csrfResponse.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    const { headerName, token } = await csrfResponse.json();

    const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [headerName]: token,
        },
        body: JSON.stringify({
            accountName: accountName.trim(),
            email: email.trim(),
            password,
        }),
    });

    if (response.status === 409) {
        throw new Error('Email already in use');
    }
    if (response.status === 400) {
        throw new Error('Check your account name, email, and password');
    }
    if (!response.ok) {
        throw new Error('Registration failed');
    }
}
