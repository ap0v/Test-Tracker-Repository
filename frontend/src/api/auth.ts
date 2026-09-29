import type { LoginCredentials } from '../types/loginCredentials'

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
