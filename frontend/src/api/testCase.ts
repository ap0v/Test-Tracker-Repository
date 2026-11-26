import type { CreateTestCaseRequest, TestCase } from '../types/testCase'
import { csrfHeader } from './csrf'

// Without a folderId this returns every test case; with one, only the test cases directly in that folder
export async function getTestCases(folderId?: string): Promise<TestCase[]> {
    const query = folderId ? `?${new URLSearchParams({ folderId })}` : ''
    const response = await fetch(`/api/testcases${query}`)

    if (response.status === 401) {
        throw new Error('Failed to fetch test cases (401 Unauthorized). Sign in to access test cases.')
    }

    if (!response.ok) {
        throw new Error(`Failed to fetch test cases (${response.status})`)
    }

    if (!response.headers.get('content-type')?.includes('application/json')) {
        throw new Error('Failed to fetch test cases: the server returned a non-JSON response.')
    }

    const data: { content: TestCase[] } = await response.json()
    if (!Array.isArray(data.content)) {
        throw new TypeError('Failed to fetch test cases: the response is missing the test case list.')
    }

    return data.content
}   

export async function getTestCase(id: string): Promise<TestCase> {
  const response = await fetch(`/api/testcases/${encodeURIComponent(id)}`)

  if (!response.ok) {
    throw new Error(`Failed to fetch test case (${response.status})`)
  }

  return response.json()
}

export async function createTestCase(testCase: CreateTestCaseRequest): Promise<TestCase> {
    const response = await fetch('/api/testcases', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...await csrfHeader(),
        },
        body: JSON.stringify(testCase),
    }) 

    if (response.status === 401) {
        throw new Error('Failed to create test case (401 Unauthorized). Sign in to add a test case.')
    }

    // Nothing is saved in this case, so the form can be resubmitted after fixing the folders
    if (response.status === 404) {
        throw new Error('One of the chosen folders no longer exists. Reload the page and pick the folders again.')
    }

    if (!response.ok) {
        throw new Error(`Failed to create test case (${response.status})`)
    }

    return response.json()
}

export async function deleteTestCase(id: string): Promise<void> {
    const response = await fetch(`/api/testcases/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        headers: await csrfHeader(),
    })

    if (!response.ok) {
        switch (response.status) {
            case 401:
                throw new Error('Failed to delete test case (401 Unauthorized). Sign in to delete test cases.')
            case 404:
                throw new Error('This test case no longer exists.')
            default:
                throw new Error(`Failed to delete test case (${response.status})`)
        }
    }
}