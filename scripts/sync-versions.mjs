// scripts/sync-versions.mjs
import fs from "node:fs";
import path from "node:path";

const ROOT = process.cwd();

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function writeJson(filePath, obj) {
  fs.writeFileSync(filePath, JSON.stringify(obj, null, 2) + "\n", "utf8");
}

function listWorkspacePackageJson(dir) {
  const abs = path.join(ROOT, dir);
  if (!fs.existsSync(abs)) return [];

  return fs
    .readdirSync(abs, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .map((d) => path.join(dir, d.name, "package.json"))
    .filter((rel) => fs.existsSync(path.join(ROOT, rel)));
}

function syncWorkspacePackageJsonVersions(version) {
  const targets = [
    ...listWorkspacePackageJson("apps"),
    ...listWorkspacePackageJson("packages"),
  ];

  let changed = 0;

  for (const rel of targets) {
    const abs = path.join(ROOT, rel);
    const pkg = readJson(abs);

    if (pkg.version !== version) {
      pkg.version = version;
      writeJson(abs, pkg);
      changed++;
      console.log(`synced ${rel} -> ${version}`);
    }
  }

  if (!changed)
    console.log("all workspace package.json versions already synced");
  return changed;
}

function syncBackendPomRevision(version) {
  const relPom = "apps/backend/pom.xml";
  const absPom = path.join(ROOT, relPom);

  if (!fs.existsSync(absPom)) {
    console.warn(`skipping: ${relPom} not found`);
    return 0;
  }

  const original = fs.readFileSync(absPom, "utf8");

  // Keep indentation, replace only the value inside the first <revision>...</revision>
  const re = /^(\s*<revision>)([^<]*)(<\/revision>)/m;

  if (!re.test(original)) {
    throw new Error(
      `Could not find <revision>...</revision> in ${relPom}. ` +
        `Make sure the root backend pom defines a <revision> property.`
    );
  }

  const updated = original.replace(re, `$1${version}$3`);

  if (updated !== original) {
    fs.writeFileSync(absPom, updated, "utf8");
    console.log(`synced ${relPom} <revision> -> ${version}`);
    return 1;
  }

  console.log(`${relPom} <revision> already ${version}`);
  return 0;
}

function main() {
  const rootPkgPath = path.join(ROOT, "package.json");
  if (!fs.existsSync(rootPkgPath)) {
    throw new Error("package.json not found at repo root");
  }

  const rootPkg = readJson(rootPkgPath);
  const version = rootPkg.version;

  if (!version || typeof version !== "string") {
    throw new Error("Root package.json is missing a valid version field");
  }

  // Release-style version should NOT end with -SNAPSHOT.
  // If you want to allow snapshots later, remove this guard.
  if (/-SNAPSHOT$/i.test(version)) {
    throw new Error(
      `Root package.json version is "${version}". ` +
        `For releases, set it to a non-SNAPSHOT version (e.g., 0.0.2).`
    );
  }

  let changes = 0;
  changes += syncWorkspacePackageJsonVersions(version);
  changes += syncBackendPomRevision(version);

  if (!changes) {
    console.log("sync complete: no changes needed");
  }
}

try {
  main();
} catch (err) {
  console.error(err?.message ?? err);
  process.exit(1);
}
