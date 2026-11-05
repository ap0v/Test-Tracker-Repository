import type { Folder, FolderRequest, UpdateFolderRequest } from '../types/folder';

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

export async function updateFolder(id: string, request: UpdateFolderRequest): Promise<Folder> {
    const csrfResponse = await fetch('/api/auth/csrf');

    if (!csrfResponse.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    const { headerName, token } = await csrfResponse.json()

    const response = await fetch(`/api/folders/${encodeURIComponent(id)}`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            [headerName]: token
        },
        body: JSON.stringify(request)
    });

    if (!response.ok) {
        switch (response.status) {
            case 400:
                // A blank name is rejected before the move is checked, so any other 400 is an invalid move
                throw new Error(request.name?.trim() === ''
                    ? 'Enter a folder name.'
                    : 'A folder cannot be moved into itself or one of its subfolders.');
            case 401:
                throw new Error('Failed to update folder (401 Unauthorized). Sign in to change folders.');
            case 404:
                throw new Error(request.parentId
                    ? 'This folder or the folder you are moving it to no longer exists.'
                    : 'This folder no longer exists.');
            case 409:
                throw new Error('A folder with this name already exists here.');
            default:
                throw new Error(`Failed to update folder (${response.status})`);
        }
    }

    return response.json();
}
