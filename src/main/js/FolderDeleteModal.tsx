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
        branch: revision || ""
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
