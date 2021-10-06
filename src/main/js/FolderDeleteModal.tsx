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
import {
  apiClient,
  Button,
  ButtonGroup,
  CommitAuthor,
  ErrorNotification,
  Modal,
  Textarea
} from "@scm-manager/ui-components";
import { Changeset, File, Link, Repository } from "@scm-manager/ui-types/src/index";
import { useHistory, useLocation } from "react-router-dom";
import { CommitDto } from "./types";
import { createRedirectUrl } from "./createRedirectUrl";

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
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState();
  const history = useHistory();
  const location = useLocation();

  const submit = () => {
    const createLink = (sources._links.deleteFolder as Link).href;
    const payload: CommitDto = {
      commitMessage,
      branch: revision || ""
    };
    setLoading(true);
    apiClient
      .post(createLink, payload)
      .then(response => response.json())
      .then((newCommit: Changeset) => {
        const filePath = location.pathname
          .substr(0, location.pathname.length - sources.name.length - 1)
          .split("/sources/" + revision)[1];
        history.push(createRedirectUrl(repository, newCommit, filePath));
        onClose();
      })
      .catch(setError)
      .finally(() => setLoading(false));
  };

  const body = (
    <>
      {error ? <ErrorNotification error={error} /> : null}
      <div className="mb-3">
        <CommitAuthor />
      </div>
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
    />
  );
};

export default FolderDeleteModal;
