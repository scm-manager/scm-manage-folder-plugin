package com.cloudogu.scm.folder;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import sonia.scm.ContextEntry;
import sonia.scm.repository.Branch;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.BrowseCommandBuilder;
import sonia.scm.repository.api.ModifyCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.util.ValidationUtil;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static sonia.scm.ScmConstraintViolationException.Builder.doThrow;

public class FolderService {
  static final ByteArrayInputStream KEEP_FILE_CONTENT = new ByteArrayInputStream("This file was created automatically.".getBytes(UTF_8));
  static final String KEEP_FILE_NAME = ".scmkeep";
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public FolderService(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  void create(String namespace, String repositoryName, String branch, String path, String commitMessage) throws IOException {
    // validate path
    doThrow()
      .violation("invalid path: ", path)
      .when(!ValidationUtil.isPathValid(path) || StringUtils.isEmpty(path));
    // check permissions
    try (RepositoryService repositoryService = repositoryServiceFactory.create(new NamespaceAndName(namespace, repositoryName))) {
      RepositoryPermissions.push(repositoryService.getRepository()).check();
      // create folder with modify command
      ModifyCommandBuilder modifyCommand = repositoryService.getModifyCommand();
      if (!Strings.isNullOrEmpty(branch)) {
        modifyCommand.setBranch(branch);
      }
      modifyCommand.setCommitMessage(commitMessage);
      modifyCommand
        .createFile(path + KEEP_FILE_NAME)
        .setOverwrite(true)
        .withData(KEEP_FILE_CONTENT);
      modifyCommand.execute();
    }
  }

  void delete(String namespace, String repositoryName, @CheckForNull String branch, String path, String commitMessage) throws IOException {
    // validate path
    doThrow()
      .violation("invalid path: ", path)
      .when(!ValidationUtil.isPathValid(path) || StringUtils.isEmpty(path));
    try (RepositoryService repositoryService = repositoryServiceFactory.create(new NamespaceAndName(namespace, repositoryName))) {
      // check permissions
      final Repository repository = repositoryService.getRepository();
      RepositoryPermissions.push(repository).check();

      final BrowseCommandBuilder browseCommandBuilder = repositoryService.getBrowseCommand()
        .setDisableCache(true)
        .setDisableLastCommit(true)
        .setDisablePreProcessors(true)
        .setDisableSubRepositoryDetection(true)
        .setRecursive(true)
        .setPath(path);

      if (!Strings.isNullOrEmpty(branch)) {
        browseCommandBuilder.setRevision(branch);
      }

      final BrowserResult browserResult = browseCommandBuilder.getBrowserResult();

      // check if path is folder
      final FileObject file = browserResult.getFile();
      if (!file.isDirectory()) {
        final ContextEntry.ContextBuilder contextBuilder = new ContextEntry.ContextBuilder();
        contextBuilder.in(repository);
        if (!Strings.isNullOrEmpty(branch)) {
          contextBuilder.in(Branch.class, branch);
        }
        contextBuilder.in("path", path);
        throw new PathIsNotADirectoryException(contextBuilder.build(), "The provided path does not belong to a directory, but a file");
      }

      // delete files and folders with modify command
      final ModifyCommandBuilder modifyCommand = repositoryService.getModifyCommand();
      deleteFileRecursively(file, modifyCommand);
      if (!Strings.isNullOrEmpty(branch)) {
        modifyCommand.setBranch(branch);
      }
      modifyCommand.setCommitMessage(commitMessage);
      modifyCommand.execute();
    }
  }

  private void deleteFileRecursively(FileObject file, ModifyCommandBuilder modifyCommand) {
    if (file.isDirectory()) {
      for (FileObject child : file.getChildren()) {
        deleteFileRecursively(child, modifyCommand);
      }
    }
    modifyCommand.deleteFile(file.getPath());
  }
}
