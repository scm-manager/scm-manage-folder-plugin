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
import { File, Link, Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import FolderDeleteModal from "./FolderDeleteModal";
import { CommitDto } from "./types";
import { apiClient } from "@scm-manager/ui-components";

const Button = styled.span`
  width: 50px;
  color: #33b2e8;
  &:hover {
    color: #363636;
  }
`;

type Props = {
  repository: Repository;
  revision?: string;
  path?: string;
  sources: File;
};

const FolderDeleteButton: FC<Props> = ({ sources, path, revision }) => {
  const [t] = useTranslation("plugins");
  const [modalVisible, setModalVisible] = useState(false);

  if (!sources || !("deleteFolder" in sources._links)) {
    return null;
  }

  const submit = (message: string) => {
    const createLink = (sources._links.deleteFolder as Link).href;
    const payload: CommitDto = {
      commitMessage: message,
      branch: revision || ""
      // expectedRevision: sources.revision
    };
    apiClient
      .post(createLink, payload)
      .then(() => setModalVisible(false))
      .catch(console.log);
  };

  return (
    <>
      {modalVisible ? (
        <FolderDeleteModal onCommit={submit} onClose={() => setModalVisible(false)} loading={false} />
      ) : null}
      <Button
        className="button"
        title={t("scm-manage-folder-plugin.delete.tooltip")}
        onClick={() => setModalVisible(true)}
      >
        <i className="fas fa-trash" />
      </Button>
    </>
  );
};

export default FolderDeleteButton;
