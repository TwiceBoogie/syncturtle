#!/usr/bin/env bash
set -euo pipefail

SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
OUT="${1:-${RUNNER_TEMP:-/tmp}/syncturtle-community-export}"
OUT="$(node -p 'require("path").resolve(process.argv[1])' "$OUT")"

node "$SOURCE_DIR/scripts/community-facades.mjs" check-paths "$SOURCE_DIR" "$OUT"

BUILD_DIR="$(mktemp -d "${RUNNER_TEMP:-/tmp}/syncturtle-community-build.XXXXXX")"
cleanup() {
    rm -rf "$BUILD_DIR"
}
trap cleanup EXIT

echo "Exporting Community source from: $SOURCE_DIR"
echo "Build dir: $BUILD_DIR"
echo "Output:    $OUT"

# EE directories are excluded before copying. Private implementation files
# never enter the staging tree.
rsync -a --delete \
    --exclude ".git" \
    --exclude "node_modules" \
    --exclude ".next" \
    --exclude ".turbo" \
    --exclude ".yarn/cache" \
    --exclude "target/" \
    --exclude "dist/" \
    --exclude "build/" \
    --include ".env.example" \
    --exclude ".env" \
    --exclude ".env.*" \
    --exclude "tmp-keys/" \
    --exclude "secrets/" \
    --exclude ".aws/" \
    --exclude "*.pem" \
    --exclude "*.key" \
    --exclude "*.p12" \
    --exclude "*.pfx" \
    --exclude "apps/**/ee/" \
    --exclude "apps/**/ee-community/" \
    --exclude ".github/workflows/**" \
    --exclude ".github/dependabot.yml" \
    --exclude "/public/" \
    "$SOURCE_DIR/" "$BUILD_DIR/"

node "$SOURCE_DIR/scripts/community-facades.mjs" generate "$SOURCE_DIR" "$BUILD_DIR"
node "$SOURCE_DIR/scripts/community-facades.mjs" verify "$SOURCE_DIR" "$BUILD_DIR"

rm -rf "$OUT"
mkdir -p "$(dirname "$OUT")"
mv "$BUILD_DIR" "$OUT"
trap - EXIT

echo "Community export complete: $OUT"
