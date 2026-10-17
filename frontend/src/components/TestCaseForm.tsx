import { useState } from 'react' 
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

    const handleAddTestCase = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const newTestCase: CreateTestCaseRequest = {
            title,
            description,
            preconditions,
            expectedResults,
            priority,
            status,
            tags: tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0),
        }

        try {
          await createTestCase(newTestCase)
          setError(null)
          onCreated()
        } catch (err) {
          setError(
            err instanceof Error
              ? err.message
              : 'An error occurred while creating the test case'
          )
        }
    }

    return (
    <div className="test-case-form">
        <h1>Test Case:</h1>
        <form onSubmit={handleAddTestCase}>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Title"
            required
            data-testid="title-input"
          />
          <textarea
            value={description ?? ''}
            onChange={(e) => setDescription(e.target.value || null)}
            placeholder="Description"
            data-testid="description-input"
          />
          <textarea
            value={preconditions ?? ''}
            onChange={(e) => setPreconditions(e.target.value || null)}
            placeholder="Preconditions"
            data-testid="preconditions-input"
          />
          <textarea
            value={expectedResults ?? ''}
            onChange={(e) => setExpectedResults(e.target.value || null)}
            placeholder="Expected Results"
            data-testid="expected-results-input"
          />
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value as TestCase['priority'])}
            data-testid="priority-select"
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as TestCase['status'])}
            data-testid="status-select"
          >
            <option value="DRAFT">Draft</option>
            <option value="READY">Ready</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          <input
            type="text"
            value={tags}
            onChange={(e) => setTags(e.target.value)}
            placeholder="Tags (comma separated)"
            data-testid="tags-input"
          />
          <button type="submit" data-testid="submit-button">Add Test Case</button>
          <button type="button" onClick={onClose} data-testid="close-button">Close</button>
          {error && <p role="alert">{error}</p>}
        </form>
    </div>
  )
}

export default TestCaseForm
