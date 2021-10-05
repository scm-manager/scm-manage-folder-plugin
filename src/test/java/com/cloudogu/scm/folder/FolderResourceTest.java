package com.cloudogu.scm.folder;

import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.ChangesetDto;
import sonia.scm.api.v2.resources.ChangesetToChangesetDtoMapper;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Person;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.web.JsonMockHttpRequest;
import sonia.scm.web.RestDispatcher;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class FolderResourceTest {
  @Mock
  ChangesetToChangesetDtoMapper mapper;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  FolderService folderService;

  @InjectMocks
  FolderResource resource;

  RestDispatcher dispatcher;
  MockHttpResponse response;

  @BeforeEach
  void initDispatcher() {
    dispatcher = new RestDispatcher();
    dispatcher.addSingletonResource(resource);
    response = new MockHttpResponse();
  }

  @Test
  void shouldHandleCreateRequest() throws URISyntaxException, IOException {
    final Changeset changeset = new Changeset("1337", new Date().getTime(), new Person("Trillian"));
    final ChangesetDto changesetDto = new ChangesetDto();
    changesetDto.setId(changeset.getId());
    when(folderService.create("space", "jam", null, "some/folder", "a new folder is born"))
      .thenReturn(changeset);
    when(mapper.map(eq(changeset), any())).thenReturn(changesetDto);
    JsonMockHttpRequest request =
      JsonMockHttpRequest
        .post("/v2/folder/space/jam/create/some/folder")
        .json("{'commitMessage': 'a new folder is born'}")
        .contentType(MediaType.APPLICATION_JSON_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getContentAsString()).contains("\"id\":\"1337\"");
    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Test
  void shouldNotAllowEmptyCommitMessageForCreate() throws URISyntaxException, IOException {
    JsonMockHttpRequest request =
      JsonMockHttpRequest
        .post("/v2/folder/space/jam/create/some/folder")
        .json("{'commitMessage': ''}")
        .contentType(MediaType.APPLICATION_JSON_TYPE);

    dispatcher.invoke(request, response);

    verify(folderService, never()).create(anyString(), anyString(), anyString(), anyString(), anyString());
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void shouldHandleDeleteRequest() throws URISyntaxException, IOException {
    final Changeset changeset = new Changeset("1337", new Date().getTime(), new Person("Trillian"));
    final ChangesetDto changesetDto = new ChangesetDto();
    changesetDto.setId(changeset.getId());
    when(folderService.delete("space", "jam", null, "some/folder", "a new folder is born"))
      .thenReturn(changeset);
    when(mapper.map(eq(changeset), any())).thenReturn(changesetDto);
    JsonMockHttpRequest request =
      JsonMockHttpRequest
        .post("/v2/folder/space/jam/delete/some/folder")
        .json("{'commitMessage': 'a new folder is born'}")
        .contentType(MediaType.APPLICATION_JSON_TYPE);

    dispatcher.invoke(request, response);

    assertThat(response.getContentAsString()).contains("\"id\":\"1337\"");
    assertThat(response.getStatus()).isEqualTo(201);
  }

  @Test
  void shouldNotAllowEmptyCommitMessageForDelete() throws URISyntaxException, IOException {
    JsonMockHttpRequest request =
      JsonMockHttpRequest
        .post("/v2/folder/space/jam/delete/some/folder")
        .json("{'commitMessage': ''}")
        .contentType(MediaType.APPLICATION_JSON_TYPE);

    dispatcher.invoke(request, response);

    verify(folderService, never()).delete(anyString(), anyString(), anyString(), anyString(), anyString());
    assertThat(response.getStatus()).isEqualTo(400);
  }
}
