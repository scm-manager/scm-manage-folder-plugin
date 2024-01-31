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

import { Changeset, File, Link, Repository } from "@scm-manager/ui-types";
import { useHistory, useLocation } from "react-router-dom";
import { Commit } from "./types";
import { createRedirectUrl } from "./createRedirectUrl";
import { useMutation, useQueryClient } from "react-query";
import { apiClient } from "@scm-manager/ui-components";

type DeleteFolderRequest = {
  commit: Commit;
  sources: File;
  repository: Repository;
  revision?: string;
};

export const useDeleteFolder = () => {
  const history = useHistory();
  const location = useLocation();
  const decodedLocationPathname = decodeURIComponent(location.pathname);

  const { mutate, data, isLoading, error } = useMutation<Changeset, Error, DeleteFolderRequest>(
    ({ commit, sources }) => {
      const link = (sources._links.deleteFolder as Link).href;
      return apiClient.post(link, commit).then(response => response.json());
    },
    {
      onSuccess: async (changeset, { repository, revision, sources }) => {
        const filePath = decodedLocationPathname
          .substring(0, decodedLocationPathname.length - sources.name.length - 1)
          .split("/sources/" + revision)[1];
        history.push(createRedirectUrl(repository, changeset, filePath));
      }
    }
  );
  return {
    remove: (repository: Repository, parent: File, commit: Commit, revision?: string) => {
      mutate({ repository, commit, sources: parent, revision });
    },
    isLoading,
    error,
    changeset: data
  };
};

type CreateFolderRequest = {
  repository: Repository;
  sources: File;
  path?: string;
  commit: Commit;
  folderName: string;
};

export const useCreateFolder = () => {
  const queryClient = useQueryClient();
  const history = useHistory();
  const { mutate, data, isLoading, error } = useMutation<Changeset, Error, CreateFolderRequest>(
    ({ commit, folderName, sources }) => {
      const createLink = (sources._links.createFolder as Link).href.replace("{path}", folderName);
      return apiClient.post(createLink, commit).then(response => response.json());
    },
    {
      onSuccess: async (changeset, { repository, path, folderName }) => {
        await queryClient.invalidateQueries(["repository", repository.namespace, repository.name]);
        history.push(
          createRedirectUrl(
            repository,
            changeset,
            `${path ?? ""}${!path || path.endsWith("/") ? "" : "/"}${folderName}`
          )
        );
      }
    }
  );
  return {
    create: (repository: Repository, parent: File, folderName: string, commit: Commit, path?: string) => {
      mutate({ repository, folderName, commit, sources: parent, path });
    },
    isLoading,
    error,
    changeset: data
  };
};
