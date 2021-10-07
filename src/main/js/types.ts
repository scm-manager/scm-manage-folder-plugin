type CommitDto = {
  commitMessage: string;
  branch: string;
  expectedRevision?: string;
};

export type CreateFolderDto = CommitDto & {
  folderName: string;
};

export type DeleteFolderDto = CommitDto & {
  path: string;
};
