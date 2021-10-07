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
import { File, Repository, Link } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import FolderCreateModal from "./FolderCreateModal";
import { CreateFolderDto } from "./types";
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

const FolderCreateButton: FC<Props> = ({ sources, path, revision }) => {
  const [t] = useTranslation("plugins");
  const [creationModalVisible, setCreationModalVisible] = useState(false);

  if (!sources || !("createFolder" in sources._links)) {
    return null;
  }

  const submit = ({ name, message }: { name: string; message: string }) => {
    const createLink = (sources._links.createFolder as Link).href.replace("{path}", path || "");
    const payload: CreateFolderDto = {
      folderName: name,
      commitMessage: message,
      branch: revision || ""
    };
    apiClient
      .post(createLink, payload)
      .then(() => setCreationModalVisible(false))
      .catch(console.log);
  };
  return (
    <>
      {creationModalVisible ? (
        <FolderCreateModal
          sources={sources}
          path={path}
          revision={revision}
          onClose={() => setCreationModalVisible(false)}
          onSubmit={submit}
        />
      ) : null}
      <Button
        className="button"
        title={t("scm-manage-folder-plugin.create.tooltip")}
        onClick={() => setCreationModalVisible(true)}
      >
        <i className="fas fa-folder-plus" />
      </Button>
    </>
  );
};

export default FolderCreateButton;
