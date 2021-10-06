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

import com.google.inject.util.Providers;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileLinkEnricherTest {

  @Mock
  private Subject subject;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService repositoryService;
  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock
  private HalEnricherContext context;
  @Mock
  private HalAppender appender;

  private final Repository repository = RepositoryTestData.createHeartOfGold();
  BrowserResult fileResult = createBrowserResult(false);
  BrowserResult directoryResult = createBrowserResult(true);

  private FileLinkEnricher enricher;

  @BeforeEach
  void setUpObjectUnderTest() {
    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create("/"));
    lenient().when(repositoryServiceFactory.create(any(NamespaceAndName.class))).thenReturn(repositoryService);
    when(repositoryService.getRepository()).thenReturn(repository);
    enricher = new FileLinkEnricher(repositoryServiceFactory, Providers.of(pathInfoStore));
  }

  @BeforeEach
  void setUpSubject() {
    ThreadContext.bind(subject);
  }

  @AfterEach
  void tearDownSubject() {
    ThreadContext.unbindSubject();
  }

  @Test
  void shouldNotEnrichNonDirectories() {
    setUpRepositoryPermission(repository.getId());

    setUpHalContext(repository, false, "foo");

    enricher.enrich(context, appender);

    verifyNoMoreInteractions(appender);
  }

  @Test
  void shouldNotEnrichWithoutPushPermissions() {
    setUpHalContext(repository, true, "bar");

    enricher.enrich(context, appender);

    verifyNoMoreInteractions(appender);
  }

  @Test
  void shouldAddCreateLink() {
    setUpRepositoryPermission(repository.getId());

    setUpHalContext(repository, true, "root");

    enricher.enrich(context, appender);

    verify(appender).appendLink("createFolder", "/v2/folder/hitchhiker/HeartOfGold/create/root/{path}");
  }

  @Test
  void shouldAddDeleteLink() {
    setUpRepositoryPermission(repository.getId());

    setUpHalContext(repository, true, "dummy");

    enricher.enrich(context, appender);

    verify(appender).appendLink("deleteFolder", "/v2/folder/hitchhiker/HeartOfGold/delete/dummy");
  }

  @Test
  void shouldNotAddDeleteLinkForEmptyPath() {
    setUpRepositoryPermission(repository.getId());

    setUpHalContext(repository, true, "");

    enricher.enrich(context, appender);

    verify(appender, never()).appendLink("deleteFolder", "/v2/folder/hitchhiker/HeartOfGold/delete/dummy");
    verify(appender).appendLink("createFolder", "/v2/folder/hitchhiker/HeartOfGold/create/{path}");
  }

  @Test
  void shouldNotAddDeleteLinkForRootPath() {
    setUpRepositoryPermission(repository.getId());

    setUpHalContext(repository, true, "/");

    enricher.enrich(context, appender);

    verify(appender, never()).appendLink("deleteFolder", "/v2/folder/hitchhiker/HeartOfGold/delete/dummy");
    verify(appender).appendLink("createFolder", "/v2/folder/hitchhiker/HeartOfGold/create/{path}");
  }

  private void setUpHalContext(Repository repository, boolean directory, String path) {
    doReturn(repository.getNamespaceAndName()).when(context).oneRequireByType(NamespaceAndName.class);
    lenient().doReturn(directory ? directoryResult : fileResult).when(context).oneRequireByType(BrowserResult.class);
    FileObject fileObject = new FileObject();
    fileObject.setPath(path);
    fileObject.setDirectory(directory);
    doReturn(fileObject).when(context).oneRequireByType(FileObject.class);
  }

  private BrowserResult createBrowserResult(boolean directory) {
    FileObject fileObject = new FileObject();
    fileObject.setDirectory(directory);
    return new BrowserResult("42", "master", fileObject);
  }

  private void setUpRepositoryPermission(String repositoryId) {
    lenient().when(subject.isPermitted("repository:push:" + repositoryId)).thenReturn(true);
  }
}
