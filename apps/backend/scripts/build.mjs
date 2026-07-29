import { readFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { resolve } from "node:path";

const rootPackageJsonPath = resolve("../../package.json");
const rootPackageJson = JSON.parse(readFileSync(rootPackageJsonPath, "utf8"));

const revision = rootPackageJson.version;

console.log(`[backend] building Maven reactor with revision=${revision}`);

execFileSync(
  "./mvnw",
  ["-f", "pom.xml", `-Drevision=${revision}`, "-DskipTests", "package"],
  {
    stdio: "inherit",
  },
);