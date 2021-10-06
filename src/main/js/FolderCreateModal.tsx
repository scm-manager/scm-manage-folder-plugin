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
import { ButtonGroup, InputField, Modal, Textarea, Button, CommitAuthor } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { File } from "@scm-manager/ui-types";

type Result = {
  name: string;
  message: string;
};

type Props = {
  revision?: string;
  path?: string;
  sources: File;
  onClose: () => void;
  loading: boolean;
  onSubmit: (result: Result) => void;
};

const FolderCreateModal: FC<Props> = ({ sources, revision, onClose, onSubmit, loading }) => {
  const [t] = useTranslation("plugins");
  const [folderName, setFolderName] = useState("");
  const [commitMessage, setCommitMessage] = useState("");

  const body = (
    <>
      <InputField label={t("scm-manage-folder-plugin.create.branch.label")} value={revision} disabled={true} />
      <InputField
        label={t("scm-manage-folder-plugin.create.path.label")}
        value={sources.path === "/" ? "/" : "/" + sources.path}
        disabled={true}
      />
      <InputField
        label={t("scm-manage-folder-plugin.create.name.label")}
        value={folderName}
        onChange={setFolderName}
        disabled={loading}
      />
      <CommitAuthor />
      <Textarea
        placeholder={t("scm-manage-folder-plugin.create.commit.placeholder")}
        onChange={message => setCommitMessage(message)}
        value={commitMessage}
        disabled={loading}
      />
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button className="is-marginless" action={onClose} disabled={loading}>
        {t("scm-manage-folder-plugin.create.cancel.label")}
      </Button>
      <Button
        className="is-marginless"
        action={() => onSubmit({ message: commitMessage, name: folderName })}
        loading={loading}
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
