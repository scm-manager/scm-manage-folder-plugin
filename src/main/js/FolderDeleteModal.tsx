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
import React, { FC, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  apiClient,
  Button,
  ButtonGroup,
  CommitAuthor,
  ErrorNotification,
  Modal,
  Textarea
} from "@scm-manager/ui-components";
import { Changeset, File, Link, Repository } from "@scm-manager/ui-types";
import { useHistory, useLocation } from "react-router-dom";
import { Commit } from "./types";
import { createRedirectUrl } from "./createRedirectUrl";
import { useMutation } from "react-query";

type DeleteFolderRequest = {
  commit: Commit;
  sources: File;
  repository: Repository;
  revision?: string;
};

const useDeleteFolder = () => {
  const history = useHistory();
  const location = useLocation();

  const { mutate, data, isLoading, error } = useMutation<Changeset, Error, DeleteFolderRequest>(
    ({ commit, sources }) => {
      const link = (sources._links.deleteFolder as Link).href;
      return apiClient.post(link, commit).then(response => response.json());
    },
    {
      onSuccess: async (changeset, { repository, revision, sources }) => {
        const filePath = location.pathname
          .substr(0, location.pathname.length - sources.name.length - 1)
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

type Props = {
  repository: Repository;
  revision?: string;
  path?: string;
  sources: File;
  onClose: () => void;
};

const FolderDeleteModal: FC<Props> = ({ onClose, revision, repository, sources }) => {
  const [t] = useTranslation("plugins");
  const [commitMessage, setCommitMessage] = useState("");
  const { remove, error, isLoading } = useDeleteFolder();
  const initialFocusRef = useRef<HTMLTextAreaElement>(null);

  const submit = () =>
    remove(
      repository,
      sources,
      {
        commitMessage,
        branch: revision || ""
      },
      revision
    );

  const body = (
    <>
      {error ? <ErrorNotification error={error} /> : null}
      <div className="mb-3">
        <CommitAuthor />
      </div>
      <Textarea
        placeholder={t("scm-manage-folder-plugin.delete.commit.placeholder")}
        onChange={event => setCommitMessage(event.target.value)}
        disabled={isLoading}
        onSubmit={() => !!commitMessage && submit()}
        ref={initialFocusRef}
      />
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button
        className="is-marginless"
        label={t("scm-manage-folder-plugin.delete.cancel.label")}
        action={onClose}
        disabled={isLoading}
      />
      <Button
        className="is-marginless"
        label={t("scm-manage-folder-plugin.delete.submit.label")}
        color="primary"
        disabled={!commitMessage}
        loading={isLoading}
        action={submit}
      />
    </ButtonGroup>
  );

  return (
    <Modal
      title={t("scm-manage-folder-plugin.delete.title")}
      closeFunction={onClose}
      body={body}
      footer={footer}
      active={true}
      initialFocusRef={initialFocusRef}
    />
  );
};

export default FolderDeleteModal;
