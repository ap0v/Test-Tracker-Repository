import { useState } from 'react'
import TestCaseTable from '../components/TestCaseTable'
import TestCaseForm from '../components/TestCaseForm'

function TestCasePage() {
  const [addTestCase, setAddTestCase] = useState(false)
  const [listVersion, setListVersion] = useState(0)

  return (
    <>
      <header className="workspace-header">
        <div className="workspace-header-inner">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <rect x="4" y="3" width="16" height="18" rx="3" />
              <path d="m8 12 3 3 5-6" />
            </svg>
          </span>
          <span className="brand-name">Test Tracker</span>
        </div>
      </header>
      <main className="test-case-page">
        <header className="test-case-page-header">
          <div>
            <h1>Test cases</h1>
            <p>Keep your testing organized and your next steps clear.</p>
          </div>
          <button
            className="button button--primary"
            type="button"
            onClick={() => setAddTestCase(true)}
            aria-expanded={addTestCase}
            aria-controls={addTestCase ? 'new-test-case' : undefined}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            Add test case
          </button>
        </header>

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
          <div className="section-heading">
            <h2 id="test-case-list-heading">All test cases</h2>
            <p>Review the details, status, and priority of your test cases.</p>
          </div>
          <TestCaseTable key={listVersion} />
        </section>
      </main>
    </>
  )
}

export default TestCasePage
