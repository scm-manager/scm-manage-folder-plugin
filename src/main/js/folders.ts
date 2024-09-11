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
