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
