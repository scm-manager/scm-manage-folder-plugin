/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.cloudogu.scm.folder;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import sonia.scm.ContextEntry;
import sonia.scm.NotFoundException;
import sonia.scm.repository.Branch;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.BrowseCommandBuilder;
import sonia.scm.repository.api.LogCommandBuilder;
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
  static final byte[] KEEP_FILE_CONTENT = "This file was created automatically.".getBytes(UTF_8);
  static final String KEEP_FILE_NAME = ".scmkeep";
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public FolderService(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  Changeset create(String namespace, String repositoryName, String branch, String path, String commitMessage) throws IOException {
    doThrow()
      .violation("invalid path: ", path)
      .when(!ValidationUtil.isPathValid(path) || StringUtils.isEmpty(path));

    try (RepositoryService repositoryService = repositoryServiceFactory.create(new NamespaceAndName(namespace, repositoryName))) {
      RepositoryPermissions.push(repositoryService.getRepository()).check();

      ModifyCommandBuilder modifyCommand = repositoryService.getModifyCommand();
      if (!Strings.isNullOrEmpty(branch)) {
        modifyCommand.setBranch(branch);
      }
      modifyCommand.setCommitMessage(commitMessage);
      createKeepFile(modifyCommand, path);
      String newChangesetId = modifyCommand.execute();

      return getChangeset(repositoryService, branch , newChangesetId);
    }
  }

  Changeset delete(String namespace, String repositoryName, @CheckForNull String branch, String path, String commitMessage) throws IOException {
    doThrow()
      .violation("invalid path: ", path)
      .when(!ValidationUtil.isPathValid(path) || StringUtils.isEmpty(path));

    try (RepositoryService repositoryService = repositoryServiceFactory.create(new NamespaceAndName(namespace, repositoryName))) {
      Repository repository = repositoryService.getRepository();
      RepositoryPermissions.push(repository).check();

      String[] pathParts = path.split("/");
      String folderName = pathParts[pathParts.length - 1];
      String parentPath = getParentPath(pathParts);

      FileObject parentFile = findFile(repositoryService, branch, parentPath);
      FileObject fileToDelete = findChildByName(parentFile, folderName);

      assertIsValidDirectory(repository, branch, path, fileToDelete);

      String newChangesetId = createDeleteCommand(repositoryService, branch, parentPath, parentFile, fileToDelete, commitMessage).execute();
      return getChangeset(repositoryService, branch, newChangesetId);
    }
  }

  private FileObject findFile(RepositoryService repositoryService, String branch, String path) throws IOException {
    BrowseCommandBuilder browseCommandBuilder = repositoryService.getBrowseCommand()
      .setDisableCache(true)
      .setDisableLastCommit(true)
      .setDisablePreProcessors(true)
      .setDisableSubRepositoryDetection(true)
      .setPath(path);

    if (!Strings.isNullOrEmpty(branch)) {
      browseCommandBuilder.setRevision(branch);
    }

    BrowserResult browserResult = browseCommandBuilder.getBrowserResult();

    return browserResult.getFile();
  }

  private void assertIsValidDirectory(Repository repository, String branch, String path, FileObject fileToDelete) {
    if (fileToDelete == null) {
      throw NotFoundException.notFound(createErrorContext(branch, path, repository));
    }

    if (!fileToDelete.isDirectory()) {
      ContextEntry.ContextBuilder contextBuilder = createErrorContext(branch, path, repository);
      throw new PathIsNotADirectoryException(contextBuilder.build(), "The provided path does not belong to a directory, but a file");
    }
  }

  private ModifyCommandBuilder createDeleteCommand(RepositoryService repositoryService, String branch, String parentPath, FileObject parentFile, FileObject fileToDelete, String commitMessage) throws IOException {
    ModifyCommandBuilder modifyCommand = repositoryService.getModifyCommand();
    if (!Strings.isNullOrEmpty(branch)) {
      modifyCommand.setBranch(branch);
    }
    modifyCommand.setCommitMessage(commitMessage);

    modifyCommand.deleteFile(fileToDelete.getPath(), true);
    createKeepFileIfParentIsEmptyAfterDeletion(parentPath, parentFile, modifyCommand);

    return modifyCommand;
  }

  private void createKeepFileIfParentIsEmptyAfterDeletion(String parentPath, FileObject parentFile, ModifyCommandBuilder modifyCommand) throws IOException {
    if (parentFile.getChildren().size() == 1 && isNotRoot(parentFile)) {
      createKeepFile(modifyCommand, parentPath);
    }
  }

  private Changeset getChangeset(RepositoryService repositoryService, @CheckForNull String branch, String changesetId) throws IOException {
    LogCommandBuilder logCommand = repositoryService.getLogCommand();
    if (!Strings.isNullOrEmpty(branch)) {
      logCommand.setBranch(branch);
    }
    return logCommand.getChangeset(changesetId);
  }

  private FileObject findChildByName(FileObject fileObject, String childName) {
    for (FileObject fo : fileObject.getChildren()) {
      if (fo.getName().equals(childName)) {
        return fo;
      }
    }
    return null;
  }

  private String getParentPath(String[] pathParts) {
    StringBuilder parentPath = new StringBuilder();
    for (int i = 0; i <= pathParts.length - 2; i++) {
      if (i != 0) {
        parentPath.append("/");
      }
      parentPath.append(pathParts[i]);
    }
    return parentPath.toString();
  }

  private ContextEntry.ContextBuilder createErrorContext(String branch, String path, Repository repository) {
    ContextEntry.ContextBuilder contextBuilder = new ContextEntry.ContextBuilder();
    contextBuilder.in(repository);
    if (!Strings.isNullOrEmpty(branch)) {
      contextBuilder.in(Branch.class, branch);
    }
    contextBuilder.in("path", path);
    return contextBuilder;
  }

  private String ensureTrailingSlash(String path) {
    if (!path.endsWith("/")) {
      return path + "/";
    }
    return path;
  }

  private void createKeepFile(ModifyCommandBuilder modifyCommand, String path) throws IOException {
    modifyCommand
      .createFile(ensureTrailingSlash(path) + KEEP_FILE_NAME)
      .setOverwrite(true)
      .withData(new ByteArrayInputStream(KEEP_FILE_CONTENT));
  }

  private boolean isNotRoot(FileObject fileObject) {
    return !fileObject.getPath().equals("") && !fileObject.getPath().equals("/");
  }
}
