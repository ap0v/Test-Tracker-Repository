function TestCasePage() {
  return (
    <main className="test-case-page">
      <header className="test-case-page-header">
        <h1>Test Cases</h1>
        <p>Organize and review your test cases.</p>
      </header>

      <section className="test-case-list" aria-labelledby="test-case-list-heading">
        <h2 id="test-case-list-heading">All test cases</h2>
        {/* Replace this placeholder with TestCaseTable when data is connected. */}
        <div className="test-case-empty-state">
          <p>No test cases to display.</p>
        </div>
      </section>
    </main>
  )
}

export default TestCasePage
