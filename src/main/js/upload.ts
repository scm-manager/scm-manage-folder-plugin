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
