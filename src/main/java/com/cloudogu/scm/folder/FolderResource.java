/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.folder;

import sonia.scm.api.v2.resources.ChangesetDto;
import sonia.scm.api.v2.resources.ChangesetToChangesetDtoMapper;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.NamespaceAndName;
import sonia.scm.repository.RepositoryManager;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.CREATED;

@Path("v2/folder")
public class FolderResource {

  private final FolderService folderService;
  private final ChangesetToChangesetDtoMapper changesetMapper;
  private final RepositoryManager repositoryManager;

  @Inject
  FolderResource(FolderService folderService, ChangesetToChangesetDtoMapper changesetMapper, RepositoryManager repositoryManager) {
    this.folderService = folderService;
    this.changesetMapper = changesetMapper;
    this.repositoryManager = repositoryManager;
  }

  @POST
  @Path("{namespace}/{name}/create/{path: .*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createFolder(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Nullable @PathParam("path") String path,
                               @Valid CommitDto dto) throws IOException {
    Changeset newCommit = folderService.create(namespace, name, dto.getBranch(), path, dto.getCommitMessage());
    ChangesetDto newCommitDto = changesetMapper.map(newCommit, repositoryManager.get(new NamespaceAndName(namespace, name)));
    return Response.status(CREATED).entity(newCommitDto).build();
  }

  @POST
  @Path("{namespace}/{name}/delete/{path: .*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteFolder(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Nullable @PathParam("path") String path,
                               @Valid CommitDto dto) throws IOException {
    final Changeset newCommit = folderService.delete(namespace, name, dto.getBranch(), path, dto.getCommitMessage());
    ChangesetDto newCommitDto = changesetMapper.map(newCommit, repositoryManager.get(new NamespaceAndName(namespace, name)));
    return Response.status(CREATED).entity(newCommitDto).build();
  }

}
