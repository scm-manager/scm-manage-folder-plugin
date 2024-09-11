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

import React, { SetStateAction } from "react";
import { Radio } from "@scm-manager/ui-components";
import { droppedItemHierarchyProber } from "./upload";

const DIR_UPLOAD = "directory";

const createUploadExtension = () => ({
  renderOption: (uploadMode: string, setUploadMode: SetStateAction<any>, t: any) => (
    <>
      <Radio className="ml-2" checked={uploadMode === DIR_UPLOAD} onChange={() => setUploadMode(DIR_UPLOAD)} />
      {t("scm-manage-folder-plugin.upload.directory")}
      {uploadMode === DIR_UPLOAD ? (
        <>
          <br />
          <br />
          <span>{t("scm-manage-folder-plugin.upload.dirDescription")}</span>
        </>
      ) : null}
    </>
  ),
  dropZoneOptions: (fileHandler: any) => ({
    getFilesFromEvent: (event: any) => droppedItemHierarchyProber(event, fileHandler),
    noClick: true
  }),
  uploadMode: DIR_UPLOAD
});
export default createUploadExtension;
