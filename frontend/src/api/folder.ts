import type { Folder, FolderRequest } from '../types/folder';

export async function getFolders(): Promise<Folder[]> {
    const response = await fetch('/api/folders');

    if (!response.ok) {
        switch (response.status) {
            case 401:
                throw new Error('Failed to fetch folders (401 Unauthorized). Sign in to view folders.');
            default:
                throw new Error(`Failed to fetch folders (${response.status})`);
        }
    }

    return response.json();
}

export async function createFolder(request: FolderRequest): Promise<Folder> {
    const csrfResponse = await fetch('/api/auth/csrf');
    
    if (!csrfResponse.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    const { headerName, token } = await csrfResponse.json()

    const response = await fetch('/api/folders', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [headerName]: token
        },
        body: JSON.stringify(request)
    });

    if (!response.ok) {
        switch (response.status) {
            case 400:
                throw new Error('Enter a folder name.');
            case 401:
                throw new Error('Failed to create folder (401 Unauthorized). Sign in to add a folder.');
            case 404:
                throw new Error('The parent folder no longer exists.');
            case 409:
                throw new Error('A folder with this name already exists here.');
            default:
                throw new Error(`Failed to create folder (${response.status})`);
        }
    }

    return response.json();
}