import { useEffect, useState } from 'react'
import type { SubmitEvent } from 'react'
import { getFolders } from '../api/folder'
import { createTestCase } from '../api/testCase'
import type { Folder } from '../types/folder'
import type { CreateTestCaseRequest, TestCase } from '../types/testCase'

interface FolderOption {
  id: string
  label: string
}

// Labels each folder with its full path so subfolders with the same name can be told apart
function toFolderOptions(folders: Folder[]): FolderOption[] {
  const foldersById = new Map(folders.map(folder => [folder.id, folder]))
  const pathOf = (folder: Folder): string => {
    const parent = folder.parentId ? foldersById.get(folder.parentId) : undefined
    return parent ? `${pathOf(parent)} / ${folder.name}` : folder.name
  }

  return folders
    .map(folder => ({ id: folder.id, label: pathOf(folder) }))
    .sort((a, b) => a.label.localeCompare(b.label))
}

interface TestCaseFormProps {
  // The folder selected in the sidebar when the form opened; it starts out checked. null means none
  folderId: string | null
  onCreated: () => void
  onClose: () => void
}

function TestCaseForm({ folderId: initialFolderId, onCreated, onClose }: Readonly<TestCaseFormProps>) {
  const [title, setTitle] = useState<TestCase['title']>('')
  const [description, setDescription] = useState<TestCase['description']>(null)
  const [preconditions, setPreconditions] = useState<TestCase['preconditions']>(null)
  const [expectedResults, setExpectedResults] = useState<TestCase['expectedResults']>(null)
  const [priority, setPriority] = useState<TestCase['priority']>('MEDIUM')
  const [status, setStatus] = useState<TestCase['status']>('DRAFT')
  const [tags, setTags] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [folderIds, setFolderIds] = useState<string[]>(initialFolderId ? [initialFolderId] : [])
  // null until the folders have loaded
  const [folders, setFolders] = useState<FolderOption[] | null>(null)
  const [foldersError, setFoldersError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const fetchFolders = async () => {
      try {
        const data = await getFolders()
        if (!active) return
        setFolders(toFolderOptions(data))
        // Drop a preselected folder that no longer exists, since it can't be shown or unchecked
        setFolderIds(current => current.filter(id => data.some(folder => folder.id === id)))
      }
      catch (error) {
        if (!active) return
        setFoldersError(error instanceof Error ? error.message : 'Failed to fetch folders.')
        // Without the list nothing can be shown as checked, so don't file the test case anywhere either
        setFolderIds([])
      }
    }

    void fetchFolders()
    return () => { active = false }
  }, [])

  function toggleFolder(id: string, checked: boolean) {
    setFolderIds(current => checked ? [...current, id] : current.filter(folderId => folderId !== id))
  }

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
      folderIds,
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

  let folderMessage: string | null = null
  if (folders === null && !foldersError) folderMessage = 'Loading folders…'
  else if (folders?.length === 0) folderMessage = 'No folders yet.'

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
          <div className="form-field">
            <span className="form-field-label" id="test-case-folders-label">Folders</span>
            <div
              className="folder-picker"
              role="group"
              aria-labelledby="test-case-folders-label"
              aria-describedby="test-case-folders-hint"
              data-testid="folder-picker"
            >
              {folderMessage && <p className="field-hint">{folderMessage}</p>}
              {foldersError && <p className="field-hint">{foldersError}</p>}
              {folders?.map(folder => (
                <label className="folder-picker-option" key={folder.id}>
                  <input
                    type="checkbox"
                    checked={folderIds.includes(folder.id)}
                    onChange={(e) => toggleFolder(folder.id, e.target.checked)}
                  />
                  {folder.label}
                </label>
              ))}
            </div>
            <p className="field-hint" id="test-case-folders-hint">Leave all unchecked to keep it out of every folder.</p>
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
