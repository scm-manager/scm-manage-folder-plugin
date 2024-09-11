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

import React, { FC, useRef, useState } from "react";
import {
  Button,
  ButtonGroup,
  CommitAuthor,
  ErrorNotification,
  InputField,
  Modal,
  Textarea
} from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { File, Repository } from "@scm-manager/ui-types";
import { useCreateFolder } from "./folders";

type Props = {
  repository: Repository;
  revision?: string;
  path?: string;
  sources: File;
  onClose: () => void;
  hook: ReturnType<typeof useCreateFolder>;
};

const FolderCreateModal: FC<Props> = ({ sources, revision, path, onClose, repository, hook }) => {
  const [t] = useTranslation("plugins");
  const [folderName, setFolderName] = useState("");
  const [commitMessage, setCommitMessage] = useState("");
  const [folderNameError, setFolderNameError] = useState("");
  const initialFocusRef = useRef<HTMLInputElement>(null);
  const submitDisabled = !commitMessage || !folderName || !!folderNameError;

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
    hook.create(
      repository,
      sources,
      folderName,
      {
        commitMessage,
        branch: decodeURIComponent(revision ?? "")
      },
      path
    );

  const body = (
    <>
      {hook.error ? <ErrorNotification error={hook.error} /> : null}
      {revision ? (
        <InputField label={t("scm-manage-folder-plugin.create.branch.label")} value={decodeURIComponent(revision ?? "")} disabled={true} />
      ) : null}
      <InputField
        label={t("scm-manage-folder-plugin.create.path.label")}
        value={sources.path === "/" ? "/" : "/" + sources.path}
        disabled={true}
      />
      <InputField
        label={t("scm-manage-folder-plugin.create.name.label")}
        value={decodeURIComponent(folderName)}
        onChange={event => updateFolderName(encodeURIComponent(event.target.value))}
        disabled={hook.isLoading}
        errorMessage={folderNameError && t(folderNameError)}
        validationError={!!folderNameError}
        onReturnPressed={() => !submitDisabled && submit()}
        ref={initialFocusRef}
      />
      <div className="mb-2 mt-5">
        <CommitAuthor />
      </div>
      <Textarea
        placeholder={t("scm-manage-folder-plugin.create.commit.placeholder")}
        onChange={message => setCommitMessage(message)}
        value={commitMessage}
        disabled={hook.isLoading}
        onSubmit={() => !submitDisabled && submit()}
      />
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button action={onClose} disabled={hook.isLoading}>
        {t("scm-manage-folder-plugin.create.cancel.label")}
      </Button>
      <Button action={submit} disabled={submitDisabled || hook.isLoading} loading={hook.isLoading} color="primary">
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
      initialFocusRef={initialFocusRef}
    />
  );
};

export default FolderCreateModal;
