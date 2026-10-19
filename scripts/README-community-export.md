# Community source export

The private repository is canonical. `bash scripts/export-community.sh /tmp/syncturtle-community-export` builds a Community source tree. The script excludes every `apps/**/ee/` directory from its initial copy, generates public EE facades, verifies the exact EE file set and contents, then replaces the requested output directory. It also excludes the old `ee-community` hierarchy. The GitHub publishing workflow runs the same export, checks the copied public checkout again, and only then commits and opens a PR.

An `index.ts` or `index.tsx` below an app's `ee/` directory is an Enterprise entry point. Every such file must declare exactly one Community disposition anywhere in the file:

```ts
// @community-export reexport "@/syncturtle-web/components/app-rail"
```

This generates an `index.ts` at the same relative path containing only:

```ts
export * from "@/syncturtle-web/components/app-rail";
```

Or use:

```ts
// @community-export omit
```

This generates no file. `omit` is appropriate only when no shared or Community code imports that EE entry point. The private implementation stays in its `ee/` directory; it is never copied into the export. The generator reads the private entry point only for its directive and never transforms its TypeScript. Web re-export targets must use `@/syncturtle-web/...`; Admin targets must use `@/syncturtle-admin/...`. Both are quoted, app-specific aliases with simple path segments. Direct `ce/...`, `ee/...`, `@/...` core paths, traversal, and package names are rejected. The generator fails on missing, malformed, unsupported, or multiple directives, two entry points in one directory, and EE files without an ancestor entry point. Any unexpected EE file or changed facade content fails verification.

In Web, `@/*` selects edition-independent `core/*`, `@/syncturtle-web/*` explicitly selects `ce/*`, and `ee/*` selects the edition boundary. Admin has the corresponding `@/syncturtle-admin/*` CE alias. There is no direct `ce/*` import alias. `ce/` holds real Community Edition implementations, including complete features. A no-op component is a feature-specific choice, not a property of CE. The export tooling never rewrites CE files. Shared code can import an EE entry point such as `ee/components/app-rail` in both editions; the private entry point exports its implementation, while the Community facade re-exports CE behavior. `export *` carries named exports but does not carry a default export.

Each EE `index.ts` or `index.tsx` is an independent entry point, including a parent barrel and a nested child barrel. Each declares its own disposition based on the API it represents; the generator never inherits a child's directive for its parent or vice versa. A non-index EE implementation file must have an ancestor entry point, but it is never generated. Shared code must import the declared EE entry point rather than a private implementation file beneath it. CE barrels are normal TypeScript modules and have no directive or file-name restrictions.

The current backend has no `ee/` or `ce/` source boundary. The copy and verification rules cover any directory named `ee` beneath `apps/`, including a future backend boundary. This generator recognizes TypeScript `index.ts` and `index.tsx` entry points only; a future Java or other backend EE design needs an explicit generated-file contract before proprietary backend code is placed there.

Run focused tests with `node --test scripts/community-facades.test.mjs`. To verify an actual export during development, run the export command and then `node scripts/community-facades.mjs verify "$PWD" /tmp/syncturtle-community-export` from a repository checkout. Keep the output outside that checkout.
