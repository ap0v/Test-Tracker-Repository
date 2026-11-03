export interface Folder {
  id: string;
  name: string;
  parentId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export type FolderRequest = Pick<Folder, 'name' | 'parentId'>
