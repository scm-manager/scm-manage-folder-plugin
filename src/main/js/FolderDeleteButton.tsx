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
import FolderDeleteModal from "./FolderDeleteModal";
import { Button } from "@scm-manager/ui-components";
import { useDeleteFolder } from "./folders";

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

const FolderDeleteButton: FC<Props> = ({ sources, revision, repository }) => {
  const [t] = useTranslation("plugins");
  const [modalVisible, setModalVisible] = useState(false);
  const hook = useDeleteFolder();

  if (!sources || !("deleteFolder" in sources._links)) {
    return null;
  }

  return (
    <>
      {modalVisible ? (
        <FolderDeleteModal
          onClose={() => setModalVisible(false)}
          repository={repository}
          sources={sources}
          revision={revision}
          hook={hook}
        />
      ) : null}
      <StyledButton
        title={t("scm-manage-folder-plugin.delete.tooltip")}
        action={() => setModalVisible(true)}
        loading={hook.isLoading}
        disabled={hook.isLoading}
      >
        <i className="fas fa-trash" />
      </StyledButton>
    </>
  );
};

export default FolderDeleteButton;
