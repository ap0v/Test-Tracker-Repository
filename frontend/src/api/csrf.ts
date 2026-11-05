// The server rejects any request that changes data unless it carries this header
export async function csrfHeader(): Promise<Record<string, string>> {
    const response = await fetch('/api/auth/csrf');

    if (!response.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    const { headerName, token } = await response.json();
    return { [headerName]: token };
}
