import { useEffect, useState } from 'react'
import { getTestCases, deleteTestCase } from '../api/testCase'
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

function TestCaseTable({ folderId } : Readonly<{ folderId: string | null }>) {
  const [testCases, setTestCases] = useState<TestCase[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const fetchTestCases = async () => {
      try {
        const data = await getTestCases(folderId ?? undefined)
        if (active) setTestCases(data)
      }
      catch (error) {
        if (active) setError(error instanceof Error ? error.message : 'Failed to fetch test cases.')
      }
      finally {
        if (active) setLoading(false)
      }
    }
    
    void fetchTestCases()
    return () => { active = false }
  }, [folderId])


  if (loading) {
    return (
      <div className="loading-state">
        <output>Loading your test cases…</output>
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

  async function handleDelete(testCase : TestCase) {
    const confirmed = window.confirm(`Delete "${testCase.title}"? This can't be undone.`)
    if (!confirmed) return

    try {
      await deleteTestCase(testCase.id)
      setTestCases(current => current.filter(t => t.id !== testCase.id))
    }
    catch (error) {
      setDeleteError(error instanceof Error ? error.message : 'Failed to delete test case.')
    }
  }

  return (
    <>
      {deleteError && <p className="feedback feedback--error" role="alert">{deleteError}</p>}
      <div className="table-scroll">
        <table className="test-case-table">
          <caption className="visually-hidden">Test cases with their status, priority, and tags</caption>
          <thead>
            <tr>
              <th scope="col">Test case</th>
              <th scope="col">Status</th>
              <th scope="col">Priority</th>
              <th scope="col">Tags</th>
              <th scope="col"><span className="visually-hidden">Actions</span></th>
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
                <td className="test-case-actions">
                  <button
                    className="test-case-delete"
                    type="button"
                    aria-label={`Delete ${testCase.title}`}
                    title="Delete test case"
                    onClick={() => handleDelete(testCase)}
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M6 6l12 12M18 6L6 18" />
                    </svg>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

export default TestCaseTable
