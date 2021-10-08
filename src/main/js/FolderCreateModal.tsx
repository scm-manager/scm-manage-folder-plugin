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
import React, { FC, useState } from "react";
import {
  ButtonGroup,
  InputField,
  Modal,
  Textarea,
  Button,
  CommitAuthor,
  apiClient,
  ErrorNotification
} from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { Changeset, File, Link, Repository } from "@scm-manager/ui-types";
import { useQueryClient, useMutation } from "react-query";
import { Commit } from "./types";
import { createRedirectUrl } from "./createRedirectUrl";
import { useHistory } from "react-router-dom";

type CreateFolderRequest = {
  repository: Repository;
  sources: File;
  path?: string;
  commit: Commit;
  folderName: string;
};

const useCreateFolder = () => {
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
            `${path || ""}${!path || path.endsWith("/") ? "" : "/"}${folderName}`
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

type Props = {
  repository: Repository;
  revision?: string;
  path?: string;
  sources: File;
  onClose: () => void;
};

const FolderCreateModal: FC<Props> = ({ sources, revision, path, onClose, repository }) => {
  const [t] = useTranslation("plugins");
  const [folderName, setFolderName] = useState("");
  const [commitMessage, setCommitMessage] = useState("");
  const { isLoading, error, create } = useCreateFolder();
  const [folderNameError, setFolderNameError] = useState("");

  const updateFolderName = (newFolderName: string) => {
    if (newFolderName.startsWith("/")) {
      setFolderNameError("scm-manage-folder-plugin.create.name.errors.leadingSlash");
    } else if (newFolderName.trim() === "") {
      setFolderNameError("scm-manage-folder-plugin.create.name.errors.empty");
    } else {
      setFolderNameError("");
    }
    setFolderName(newFolderName);
  };

  const submit = () =>
    create(
      repository,
      sources,
      folderName,
      {
        commitMessage,
        branch: revision || ""
      },
      path
    );

  const body = (
    <>
      {error ? <ErrorNotification error={error} /> : null}
      {revision ? (
        <InputField label={t("scm-manage-folder-plugin.create.branch.label")} value={revision} disabled={true} />
      ) : null}
      <InputField
        label={t("scm-manage-folder-plugin.create.path.label")}
        value={sources.path === "/" ? "/" : "/" + sources.path}
        disabled={true}
      />
      <InputField
        label={t("scm-manage-folder-plugin.create.name.label")}
        value={folderName}
        onChange={updateFolderName}
        disabled={isLoading}
        errorMessage={folderNameError && t(folderNameError)}
        validationError={!!folderNameError}
      />
      <div className="mb-2 mt-5">
        <CommitAuthor />
      </div>
      <Textarea
        placeholder={t("scm-manage-folder-plugin.create.commit.placeholder")}
        onChange={message => setCommitMessage(message)}
        value={commitMessage}
        disabled={isLoading}
      />
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button className="is-marginless" action={onClose} disabled={isLoading}>
        {t("scm-manage-folder-plugin.create.cancel.label")}
      </Button>
      <Button
        className="is-marginless"
        action={submit}
        disabled={!commitMessage || !folderName || !!folderNameError}
        loading={isLoading}
        color="primary"
      >
        {t("scm-manage-folder-plugin.create.submit.label")}
      </Button>
    </ButtonGroup>
  );

  return (
    <Modal
      body={body}
      footer={footer}
      title={t("scm-manage-folder-plugin.create.title")}
      closeFunction={onClose}
      active={true}
    />
  );
};

export default FolderCreateModal;
