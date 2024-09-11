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

import { Changeset, Repository, Branch } from "@scm-manager/ui-types";
import { createRedirectUrl } from "./createRedirectUrl";

describe("Redirect Url Tests", () => {
  const repository: Repository = {
    namespace: "scm",
    name: "core",
    type: "git",
    _links: {}
  };

  const changeset: Changeset = {
    id: "42",
    description: "Awesome change",
    date: new Date(),
    author: {
      name: "Arthur Dent"
    },
    _embedded: {},
    _links: {}
  };

  it("should encode path with percent sign", () => {
    expect(createRedirectUrl(repository, changeset, "beforePercent%afterSign")).toBe(
      "/repo/scm/core/code/sources/42/beforePercent%25afterSign/"
    );
  });

  it("should remove start and ending slash from path", () => {
    expect(createRedirectUrl(repository, changeset, "/path/")).toBe(
      "/repo/scm/core/code/sources/42/path/"
    );
  });

  it("should keep slashes in path", () => {
    expect(createRedirectUrl(repository, changeset, "a/b/c")).toBe(
      "/repo/scm/core/code/sources/42/a%2Fb%2Fc/"
    );
  });

  it("should return only one slash for empty path", () => {
    expect(createRedirectUrl(repository, changeset, "")).toBe(
      "/repo/scm/core/code/sources/42/"
    );
  })

  it("should encode branch with slash", () => {
    const branchWithSlash: Branch = {
      name: "feature/awesome",
      revision: "12",
      lastCommitter: { name: "trillian" },
      _links: {}
    };

    changeset._embedded = {
      branches: [branchWithSlash]
    };

    expect(createRedirectUrl(repository, changeset, "path")).toBe(
      "/repo/scm/core/code/sources/feature%2Fawesome/path/"
    );
  });
});
