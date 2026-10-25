import { useState } from 'react'
import TestCaseTable from '../components/TestCaseTable'
import TestCaseForm from '../components/TestCaseForm'
import { logout } from '../api/auth'

interface TestCasePageProps {
  onLogout: () => void
}

function TestCasePage({ onLogout }: Readonly<TestCasePageProps>) {
  const [addTestCase, setAddTestCase] = useState(false)
  const [listVersion, setListVersion] = useState(0)
  const [signOutError, setSignOutError] = useState<string | null>(null)


  const handleSignOut = async () => {
    setSignOutError(null)
    try {
      await logout()
      onLogout()
    } catch (error) {
      setSignOutError(error instanceof Error ? error.message : 'Failed to sign out. Please try again.')
    }
  }


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
        <section className="test-case-page-footer">
          <div className="section-footer">
            {signOutError && <p className="feedback feedback--error" role="alert">{signOutError}</p>}
            <button className="button button--secondary" type="button" onClick={handleSignOut}>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M9 5H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4" />
                <path d="m16 8 4 4-4 4M20 12H9" />
              </svg>
              Sign Out
            </button>
          </div>
        </section>
      </main>
    </>
  )
}

export default TestCasePage
