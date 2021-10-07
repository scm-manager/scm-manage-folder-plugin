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
import { useTranslation } from "react-i18next";
import { Button, ButtonGroup, Modal, Textarea } from "@scm-manager/ui-components";

type Props = {
  onCommit: (p: string) => void;
  onClose: () => void;
  loading: boolean;
};

const FolderDeleteModal: FC<Props> = ({ onCommit, onClose, loading }) => {
  const [t] = useTranslation("plugins");
  const [commitMessage, setCommitMessage] = useState("");

  const body = (
    <>
      {/*<CommitAuthor />*/}
      <Textarea
        placeholder={t("scm-manage-folder-plugin.delete.commit.placeholder")}
        onChange={setCommitMessage}
        disabled={loading}
      />
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button
        className="is-marginless"
        label={t("scm-manage-folder-plugin.delete.cancel.label")}
        action={onClose}
        disabled={loading}
      />
      <Button
        className="is-marginless"
        label={t("scm-manage-folder-plugin.delete.submit.label")}
        color="primary"
        disabled={!commitMessage}
        loading={loading}
        action={() => onCommit(commitMessage)}
      />
    </ButtonGroup>
  );

  return (
    <Modal
      title={t("scm-manage-folder-plugin.delete.title")}
      closeFunction={() => onClose()}
      body={body}
      footer={footer}
      active={true}
    />
  );
};

export default FolderDeleteModal;
