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
import { useTranslation } from "react-i18next";
import { Button, ButtonGroup, CommitAuthor, ErrorNotification, Modal, Textarea } from "@scm-manager/ui-components";
import { File, Repository } from "@scm-manager/ui-types";
import { useDeleteFolder } from "./folders";

type Props = {
  repository: Repository;
  revision?: string;
  path?: string;
  sources: File;
  onClose: () => void;
  hook: ReturnType<typeof useDeleteFolder>;
};

const FolderDeleteModal: FC<Props> = ({ onClose, revision, repository, sources, hook }) => {
  const [t] = useTranslation("plugins");
  const [commitMessage, setCommitMessage] = useState("");
  const initialFocusRef = useRef<HTMLTextAreaElement>(null);

  const submit = () =>
    hook.remove(
      repository,
      sources,
      {
        commitMessage,
        branch: decodeURIComponent(revision ?? "")
      },
      revision
    );

  const body = (
    <>
      {hook.error ? <ErrorNotification error={hook.error} /> : null}
      <div className="mb-3">
        <CommitAuthor />
      </div>
      <Textarea
        placeholder={t("scm-manage-folder-plugin.delete.commit.placeholder")}
        onChange={event => setCommitMessage(event.target.value)}
        disabled={hook.isLoading}
        onSubmit={() => !!commitMessage && submit()}
        ref={initialFocusRef}
      />
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button label={t("scm-manage-folder-plugin.delete.cancel.label")} action={onClose} disabled={hook.isLoading} />
      <Button
        label={t("scm-manage-folder-plugin.delete.submit.label")}
        color="primary"
        disabled={!commitMessage || hook.isLoading}
        loading={hook.isLoading}
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
