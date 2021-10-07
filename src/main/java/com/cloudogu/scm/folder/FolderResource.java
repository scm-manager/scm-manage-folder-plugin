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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;

@Path("v2/folder")
public class FolderResource {

  private final FolderService folderService;

  @Inject
  FolderResource(FolderService folderService) {
    this.folderService = folderService;
  }

  @POST
  @Path("{namespace}/{name}/create/{path: .*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createFolder(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Nullable @PathParam("path") String path,
                               @Valid CreateFolderDto dto) throws IOException {
    folderService.create(namespace, name, dto.getBranch(), path, dto.getFolderName(), dto.getCommitMessage(), dto.getExpectedRevision());
    return Response.status(CREATED).entity(dto).build();
  }

  @POST
  @Path("{namespace}/{name}/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createFolderInRoot(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Valid CreateFolderDto dto) throws IOException {
    folderService.create(namespace, name, dto.getBranch(), "", dto.getFolderName(), dto.getCommitMessage(), dto.getExpectedRevision());
    return Response.status(CREATED).entity(dto).build();
  }

  @POST
  @Path("{namespace}/{name}/delete/{path: .*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteFolder(@PathParam("namespace") String namespace,
                               @PathParam("name") String name,
                               @Nullable @PathParam("path") String path,
                               @Valid CreateFolderDto dto) throws IOException {
    folderService.create(namespace, name, dto.getBranch(), path, dto.getFolderName(), dto.getCommitMessage(), dto.getExpectedRevision());
    return Response.status(NO_CONTENT).entity(dto).build();
  }

}
