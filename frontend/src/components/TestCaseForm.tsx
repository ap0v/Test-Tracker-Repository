import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { createTestCase } from '../api/testCase'
import type { CreateTestCaseRequest, TestCase } from '../types/testCase'

function TestCaseForm({ onCreated, onClose }: {
  onCreated: () => void
  onClose: () => void
}) {
  const [title, setTitle] = useState<TestCase['title']>('')
  const [description, setDescription] = useState<TestCase['description']>(null)
  const [preconditions, setPreconditions] = useState<TestCase['preconditions']>(null)
  const [expectedResults, setExpectedResults] = useState<TestCase['expectedResults']>(null)
  const [priority, setPriority] = useState<TestCase['priority']>('MEDIUM')
  const [status, setStatus] = useState<TestCase['status']>('DRAFT')
  const [tags, setTags] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleAddTestCase(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    const newTestCase: CreateTestCaseRequest = {
      title,
      description,
      preconditions,
      expectedResults,
      priority,
      status,
      tags: tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0),
    }

    setError(null)
    setIsSubmitting(true)

    try {
      await createTestCase(newTestCase)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Unable to create the test case. Please try again.'
      )
      return
    } finally {
      setIsSubmitting(false)
    }

    onCreated()
  }

  return (
    <section className="test-case-form" id="new-test-case" aria-labelledby="new-test-case-heading">
      <header className="section-heading">
        <h2 id="new-test-case-heading">New test case</h2>
        <p>Describe what to test and what a successful result looks like.</p>
      </header>
      <form onSubmit={handleAddTestCase} aria-busy={isSubmitting}>
        <fieldset className="form-grid" disabled={isSubmitting}>
          <legend className="visually-hidden">Test case details</legend>
          <div className="form-field form-field--full">
            <label htmlFor="test-case-title">Title <span className="muted">(required)</span></label>
            <input
              id="test-case-title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Sign in with valid credentials"
              required
              autoFocus
              data-testid="title-input"
            />
          </div>
          <div className="form-field form-field--full">
            <label htmlFor="test-case-description">Description</label>
            <textarea
              id="test-case-description"
              value={description ?? ''}
              onChange={(e) => setDescription(e.target.value || null)}
              placeholder="What does this test cover?"
              rows={3}
              data-testid="description-input"
            />
          </div>
          <div className="form-field">
            <label htmlFor="test-case-preconditions">Preconditions</label>
            <textarea
              id="test-case-preconditions"
              value={preconditions ?? ''}
              onChange={(e) => setPreconditions(e.target.value || null)}
              placeholder="What needs to be in place before testing?"
              rows={3}
              data-testid="preconditions-input"
            />
          </div>
          <div className="form-field">
            <label htmlFor="test-case-expected-results">Expected results</label>
            <textarea
              id="test-case-expected-results"
              value={expectedResults ?? ''}
              onChange={(e) => setExpectedResults(e.target.value || null)}
              placeholder="What should happen when the test passes?"
              rows={3}
              data-testid="expected-results-input"
            />
          </div>
          <div className="form-field">
            <label htmlFor="test-case-priority">Priority</label>
            <select
              id="test-case-priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value as TestCase['priority'])}
              data-testid="priority-select"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
          <div className="form-field">
            <label htmlFor="test-case-status">Status</label>
            <select
              id="test-case-status"
              value={status}
              onChange={(e) => setStatus(e.target.value as TestCase['status'])}
              data-testid="status-select"
            >
              <option value="DRAFT">Draft</option>
              <option value="READY">Ready</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </div>
          <div className="form-field form-field--full">
            <label htmlFor="test-case-tags">Tags</label>
            <input
              id="test-case-tags"
              type="text"
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              placeholder="e.g. authentication, smoke test"
              aria-describedby="test-case-tags-hint"
              data-testid="tags-input"
            />
            <p className="field-hint" id="test-case-tags-hint">Separate tags with commas.</p>
          </div>
        </fieldset>

        {error && <p className="feedback feedback--error" role="alert">{error}</p>}

        <div className="form-actions">
          <button
            className="button button--secondary"
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            data-testid="close-button"
          >
            Cancel
          </button>
          <button
            className="button button--primary"
            type="submit"
            disabled={isSubmitting}
            data-testid="submit-button"
          >
            {isSubmitting ? 'Creating test case…' : 'Create test case'}
          </button>
        </div>
      </form>
    </section>
  )
}

export default TestCaseForm
