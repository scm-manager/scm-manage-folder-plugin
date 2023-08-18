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
        branch: revision || ""
      },
      path
    );

  const body = (
    <>
      {hook.error ? <ErrorNotification error={hook.error} /> : null}
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
        onChange={event => updateFolderName(event.target.value)}
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
