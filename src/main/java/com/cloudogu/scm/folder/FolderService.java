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
  static final ByteArrayInputStream KEEP_FILE_CONTENT = new ByteArrayInputStream("This file was created automatically.".getBytes(UTF_8));
  static final String KEEP_FILE_NAME = ".scmkeep";
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public FolderService(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  Changeset create(String namespace, String repositoryName, String branch, String path, String commitMessage) throws IOException {
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
      createKeepFile(path, modifyCommand);
      String changesetId = modifyCommand.execute();
      LogCommandBuilder logCommand = repositoryService.getLogCommand();
      if (!Strings.isNullOrEmpty(branch)) {
        logCommand.setBranch(branch);
      }
      return logCommand.getChangeset(changesetId);
    }
  }

  Changeset delete(String namespace, String repositoryName, @CheckForNull String branch, String path, String commitMessage) throws IOException {
    // validate path
    doThrow()
      .violation("invalid path: ", path)
      .when(!ValidationUtil.isPathValid(path) || StringUtils.isEmpty(path));
    try (RepositoryService repositoryService = repositoryServiceFactory.create(new NamespaceAndName(namespace, repositoryName))) {
      // check permissions
      final Repository repository = repositoryService.getRepository();
      RepositoryPermissions.push(repository).check();

      final String[] pathParts = path.split("/");
      String folderName = pathParts[pathParts.length - 1];
      StringBuilder parentPath = new StringBuilder();
      for (int i = 0; i <= pathParts.length - 2; i++) {
        if (i != 0) {
          parentPath.append("/");
        }
        parentPath.append(pathParts[i]);
      }

      final BrowseCommandBuilder browseCommandBuilder = repositoryService.getBrowseCommand()
        .setDisableCache(true)
        .setDisableLastCommit(true)
        .setDisablePreProcessors(true)
        .setDisableSubRepositoryDetection(true)
        .setPath(parentPath.toString());

      if (!Strings.isNullOrEmpty(branch)) {
        browseCommandBuilder.setRevision(branch);
      }

      final BrowserResult browserResult = browseCommandBuilder.getBrowserResult();

      final FileObject parentFile = browserResult.getFile();
      FileObject file = null;
      boolean createScmKeepInParent = false;
      if (parentFile.getChildren().size() == 1) {
        createScmKeepInParent = true;
        file = parentFile.getChildren().iterator().next();
      } else {
        for (FileObject fo : parentFile.getChildren()) {
          if (fo.getName().equals(folderName)) {
            file = fo;
          }
        }
      }
      if (file == null) {
        throw NotFoundException.notFound(createErrorContext(branch, path, repository));
      }

      // check if path is folder
      if (!file.isDirectory()) {
        final ContextEntry.ContextBuilder contextBuilder = createErrorContext(branch, path, repository);
        throw new PathIsNotADirectoryException(contextBuilder.build(), "The provided path does not belong to a directory, but a file");
      }

      // delete files and folders with modify command
      final ModifyCommandBuilder modifyCommand = repositoryService.getModifyCommand();
      modifyCommand.deleteFile(file.getPath(), true);
      // create keep file if parent would be empty after deletion
      if (createScmKeepInParent) {
        createKeepFile(parentPath.toString(), modifyCommand);
      }
      if (!Strings.isNullOrEmpty(branch)) {
        modifyCommand.setBranch(branch);
      }
      modifyCommand.setCommitMessage(commitMessage);
      String changesetId = modifyCommand.execute();
      LogCommandBuilder logCommand = repositoryService.getLogCommand();
      if (!Strings.isNullOrEmpty(branch)) {
        logCommand.setBranch(branch);
      }
      return logCommand.getChangeset(changesetId);
    }
  }

  private ContextEntry.ContextBuilder createErrorContext(String branch, String path, Repository repository) {
    final ContextEntry.ContextBuilder contextBuilder = new ContextEntry.ContextBuilder();
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

  private ModifyCommandBuilder createKeepFile(String path, ModifyCommandBuilder modifyCommand) throws IOException {
    return modifyCommand
      .createFile(ensureTrailingSlash(path) + KEEP_FILE_NAME)
      .setOverwrite(true)
      .withData(KEEP_FILE_CONTENT);
  }
}
