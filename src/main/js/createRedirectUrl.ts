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

import { Repository, Changeset } from "@scm-manager/ui-types";

export const getBranch = (changeset: Changeset) =>
  changeset._embedded?.branches?.[0]?.name
    ? changeset._embedded.branches[0].name
    : changeset.id;

export const createRedirectUrl = (repository: Repository, newCommit: Changeset, path?: string) => {
  const newRevision = getBranch(newCommit);

  // remove start and ending slash
  path = path?.startsWith("/") ? path.slice(1) : path;
  path = path?.endsWith("/") ? path.slice(0, -1) : path;

  return `/repo/${repository.namespace}/${repository.name}/code/sources/${encodeURIComponent(newRevision)}/${path ? encodeURIComponent(path) + "/" : ""}`;
};
