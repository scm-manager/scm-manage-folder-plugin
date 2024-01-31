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
