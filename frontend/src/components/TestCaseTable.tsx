import { useEffect, useState } from 'react' 
import { getTestCases } from '../api/testCase'
import type { TestCase } from '../types/testCase'

function TestCaseTable() {
  const [testCases, setTestCases] = useState<TestCase[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchTestCases = async () => {
      try {
        const data = await getTestCases()
        setTestCases(data)
      }
      catch (error) {
        setError(error instanceof Error ? error.message : 'Failed to fetch test cases.')
      }
      finally {
        setLoading(false)
      }
    }
  
    fetchTestCases()
  }, [])
  if (loading) return <p>Loading test cases...</p>
  if (error) return <p role="alert">{error}</p>

  return (
    <div className="test-case-table">
      <ul>
        {testCases.map(testCase => (
          <li key={testCase.id}>{testCase.title}</li>
        ))}
      </ul>
    </div>
  )
}

export default TestCaseTable
