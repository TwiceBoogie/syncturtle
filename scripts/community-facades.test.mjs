import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import { checkPaths, generate, verify } from "./community-facades.mjs";

function fixture(t, appName = "web") {
  const root = mkdtempSync(join(tmpdir(), "syncturtle-facades-test-"));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const source = join(root, "source");
  const output = join(root, "output");
  mkdirSync(join(source, "apps", appName, "ee"), { recursive: true });
  mkdirSync(join(output, "apps", appName), { recursive: true });
  return { source, output };
}

function entry(source, folder, content, appName = "web") {
  const dir = join(source, "apps", appName, "ee", "components", folder);
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, "index.ts"), content);
  writeFileSync(join(dir, "root.tsx"), "private implementation\n");
  writeFileSync(join(dir, "items-root.tsx"), "private implementation\n");
}

test("reexport produces only a deterministic public facade and rejects leaked files", (t) => {
  const { source, output } = fixture(t);
  entry(
    source,
    "app-rail",
    '/** Enterprise entry point. */\n\n// @community-export reexport "@/syncturtle-web/components/app-rail"\n\nexport * from "./root";\nexport * from "./types";\n'
  );
  entry(source, "hidden", '// @community-export omit\nexport * from "./root";\n');
  const expected = 'export * from "@/syncturtle-web/components/app-rail";\n';
  assert.equal(generate(source, output).size, 1);
  const facade = join(output, "apps", "web", "ee", "components", "app-rail", "index.ts");
  assert.equal(readFileSync(facade, "utf8"), expected);
  assert.equal(verify(source, output), 1);
  writeFileSync(join(output, "apps", "web", "ee", "components", "app-rail", "root.tsx"), "leak");
  assert.throws(() => verify(source, output), /Unexpected.*root\.tsx/);
  rmSync(join(output, "apps", "web", "ee", "components", "app-rail", "root.tsx"));
  const nestedEe = join(output, "apps", "web", "build", "ee");
  mkdirSync(nestedEe, { recursive: true });
  writeFileSync(join(nestedEe, "internal.ts"), "leak");
  assert.throws(() => verify(source, output), /Unexpected.*build.*ee.*internal\.ts/);
  rmSync(join(output, "apps", "web", "build"), { recursive: true });
  writeFileSync(facade, "altered");
  assert.throws(() => verify(source, output), /content mismatch/);
});

test("admin uses its own Community alias", (t) => {
  const { source, output } = fixture(t, "admin");
  entry(
    source,
    "authentication",
    '// @community-export reexport "@/syncturtle-admin/components/authentication"\n',
    "admin"
  );
  assert.equal(generate(source, output).size, 1);
  assert.equal(
    readFileSync(join(output, "apps", "admin", "ee", "components", "authentication", "index.ts"), "utf8"),
    'export * from "@/syncturtle-admin/components/authentication";\n'
  );
  assert.equal(verify(source, output), 1);
  const other = fixture(t, "admin");
  entry(other.source, "wrong", '// @community-export reexport "@/syncturtle-web/components/wrong"\n', "admin");
  assert.throws(() => generate(other.source, other.output), /Invalid/);
});

test("missing, invalid, duplicate, and unsafe directives fail closed", (t) => {
  const invalid = [
    ["missing", 'export * from "./root";\n', /does not declare/],
    ["unsupported", "// @community-export copy\n", /Invalid/],
    ["malformed", "// @community-export reexport ce/components/foo\n", /Invalid/],
    [
      "duplicate",
      '// @community-export omit\n// @community-export reexport "@/syncturtle-web/components/foo"\n',
      /Multiple/,
    ],
    ["traversal", '// @community-export reexport "../../../../outside"\n', /Invalid/],
    ["absolute", '// @community-export reexport "/tmp/outside"\n', /Invalid/],
    ["legacy", '// @community-export reexport "ce/components/foo"\n', /Invalid/],
    ["ee", '// @community-export reexport "ee/components/foo"\n', /Invalid/],
    ["core", '// @community-export reexport "@/components/foo"\n', /Invalid/],
    ["package", '// @community-export reexport "random-package/foo"\n', /Invalid/],
    ["malformed-alias", '// @community-export reexport "@/syncturtle-web/../foo"\n', /Invalid/],
    ["wrong-app", '// @community-export reexport "@/syncturtle-admin/components/foo"\n', /Invalid/],
  ];
  for (const [name, contents, error] of invalid) {
    const { source, output } = fixture(t);
    entry(source, name, contents);
    assert.throws(() => generate(source, output), error);
  }
});

test("parent and child EE entry points declare independent dispositions", (t) => {
  const { source, output } = fixture(t);
  const components = join(source, "apps", "web", "ee", "components");
  mkdirSync(components, { recursive: true });
  writeFileSync(
    join(components, "index.ts"),
    '// @community-export reexport "@/syncturtle-web/components/app-rail"\nexport * from "./root";\n'
  );
  writeFileSync(join(components, "root.tsx"), "private parent implementation\n");
  entry(source, "other-feature", '// @community-export omit\nexport * from "./root";\n');
  assert.equal(generate(source, output).size, 1);
  assert.equal(
    readFileSync(join(output, "apps", "web", "ee", "components", "index.ts"), "utf8"),
    'export * from "@/syncturtle-web/components/app-rail";\n'
  );
  assert.equal(verify(source, output), 1);
});

test("EE implementation without an entry point fails instead of disappearing silently", (t) => {
  const { source, output } = fixture(t);
  const orphan = join(source, "apps", "web", "ee", "components", "orphan");
  mkdirSync(orphan, { recursive: true });
  writeFileSync(join(orphan, "root.tsx"), "private implementation\n");
  assert.throws(() => generate(source, output), /no declared entry point/);
});

test("output path cannot overlap source; repeat generation is identical", (t) => {
  const { source, output } = fixture(t);
  entry(source, "one", '// @community-export reexport "@/syncturtle-web/components/one"\n');
  assert.throws(() => checkPaths(source, join(source, "export")), /outside/);
  assert.throws(() => checkPaths(source, join(source, "apps")), /outside/);
  generate(source, output);
  const first = readFileSync(join(output, "apps", "web", "ee", "components", "one", "index.ts"), "utf8");
  rmSync(join(output, "apps", "web", "ee"), { recursive: true });
  generate(source, output);
  const second = readFileSync(join(output, "apps", "web", "ee", "components", "one", "index.ts"), "utf8");
  assert.equal(first, second);
});
