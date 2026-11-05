export interface Folder {
  id: string;
  name: string;
  parentId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export type FolderRequest = Pick<Folder, 'name' | 'parentId'>

// Leave a field out to keep its current value. Only an explicit parentId moves the folder;
// parentId: null moves it to the top level.
export type UpdateFolderRequest = Partial<FolderRequest>
