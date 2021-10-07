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
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;

import javax.inject.Inject;
import javax.inject.Provider;

@Extension
@Enrich(FileObject.class)
public class FileLinkEnricher implements HalEnricher {

  private final Provider<ScmPathInfoStore> scmPathInfoStore;
  private final EditorPreconditions editorPreconditions;
  private final ChangeGuardCheck changeGuardCheck;

  @Inject
  public FileLinkEnricher(Provider<ScmPathInfoStore> scmPathInfoStore, EditorPreconditions editorPreconditions, ChangeGuardCheck changeGuardCheck) {
    this.scmPathInfoStore = scmPathInfoStore;
    this.editorPreconditions = editorPreconditions;
    this.changeGuardCheck = changeGuardCheck;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    NamespaceAndName namespaceAndName = context.oneRequireByType(NamespaceAndName.class);
    BrowserResult browserResult = context.oneRequireByType(BrowserResult.class);
    FileObject fileObject = context.oneRequireByType(FileObject.class);
    if (editorPreconditions.isEditable(namespaceAndName, browserResult)) {
      appendLinks(appender, fileObject, namespaceAndName, browserResult.getRequestedRevision());
    }
  }

  private void appendLinks(HalAppender appender, FileObject fileObject, NamespaceAndName namespaceAndName, String revision) {
    if (fileObject.isDirectory()) {
      appendDirectoryLinks(appender, fileObject, namespaceAndName, revision);
    }
  }

  private void appendDirectoryLinks(HalAppender appender, FileObject fileObject, NamespaceAndName namespaceAndName, String revision) {
    LinkBuilder linkBuilder = new LinkBuilder(scmPathInfoStore.get().get(), FolderResource.class);
    if (changeGuardCheck.canCreateFilesIn(namespaceAndName, revision, fileObject.getPath()).isEmpty()) {
      appender.appendLink("createFolder", createCreateLink(fileObject, namespaceAndName, linkBuilder));
    }
    if (changeGuardCheck.isDeletable(namespaceAndName, revision, fileObject.getPath()).isEmpty()) {
      appender.appendLink("deleteFolder", createDeleteLink(fileObject, namespaceAndName, linkBuilder));
    }
  }

  private String createCreateLink(FileObject fileObject, NamespaceAndName namespaceAndName, LinkBuilder linkBuilder) {
    return createModifyLink("createFolder", fileObject.getPath(), namespaceAndName, linkBuilder);
  }

  private String createDeleteLink(FileObject fileObject, NamespaceAndName namespaceAndName, LinkBuilder linkBuilder) {
    return createModifyLink("deleteFolder", fileObject.getPath(), namespaceAndName, linkBuilder);
  }

  private String createModifyLink(String method, String path, NamespaceAndName namespaceAndName, LinkBuilder linkBuilder) {
    return linkBuilder.method(method).parameters(namespaceAndName.getNamespace(), namespaceAndName.getName(), path).href();
  }
}
