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

import React, { FC, useState } from "react";
import { File, Repository } from "@scm-manager/ui-types";
import { useTranslation } from "react-i18next";
import styled from "styled-components";
import FolderCreateModal from "./FolderCreateModal";
import { Button } from "@scm-manager/ui-components";
import { useCreateFolder } from "./folders";

const StyledButton = styled(Button)`
  width: 50px;
  &:hover {
    color: #33b2e8;
  }
`;

type Props = {
  repository: Repository;
  revision?: string;
  path?: string;
  sources: File;
};

const FolderCreateButton: FC<Props> = ({ sources, path, revision, repository }) => {
  const [t] = useTranslation("plugins");
  const [creationModalVisible, setCreationModalVisible] = useState(false);
  const hook = useCreateFolder();

  if (!sources || !("createFolder" in sources._links)) {
    return null;
  }

  return (
    <>
      {creationModalVisible ? (
        <FolderCreateModal
          repository={repository}
          sources={sources}
          path={path}
          revision={revision}
          onClose={() => setCreationModalVisible(false)}
          hook={hook}
        />
      ) : null}
      <StyledButton
        title={t("scm-manage-folder-plugin.create.tooltip")}
        action={() => setCreationModalVisible(true)}
        loading={hook.isLoading}
        disabled={hook.isLoading}
      >
        <i className="fas fa-folder-plus" />
      </StyledButton>
    </>
  );
};

export default FolderCreateButton;
