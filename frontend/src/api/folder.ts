import type { Folder, FolderRequest, UpdateFolderRequest } from '../types/folder';
import { csrfHeader } from './csrf';

function folderPath(id: string): string {
    return `/api/folders/${encodeURIComponent(id)}`;
}

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
    const response = await fetch('/api/folders', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...await csrfHeader()
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
    const response = await fetch(folderPath(id), {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            ...await csrfHeader()
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

// Also deletes its subfolders; the test cases inside are kept
export async function deleteFolder(id: string): Promise<void> {
    const response = await fetch(folderPath(id), {
        method: 'DELETE',
        headers: await csrfHeader()
    });

    if (!response.ok) {
        switch (response.status) {
            case 401:
                throw new Error('Failed to delete folder (401 Unauthorized). Sign in to delete folders.');
            case 404:
                throw new Error('This folder no longer exists.');
            default:
                throw new Error(`Failed to delete folder (${response.status})`);
        }
    }
}

// Adding a test case that is already in the folder succeeds without changing anything
export async function addTestCaseToFolder(folderId: string, testCaseId: string): Promise<void> {
    const response = await fetch(`${folderPath(folderId)}/testcases/${encodeURIComponent(testCaseId)}`, {
        method: 'PUT',
        headers: await csrfHeader()
    });

    if (!response.ok) {
        switch (response.status) {
            case 401:
                throw new Error('Failed to add test case to folder (401 Unauthorized). Sign in to organize test cases.');
            case 404:
                throw new Error('This folder or test case no longer exists.');
            default:
                throw new Error(`Failed to add test case to folder (${response.status})`);
        }
    }
}

// Only takes the test case out of the folder; the test case itself is kept
export async function removeTestCaseFromFolder(folderId: string, testCaseId: string): Promise<void> {
    const response = await fetch(`${folderPath(folderId)}/testcases/${encodeURIComponent(testCaseId)}`, {
        method: 'DELETE',
        headers: await csrfHeader()
    });

    if (!response.ok) {
        switch (response.status) {
            case 401:
                throw new Error('Failed to remove test case from folder (401 Unauthorized). Sign in to organize test cases.');
            case 404:
                throw new Error('This folder no longer exists.');
            default:
                throw new Error(`Failed to remove test case from folder (${response.status})`);
        }
    }
}
