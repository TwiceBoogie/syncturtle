#!/usr/bin/env node
import { existsSync, lstatSync, mkdirSync, readFileSync, readdirSync, realpathSync, writeFileSync } from "node:fs";
import { dirname, isAbsolute, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const directiveMarker = /\/\/\s*@community-export\b/;
const reexportDirective = /^\s*\/\/\s*@community-export reexport "([^"]+)"\s*$/;
const omitDirective = /^\s*\/\/\s*@community-export omit\s*$/;
const entryPointName = /^index\.tsx?$/;
const communityTargets = new Map([
  ["web", /^@\/syncturtle-web\/[a-zA-Z0-9_-]+(?:\/[a-zA-Z0-9_-]+)*$/],
  ["admin", /^@\/syncturtle-admin\/[a-zA-Z0-9_-]+(?:\/[a-zA-Z0-9_-]+)*$/],
]);

function fail(message) {
  throw new Error(message);
}

function inside(parent, child) {
  const path = relative(parent, child);
  return path === "" || (path !== ".." && !path.startsWith(`..${sep}`) && !isAbsolute(path));
}

function existingAncestor(path) {
  let current = path;
  while (!existsSync(current)) current = dirname(current);
  return realpathSync(current);
}

export function checkPaths(source, output) {
  const sourcePath = realpathSync(source);
  const outputPath = resolve(output);
  const outputAncestor = existingAncestor(outputPath);
  if (inside(sourcePath, outputPath) || inside(outputPath, sourcePath) || inside(sourcePath, outputAncestor)) {
    fail(`Community output must be outside the source checkout: ${outputPath}`);
  }
  if (existsSync(outputPath) && lstatSync(outputPath).isSymbolicLink()) {
    fail(`Community output must not be a symlink: ${outputPath}`);
  }
}

function walk(dir, visit) {
  for (const item of readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name, "en"))) {
    const path = join(dir, item.name);
    if (item.isSymbolicLink()) fail(`Symlink inside EE tree is forbidden: ${path}`);
    if (item.isDirectory()) walk(path, visit);
    else if (item.isFile()) visit(path);
    else fail(`Unsupported file inside EE tree: ${path}`);
  }
}

function disposition(file, appName) {
  const directives = readFileSync(file, "utf8")
    .split(/\r?\n/)
    .filter((line) => directiveMarker.test(line));
  if (directives.length === 0) {
    fail(
      `Enterprise entry point does not declare Community export behavior: ${file}\nExpected // @community-export reexport "<approved Community alias>/..." or // @community-export omit`
    );
  }
  if (directives.length !== 1) fail(`Multiple Community export directives: ${file}`);
  if (omitDirective.test(directives[0])) return null;
  const match = reexportDirective.exec(directives[0]);
  if (!match || !communityTargets.get(appName)?.test(match[1])) {
    fail(`Invalid Community export directive for ${appName}: ${file}: ${directives[0].trim()}`);
  }
  return `export * from "${match[1]}";\n`;
}

export function expectedFacades(source) {
  const expected = new Map();
  const apps = join(source, "apps");
  for (const ee of eeDirectories(apps, true)) {
    const appName = relative(apps, ee).split(sep)[0];
    const files = [];
    walk(ee, (file) => files.push(file));
    const entries = files.filter((file) => entryPointName.test(file.slice(file.lastIndexOf(sep) + 1)));
    const entryDirs = new Set();
    for (const file of entries) {
      const directory = dirname(file);
      if (entryDirs.has(directory)) fail(`Multiple Enterprise entry points in one directory: ${directory}`);
      entryDirs.add(directory);
      const content = disposition(file, appName);
      if (content !== null) expected.set(relative(source, file), content);
    }
    for (const file of files) {
      if (entries.includes(file)) continue;
      let directory = dirname(file);
      while (inside(ee, directory) && !entryDirs.has(directory)) directory = dirname(directory);
      if (!entryDirs.has(directory)) fail(`EE file has no declared entry point: ${file}`);
    }
  }
  return expected;
}

function eeDirectories(apps, ignoreBuildArtifacts = false) {
  const found = [];
  const ignored = new Set(["node_modules", ".next", ".turbo", ".git", ".yarn", "target", "dist", "build"]);
  function visit(dir) {
    for (const item of readdirSync(dir, { withFileTypes: true })) {
      if (ignoreBuildArtifacts && ignored.has(item.name)) continue;
      const path = join(dir, item.name);
      if (item.name === "ee-community") fail(`Obsolete ee-community directory must be removed: ${path}`);
      if (item.name === "ee") {
        if (!item.isDirectory()) fail(`EE boundary must be a directory: ${path}`);
        found.push(path);
      } else if (item.isDirectory()) {
        visit(path);
      }
    }
  }
  visit(apps);
  return found.sort();
}

function eeFiles(output) {
  const actual = new Set();
  const apps = join(output, "apps");
  if (!existsSync(apps)) fail(`Missing apps directory in Community output: ${apps}`);
  for (const ee of eeDirectories(apps)) walk(ee, (file) => actual.add(relative(output, file)));
  return actual;
}

export function generate(source, output) {
  checkPaths(source, output);
  const expected = expectedFacades(source);
  const existing = eeFiles(output);
  if (existing.size) fail(`Community staging tree already contains EE files: ${[...existing].sort().join(", ")}`);
  for (const [path, content] of expected) {
    const destination = join(output, path);
    mkdirSync(dirname(destination), { recursive: true });
    writeFileSync(destination, content, { flag: "wx" });
  }
  return expected;
}

export function verify(source, output) {
  const expected = expectedFacades(source);
  const actual = eeFiles(output);
  const missing = [...expected.keys()].filter((path) => !actual.has(path));
  const unexpected = [...actual].filter((path) => !expected.has(path));
  if (missing.length || unexpected.length) {
    fail(
      `Community EE file mismatch. Missing: ${missing.sort().join(", ") || "none"}. Unexpected: ${unexpected.sort().join(", ") || "none"}.`
    );
  }
  for (const [path, content] of expected) {
    if (readFileSync(join(output, path), "utf8") !== content) fail(`Community EE facade content mismatch: ${path}`);
  }
  return expected.size;
}

if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const [command, source, output] = process.argv.slice(2);
    if (!source || !output || !["check-paths", "generate", "verify"].includes(command)) {
      fail("Usage: node scripts/community-facades.mjs <check-paths|generate|verify> <source-root> <output-root>");
    }
    if (command === "check-paths") checkPaths(source, output);
    if (command === "generate") console.log(`Generated ${generate(source, output).size} Community EE facade(s).`);
    if (command === "verify") console.log(`Verified ${verify(source, output)} Community EE facade(s).`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
