import { useEffect, useState } from 'react'
import { getTestCases } from '../api/testCase'
import type { TestCase } from '../types/testCase'

const statusLabels: Record<TestCase['status'], string> = {
  DRAFT: 'Draft',
  READY: 'Ready',
  ARCHIVED: 'Archived',
}

const priorityLabels: Record<TestCase['priority'], string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

function TestCaseTable() {
  const [testCases, setTestCases] = useState<TestCase[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const fetchTestCases = async () => {
      try {
        const data = await getTestCases()
        if (active) setTestCases(data)
      }
      catch (error) {
        if (active) setError(error instanceof Error ? error.message : 'Failed to fetch test cases.')
      }
      finally {
        if (active) setLoading(false)
      }
    }

    fetchTestCases()
    return () => { active = false }
  }, [])

  if (loading) {
    return (
      <div className="loading-state" role="status">
        <p>Loading your test cases…</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="list-message">
        <p className="feedback feedback--error" role="alert">{error}</p>
      </div>
    )
  }

  if (testCases.length === 0) {
    return (
      <div className="empty-state">
        <span className="empty-state-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="4" y="3" width="16" height="18" rx="3" />
            <path d="M8 8h8M8 12h8M8 16h4" />
          </svg>
        </span>
        <h3>No test cases yet</h3>
        <p>Add your first test case to start organizing your testing.</p>
      </div>
    )
  }

  return (
    <div className="table-scroll" role="region" aria-labelledby="test-case-list-heading" tabIndex={0}>
      <table className="test-case-table">
        <caption className="visually-hidden">Test cases with their status, priority, and tags</caption>
        <thead>
          <tr>
            <th scope="col">Test case</th>
            <th scope="col">Status</th>
            <th scope="col">Priority</th>
            <th scope="col">Tags</th>
          </tr>
        </thead>
        <tbody>
          {testCases.map(testCase => (
            <tr key={testCase.id}>
              <td className="test-case-title">
                {testCase.title}
                {testCase.description && <p className="test-case-description">{testCase.description}</p>}
              </td>
              <td>
                <span className={`badge badge--${testCase.status.toLowerCase()}`}>
                  {statusLabels[testCase.status]}
                </span>
              </td>
              <td>
                <span className={`badge badge--${testCase.priority.toLowerCase()}`}>
                  {priorityLabels[testCase.priority]}
                </span>
              </td>
              <td>
                <div className="test-case-tags">
                  {testCase.tags.length > 0
                    ? testCase.tags.map((tag, index) => <span className="tag" key={`${tag}-${index}`}>{tag}</span>)
                    : <span className="muted">No tags</span>}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default TestCaseTable
