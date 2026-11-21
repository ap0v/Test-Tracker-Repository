import { useEffect, useState } from 'react'
import { getFolders } from '../api/folder'
import type { Folder } from '../types/folder'

interface FolderTreeProps {
  // null means "All test cases"
  selectedFolderId: string | null
  onSelect: (folderId: string | null) => void
}

// The API returns one flat list sorted by name, so grouping keeps each level in that order
function groupByParent(folders: Folder[]): Map<string | null, Folder[]> {
  const childrenByParent = new Map<string | null, Folder[]>()

  for (const folder of folders) {
    const parentId = folder.parentId ?? null
    const siblings = childrenByParent.get(parentId)
    if (siblings) siblings.push(folder)
    else childrenByParent.set(parentId, [folder])
  }

  return childrenByParent
}

function FolderTree({ selectedFolderId, onSelect }: Readonly<FolderTreeProps>) {
  const [folders, setFolders] = useState<Folder[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const fetchFolders = async () => {
      try {
        const data = await getFolders()
        if (active) setFolders(data)
      }
      catch (error) {
        if (active) setError(error instanceof Error ? error.message : 'Failed to fetch folders.')
      }
      finally {
        if (active) setLoading(false)
      }
    }

    fetchFolders()
    return () => { active = false }
  }, [])

  const childrenByParent = groupByParent(folders)

  function renderFolders(parentId: string | null) {
    return childrenByParent.get(parentId)?.map(folder => (
      <li key={folder.id}>
        <button
          className="folder-tree-item"
          type="button"
          onClick={() => onSelect(folder.id)}
          aria-current={folder.id === selectedFolderId ? 'true' : undefined}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          </svg>
          <span className="folder-tree-label">{folder.name}</span>
        </button>
        {/* TODO: rename, move and delete actions (updateFolder, deleteFolder) */}
        {childrenByParent.has(folder.id) && (
          <ul className="folder-tree-list">{renderFolders(folder.id)}</ul>
        )}
      </li>
    ))
  }

  return (
    <nav className="folder-tree" aria-labelledby="folder-tree-heading">
      <div className="section-heading">
        <h2 id="folder-tree-heading">Folders</h2>
        {/* TODO: "New folder" button (createFolder) */}
      </div>

      {/* "All test cases" stays usable even if the folders fail to load */}
      <ul className="folder-tree-list">
        <li>
          <button
            className="folder-tree-item"
            type="button"
            onClick={() => onSelect(null)}
            aria-current={selectedFolderId === null ? 'true' : undefined}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <rect x="4" y="3" width="16" height="18" rx="3" />
              <path d="M8 8h8M8 12h8M8 16h4" />
            </svg>
            <span className="folder-tree-label">All test cases</span>
          </button>
        </li>
        {renderFolders(null)}
      </ul>

      {loading && <p className="folder-tree-message" role="status">Loading folders…</p>}
      {error && <p className="feedback feedback--error" role="alert">{error}</p>}
      {!loading && !error && folders.length === 0 && <p className="folder-tree-message">No folders yet.</p>}
    </nav>
  )
}

export default FolderTree
