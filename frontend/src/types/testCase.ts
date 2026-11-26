export interface TestCase {
  id: string
  title: string
  description: string | null
  preconditions: string | null
  expectedResults: string | null
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  status: 'DRAFT' | 'READY' | 'ARCHIVED'
  tags: string[]
  createdBy: string | null
  createdAt: string
  updatedAt: string
}

export type CreateTestCaseRequest = Omit<TestCase, 'id' | 'createdBy' | 'createdAt' | 'updatedAt'> & {
  // Folders to file the new test case in; the server rejects the whole request if any is missing
  folderIds?: string[]
}
