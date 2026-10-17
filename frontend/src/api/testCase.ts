import type { CreateTestCaseRequest, TestCase } from '../types/testCase'

export async function getTestCases(): Promise<TestCase[]> {
    const response = await fetch('/api/testcases')

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
        throw new Error('Failed to fetch test cases: the response is missing the test case list.')
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
    const csrfResponse = await fetch('/api/auth/csrf')
    if (!csrfResponse.ok) {
        throw new Error('Failed to fetch CSRF token')
    }

    const { headerName, token } = await csrfResponse.json()

    const response = await fetch('/api/testcases', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [headerName]: token,
        },
        body: JSON.stringify(testCase),
    }) 

    if (response.status === 401) {
        throw new Error('Failed to create test case (401 Unauthorized). Sign in to add a test case.')
    }

    if (!response.ok) {
        throw new Error(`Failed to create test case (${response.status})`)
    }

    return response.json()
}
