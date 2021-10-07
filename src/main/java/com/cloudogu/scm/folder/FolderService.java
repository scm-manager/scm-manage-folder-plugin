package com.cloudogu.scm.folder;

import org.apache.commons.lang.StringUtils;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.ModifyCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FolderService {
  private final RepositoryServiceFactory repositoryServiceFactory;

  @Inject
  public FolderService(RepositoryServiceFactory repositoryServiceFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
  }

  void create(String namespace, String repositoryName, String branch, String path, String folderName, String commitMessage, String revision) throws IOException {

    try (RepositoryService repositoryService = repositoryServiceFactory.create(new NamespaceAndName(namespace, repositoryName))) {
      RepositoryPermissions.push(repositoryService.getRepository()).check();

      ModifyCommandBuilder modifyCommand = repositoryService.getModifyCommand();
      if (!StringUtils.isEmpty(branch)) {
        modifyCommand.setBranch(branch);
      }
      if (!StringUtils.isEmpty(revision)) {
        modifyCommand.setExpectedRevision(revision);
      }
      modifyCommand.setCommitMessage(commitMessage);
      modifyCommand.createFile(path + "/" + folderName + "/.scmkeep").setOverwrite(true).withData(new ByteArrayInputStream("This file was created automatically.".getBytes(UTF_8)));
      modifyCommand.execute();
    }
  }
}
