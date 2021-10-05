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

import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.LinkBuilder;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import javax.inject.Inject;
import javax.inject.Provider;

@Extension
@Enrich(FileObject.class)
public class FileLinkEnricher implements HalEnricher {

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final Provider<ScmPathInfoStore> scmPathInfoStore;

  @Inject
  public FileLinkEnricher(RepositoryServiceFactory repositoryServiceFactory, Provider<ScmPathInfoStore> scmPathInfoStore) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.scmPathInfoStore = scmPathInfoStore;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    NamespaceAndName namespaceAndName = context.oneRequireByType(NamespaceAndName.class);
    FileObject fileObject = context.oneRequireByType(FileObject.class);

    if (fileObject.isDirectory()) {
      try (RepositoryService repositoryService = repositoryServiceFactory.create(namespaceAndName)) {
        if (RepositoryPermissions.push(repositoryService.getRepository()).isPermitted()) {
          LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), FolderResource.class);

          appender.appendLink("createFolder", linkBuilder
            .method("createFolder")
            .parameters(namespaceAndName.getNamespace(), namespaceAndName.getName(), "PATH_PART").href().replace("PATH_PART", fixObjectPath(fileObject.getPath()) + "{path}")
          );

          if (isNotRoot(fileObject)) {
            appender.appendLink("deleteFolder", linkBuilder
              .method("deleteFolder")
              .parameters(namespaceAndName.getNamespace(), namespaceAndName.getName(), fileObject.getPath()).href()
            );
          }
        }
      }
    }
  }

  private boolean isNotRoot(FileObject fileObject) {
    return !fileObject.getPath().equals("") && !fileObject.getPath().equals("/");
  }

  private String fixObjectPath(String path) {
    if (path.isEmpty() || path.equals("/")) {
      return "";
    }
    if (!path.endsWith("/")) {
      return path + "/";
    }
    return path;
  }
}
