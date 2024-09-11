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

import { fromEvent } from "file-selector";

export async function droppedItemHierarchyProber(e: any, fileHandler: any) {
  const filesDataPromise = fromEvent(e);
  const hierarchyDetails = await probeFolders(e);

  const filesData = await filesDataPromise;

  fileHandler(filesData);
  for (const folder of hierarchyDetails.emptyFolders) {
    let file = new File([""], ".scmkeep", { type: "text/plain" });
    file.path = folder.path + "/" + file.name;
    fileHandler(file);
  }
  return { filesData, hierarchyDetails };
}

async function probeFolders(event: any) {
  const hierarchyDetails = {
    emptyFolders: [],
    allFolders: [],
    files: []
  };

  if (!event.dataTransfer.items[0].getAsFileSystemHandle || !(event.dataTransfer.items[0].getAsFileSystemHandle instanceof Function)) {
    // Feature not supported therefore abort
    return hierarchyDetails;
  }

  const rootHandle = await event.dataTransfer.items[0].getAsFileSystemHandle();
  const path = `/${rootHandle.name}`;
  if (rootHandle.kind === "directory") {
    await traverseDirectory(rootHandle, path, hierarchyDetails);
  } else if (rootHandle.kind === "file") {
    const file = { name: rootHandle.name, kind: rootHandle.kind, path };
    hierarchyDetails.files.push(file);
  }

  return hierarchyDetails;
}

async function traverseDirectory(dirHandle, currentPath, hierarchyDetails) {
  const folderDetails = {
    name: dirHandle.name,
    kind: dirHandle.kind,
    path: currentPath,
    children: []
  };

  for await (const [name, handle] of dirHandle.entries()) {
    const path = `${currentPath}/${name}`;

    if (handle.kind === "file") {
      const file = { path, name: handle.name, kind: handle.kind };
      hierarchyDetails.files.push(file);
      folderDetails.children.push(file);
    } else if (handle.kind === "directory") {
      const childDetails = await traverseDirectory(handle, path, hierarchyDetails); //

      if (childDetails.children.length === 0) {
        hierarchyDetails.emptyFolders.push(childDetails);
      }

      folderDetails.children.push(childDetails);
    }
  }

  hierarchyDetails.allFolders.push(folderDetails);
  return folderDetails;
}
