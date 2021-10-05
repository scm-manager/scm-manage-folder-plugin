package com.cloudogu.scm.folder;

import org.apache.shiro.authz.AuthorizationException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.ScmConstraintViolationException;
import sonia.scm.api.v2.resources.ScmPathInfoStore;
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.BrowseCommandBuilder;
import sonia.scm.repository.api.ModifyCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.cloudogu.scm.folder.FolderService.KEEP_FILE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SubjectAware("trillian")
@ExtendWith({MockitoExtension.class, ShiroExtension.class})
class FolderServiceTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService repositoryService;
  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;
  @Mock(answer = Answers.RETURNS_SELF)
  private ModifyCommandBuilder modifyCommandBuilder;
  @Mock
  ModifyCommandBuilder.WithOverwriteFlagContentLoader createContentLoader;
  @Mock(answer = Answers.RETURNS_SELF)
  BrowseCommandBuilder browseCommandBuilder;
  @Mock
  BrowserResult browserResult;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  private FolderService folderService;

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
    void shouldCreateCorrectModifyCommand() throws IOException {
      folderService.create(repository.getNamespace(), repository.getName(), "master", "newFolder", "create new folder commit");

      verify(modifyCommandBuilder.createFile("newFolder/" + KEEP_FILE_NAME).setOverwrite(true)).withData(any(InputStream.class));
      verify(modifyCommandBuilder).setCommitMessage("create new folder commit");
      verify(modifyCommandBuilder).setBranch("master");
      verify(modifyCommandBuilder).execute();
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

      when(browserResult.getFile()).thenReturn(createFileObject("root/notAFolder.txt"));

      assertThrows(
        PathIsNotADirectoryException.class,
        () -> folderService.delete(namespace, name, "master", "root/notAFolder.txt", "create crappy folder")
      );
    }

    @SubjectAware(permissions = "repository:push:*")
    @Test
    void shouldDeleteFilesRecursively() throws IOException {
      when(browserResult.getFile()).thenReturn(
        createFileObject("root",
          createFileObject("root/folder",
            createFileObject("root/folder/foo.txt"),
            createFileObject("root/folder/bar.test")
          ),
          createFileObject("root/other",
            createFileObject("root/other/trillian.xml"),
            createFileObject("root/other/space.jar")
          )
        )
      );


      folderService.delete(repository.getNamespace(), repository.getName(), "master", "root/folder", "delete folders");

      InOrder orderVerifier = Mockito.inOrder(modifyCommandBuilder);
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root/folder/foo.txt");
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root/folder/bar.test");
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root/folder");
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root/other/trillian.xml");
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root/other/space.jar");
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root/other");
      orderVerifier.verify(modifyCommandBuilder).deleteFile("root");
      verify(modifyCommandBuilder).setCommitMessage("delete folders");
      verify(modifyCommandBuilder).setBranch("master");
      verify(modifyCommandBuilder).execute();
    }
  }

  private FileObject createFileObject(String path, FileObject ...children) {
    final FileObject fileObject = new FileObject();
    if (children != null && children.length > 0) {
      fileObject.setDirectory(true);
      fileObject.setChildren(Arrays.asList(children));
    }
    fileObject.setPath(path);
    return fileObject;
  }

}
