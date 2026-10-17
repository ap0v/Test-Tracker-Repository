import { useState } from 'react'
import TestCaseTable from '../components/TestCaseTable'
import TestCaseForm from '../components/TestCaseForm'

function TestCasePage() {
  const [addTestCase, setAddTestCase] = useState(false)
  const [listVersion, setListVersion] = useState(0)

  return (
    <main className="test-case-page">
      <header className="test-case-page-header">
        <h1>Test Cases</h1>
        <p>Organize and review your test cases.</p>
      </header>

      <button onClick={() => setAddTestCase(true)}>Add Test Case</button>

      {addTestCase && (
        <TestCaseForm
          onCreated={() => {
            setAddTestCase(false)
            setListVersion(version => version + 1)
          }}
          onClose={() => setAddTestCase(false)}
        />
      )}

      <section className="test-case-list" aria-labelledby="test-case-list-heading">
        <h2 id="test-case-list-heading">All test cases</h2>
        <TestCaseTable key={listVersion} />
      </section>
    </main>
  )
}

export default TestCasePage
