import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { KeyboardEvent, MouseEvent, SubmitEvent } from 'react'
import { createFolder, getFolders } from '../api/folder'
import type { Folder } from '../types/folder'

interface FolderTreeProps {
  // null means "All test cases"
  selectedFolderId: string | null
  onSelect: (folderId: string | null) => void
}

interface ContextMenuState {
  folder: Folder
  x: number
  y: number
}

interface NewFolderInputProps {
  parentId: string | null
  onCreated: (folder: Folder) => void
  onCancel: () => void
}

// Inline name field shown where the new folder will appear; Enter creates it, Escape cancels
function NewFolderInput({ parentId, onCreated, onCancel }: Readonly<NewFolderInputProps>) {
  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving || name.trim() === '') return

    setSaving(true)
    setError(null)
    try {
      onCreated(await createFolder({ name: name.trim(), parentId }))
    }
    catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to create folder.')
      setSaving(false)
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') onCancel()
  }

  return (
    <li className="folder-tree-draft">
      <form onSubmit={handleSubmit}>
        <input
          className="folder-tree-input"
          aria-label="New folder name"
          placeholder="Folder name"
          value={name}
          onChange={event => setName(event.target.value)}
          onKeyDown={handleKeyDown}
          // Clicking away from an empty field drops it; a typed name stays until Enter or Escape
          onBlur={() => { if (!saving && name.trim() === '') onCancel() }}
          readOnly={saving}
          aria-busy={saving || undefined}
          aria-invalid={error ? 'true' : undefined}
          autoFocus
        />
      </form>
      {error && <p className="feedback feedback--error" role="alert">{error}</p>}
    </li>
  )
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
  const [menu, setMenu] = useState<ContextMenuState | null>(null)
  // The folder a new subfolder is being named under; null would mean the top level
  const [draft, setDraft] = useState<{ parentId: string | null } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  // The folder button that opened the menu, so Escape can hand focus back to it
  const menuTriggerRef = useRef<HTMLButtonElement | null>(null)

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

    void fetchFolders()
    return () => { active = false }
  }, [])

  // While the menu is open, close it on a click outside it, Escape, or anything that scrolls it out of place
  useEffect(() => {
    if (!menu) return

    const close = () => setMenu(null)
    const closeOnOutsidePointer = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) close()
    }

    menuRef.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus({ preventScroll: true })
    window.addEventListener('pointerdown', closeOnOutsidePointer)
    window.addEventListener('scroll', close, true)
    window.addEventListener('resize', close)
    return () => {
      window.removeEventListener('pointerdown', closeOnOutsidePointer)
      window.removeEventListener('scroll', close, true)
      window.removeEventListener('resize', close)
    }
  }, [menu])

  // Keep the menu on screen when the click was near the right or bottom edge
  useLayoutEffect(() => {
    const element = menuRef.current
    if (!menu || !element) return

    const { width, height } = element.getBoundingClientRect()
    element.style.left = `${Math.max(0, Math.min(menu.x, window.innerWidth - width))}px`
    element.style.top = `${Math.max(0, Math.min(menu.y, window.innerHeight - height))}px`
  }, [menu])

  function openMenu(event: MouseEvent<HTMLButtonElement>, folder: Folder) {
    // Stop the browser's own right-click menu from opening
    event.preventDefault()

    let { clientX: x, clientY: y } = event
    // Shift+F10 and the Menu key fire this too, but without a pointer position
    if (x === 0 && y === 0) {
      const rect = event.currentTarget.getBoundingClientRect()
      x = rect.left
      y = rect.bottom
    }

    menuTriggerRef.current = event.currentTarget
    setMenu({ folder, x, y })
  }

  function handleMenuKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      setMenu(null)
      menuTriggerRef.current?.focus()
    }
    else if (event.key === 'Tab') {
      setMenu(null)
    }
  }

  function startSubfolder(parent: Folder) {
    setMenu(null)
    setDraft({ parentId: parent.id })
  }

  function addFolder() {
    setDraft({ parentId: null })
  }

  function handleFolderCreated(folder: Folder) {
    // Insert in name order so the tree matches what a reload would show
    setFolders(current => [...current, folder].sort((a, b) => a.name.localeCompare(b.name)))
    setDraft(null)
  }

  const childrenByParent = groupByParent(folders)

  function renderFolders(parentId: string | null) {
    return (
      <>
        {draft?.parentId === parentId && (
          <NewFolderInput
            parentId={parentId}
            onCreated={handleFolderCreated}
            onCancel={() => setDraft(null)}
          />
        )}
        {childrenByParent.get(parentId)?.map(folder => (
          <li key={folder.id}>
            <button
              className="folder-tree-item"
              type="button"
              onClick={() => onSelect(folder.id)}
              onContextMenu={event => openMenu(event, folder)}
              aria-current={folder.id === selectedFolderId ? 'true' : undefined}
              aria-haspopup="menu"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              </svg>
              <span className="folder-tree-label">{folder.name}</span>
            </button>
            {/* TODO: rename, move and delete actions (updateFolder, deleteFolder) */}
            {(childrenByParent.has(folder.id) || draft?.parentId === folder.id) && (
              <ul className="folder-tree-list">{renderFolders(folder.id)}</ul>
            )}
          </li>
        ))}
      </>
    )
  }

  return (
    <nav className="folder-tree" aria-labelledby="folder-tree-heading">
      <div className="section-heading">
        <h2 id="folder-tree-heading">Folders</h2>
        <button
          className="folder-tree-add"
          type="button"
          onClick={addFolder}
          aria-label="New folder"
          title="New folder"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
        </button>
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

      {loading && <output className="folder-tree-message">Loading folders…</output>}
      {error && <p className="feedback feedback--error" role="alert">{error}</p>}
      {!loading && !error && folders.length === 0 && <p className="folder-tree-message">No folders yet.</p>}

      {menu && (
        <div
          ref={menuRef}
          className="context-menu"
          role="menu"
          tabIndex={-1}
          aria-label={`Actions for ${menu.folder.name}`}
          style={{ left: menu.x, top: menu.y }}
          onKeyDown={handleMenuKeyDown}
        >
          <button
            className="context-menu-item"
            type="button"
            role="menuitem"
            onClick={() => startSubfolder(menu.folder)}
          >
            Add subfolder
          </button>
        </div>
      )}
    </nav>
  )
}

export default FolderTree
