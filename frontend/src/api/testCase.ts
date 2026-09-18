import type { TestCase } from '../types/testCase'

export async function getTestCases(): Promise<TestCase[]> {
    const response = await fetch('/api/testcases')

    if (!response.ok) {
        throw new Error(`Failed to fetch test cases (${response.status})`)
    }

    return response.json()
}   

export async function getTestCase(id: string): Promise<TestCase> {
  const response = await fetch(`/api/testcases/${encodeURIComponent(id)}`)

  if (!response.ok) {
    throw new Error(`Failed to fetch test case (${response.status})`)
  }

  return response.json()
}

export async function createTestCase(testCase: Omit<TestCase, 'id' | 'createdAt' | 'updatedAt'>): Promise<TestCase> {
    const response = await fetch('/api/testcases', {
        method: 'POST',
        headers: {
        'Content-Type': 'application/json',
        },
        body: JSON.stringify(testCase),
    }) 

    if (!response.ok) {
        throw new Error(`Failed to create test case (${response.status})`)
    }

    return response.json()
}
