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

import org.apache.shiro.authz.AuthorizationException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Person;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BrowseCommandBuilder;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.ModifyCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;

import static com.cloudogu.scm.folder.FolderService.KEEP_FILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware("trillian")
@ExtendWith({MockitoExtension.class, ShiroExtension.class})
class FolderServiceTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  RepositoryService repositoryService;
  @Mock
  RepositoryServiceFactory repositoryServiceFactory;
  @Mock(answer = Answers.RETURNS_SELF)
  ModifyCommandBuilder modifyCommandBuilder;
  @Mock
  ModifyCommandBuilder.WithOverwriteFlagContentLoader createContentLoader;
  @Mock(answer = Answers.RETURNS_SELF)
  BrowseCommandBuilder browseCommandBuilder;
  @Mock
  BrowserResult browserResult;
  @Mock
  LogCommandBuilder logCommandBuilder;

  final Repository repository = RepositoryTestData.createHeartOfGold();

  FolderService folderService;

  @BeforeEach
  void setUpObjectUnderTest() throws IOException {
    ScmPathInfoStore pathInfoStore = new ScmPathInfoStore();
    pathInfoStore.set(() -> URI.create("/"));
    lenient().when(repositoryServiceFactory.create(any(NamespaceAndName.class))).thenReturn(repositoryService);
    when(repositoryService.getRepository()).thenReturn(repository);
    when(repositoryService.getModifyCommand()).thenReturn(modifyCommandBuilder);
    lenient().when(repositoryService.getBrowseCommand()).thenReturn(browseCommandBuilder);
    lenient().when(browseCommandBuilder.getBrowserResult()).thenReturn(browserResult);
    lenient().when(modifyCommandBuilder.createFile(anyString())).thenReturn(createContentLoader);
    lenient().when(createContentLoader.setOverwrite(anyBoolean())).thenReturn(createContentLoader);
    lenient().when(createContentLoader.withData(any(ByteArrayInputStream.class))).thenReturn(modifyCommandBuilder);
    lenient().when(repositoryService.getLogCommand()).thenReturn(logCommandBuilder);

    folderService = new FolderService(repositoryServiceFactory);
  }

  @Nested
  class CreateTests {
    @Test
    void shouldValidatePath() {
      final String namespace = repository.getNamespace();
      final String name = repository.getName();

      assertThrows(
        ScmConstraintViolationException.class,
        () -> folderService.create(namespace, name, "master", "/trash//path/", "create crappy folder")
      );
    }

    @Test
    void shouldCheckPermissions() {
      final String namespace = repository.getNamespace();
      final String name = repository.getName();

      assertThrows(
        AuthorizationException.class,
        () -> folderService.create(namespace, name, "master", "newFolder", "create ok folder without permissions")
      );
    }

    @SubjectAware(permissions = "repository:push:*")
    @Test
    void shouldCreateCorrectModifyCommandAndReturnChangeset() throws IOException {
      when(modifyCommandBuilder.execute()).thenReturn("1337");
      when(logCommandBuilder.getChangeset("1337")).thenReturn(new Changeset("1337", new Date().getTime(), new Person("Trillian")));

      final Changeset changeset = folderService.create(repository.getNamespace(), repository.getName(), "master", "newFolder", "create new folder commit");

      verify(modifyCommandBuilder).createFile("newFolder/" + KEEP_FILE_NAME);
      verify(createContentLoader).withData(any(InputStream.class));
      verify(createContentLoader).setOverwrite(true);
      verify(modifyCommandBuilder).setCommitMessage("create new folder commit");
      verify(modifyCommandBuilder).setBranch("master");
      verify(modifyCommandBuilder).execute();
      verify(logCommandBuilder).setBranch("master");
      verify(logCommandBuilder).getChangeset("1337");
      assertThat(changeset).isNotNull();
      assertThat(changeset.getId()).isEqualTo("1337");
    }

    @SubjectAware(permissions = "repository:push:*")
    @Test
    void shouldNotAddSecondTrailingSlashToPath() throws IOException {
      folderService.create(repository.getNamespace(), repository.getName(), "master", "newFolder/", "create new folder commit");

      verify(modifyCommandBuilder).createFile("newFolder/" + KEEP_FILE_NAME);
    }
  }

  @Nested
  class DeleteTests {
    @Test
    void shouldValidatePath() {
      final String namespace = repository.getNamespace();
      final String name = repository.getName();

      assertThrows(
        ScmConstraintViolationException.class,
        () -> folderService.delete(namespace, name, "master", "/trash//path/", "create crappy folder")
      );
    }

    @Test
    void shouldCheckPermissions() {
      final String namespace = repository.getNamespace();
      final String name = repository.getName();

      assertThrows(
        AuthorizationException.class,
        () -> folderService.delete(namespace, name, "master", "newFolder", "create crappy folder")
      );
    }

    @SubjectAware(permissions = "repository:push:*")
    @Test
    void shouldNotAllowFilePaths() {
      final String namespace = repository.getNamespace();
      final String name = repository.getName();

      when(browserResult.getFile()).thenReturn(
        createFileObject("root",
          createFileObject("root/notAFolder.txt")
        )
      );

      assertThrows(
        PathIsNotADirectoryException.class,
        () -> folderService.delete(namespace, name, "master", "root/notAFolder.txt", "create crappy folder")
      );
    }

    @SubjectAware(permissions = "repository:push:*")
    @Test
    void shouldDeleteFilesRecursivelyAndReturnChangeset() throws IOException {
      when(browserResult.getFile()).thenReturn(
        createFileObject("",
          createEmptyDirectoryObject("root")
        )
      );
      when(modifyCommandBuilder.execute()).thenReturn("1337");
      when(logCommandBuilder.getChangeset("1337")).thenReturn(new Changeset("1337", new Date().getTime(), new Person("Trillian")));

      final Changeset changeset = folderService.delete(repository.getNamespace(), repository.getName(), "master", "root/folder", "delete folders");

      verify(modifyCommandBuilder).deleteFile("root", true);
      verify(modifyCommandBuilder, never()).createFile(".scmkeep");
      verify(modifyCommandBuilder).setCommitMessage("delete folders");
      verify(modifyCommandBuilder).setBranch("master");
      verify(modifyCommandBuilder).execute();
      verify(logCommandBuilder).setBranch("master");
      verify(logCommandBuilder).getChangeset("1337");
      assertThat(changeset).isNotNull();
      assertThat(changeset.getId()).isEqualTo("1337");
    }
  }

  @SubjectAware(permissions = "repository:push:*")
  @Test
  void shouldCreateScmKeepIfParentFolderIsEmptyAfterDeletion() throws IOException {
    when(browserResult.getFile()).thenReturn(
      createFileObject("folderWithOneFile",
        createEmptyDirectoryObject("folderWithOneFile/subfolder")
      )
    );
    when(modifyCommandBuilder.execute()).thenReturn("1337");
    when(logCommandBuilder.getChangeset("1337")).thenReturn(new Changeset("1337", new Date().getTime(), new Person("Trillian")));

    folderService.delete(repository.getNamespace(), repository.getName(), "master", "folderWithOneFile/subfolder", "delete subfolder");

    verify(modifyCommandBuilder).createFile("folderWithOneFile/.scmkeep");
  }

  private FileObject createEmptyDirectoryObject(String path) {
    FileObject directory = createFileObject(path);
    directory.setDirectory(true);
    return directory;
  }

  private FileObject createFileObject(String path, FileObject ...children) {
    final FileObject fileObject = new FileObject();
    if (children != null && children.length > 0) {
      fileObject.setDirectory(true);
      fileObject.setChildren(Arrays.asList(children));
    }
    fileObject.setPath(path);
    final String[] pathParts = path.split("/");
    fileObject.setName(pathParts[pathParts.length - 1]);
    return fileObject;
  }

}
