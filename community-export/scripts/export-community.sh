#!/usr/bin/env bash
set -euo pipefail

OUT="${1:-${RUNNER_TEMP:-/tmp}/syncturtle-community-export}"

# build temp folder outside repo so rsync never copies the output into itself
BUILD_DIR="$(mktemp -d "${RUNNER_TEMP:-/tmp}/syncturtle-community-build.XXXXXX")"

cleanup() {
    rm -rf "$BUILD_DIR"
}
trap cleanup EXIT

echo "Exporting community build..."
echo "Build dir: $BUILD_DIR"
echo "Output:    $OUT"

#1: copy everything
rsync -a --delete \
    --exclude ".git" \
    --exclude "node_modules" \
    --exclude ".next" \
    --exclude ".turbo" \
    --exclude ".yarn/cache" \
    --exclude ".env" \
    --exclude ".env.local" \
    --exclude ".env.*.*local" \
    --exclude "apps/backend/.env" \
    --exclude "apps/backend/.env.local" \
    --exclude "apps/backend/.env.*.local" \
    --exclude "apps/backend/.env.*.*local" \
    --exclude "apps/**/ee/**" \
    --exclude ".github/workflows/**" \
    --exclude ".github/dependabot.yml" \
    --exclude "/dist/" \
    --exclude "/public/" \
    ./ "$BUILD_DIR/"

#2: replace admin EE with public safe stubs
if [ -d "apps/admin/ee-community" ]; then
    rm -rf "$BUILD_DIR/apps/admin/ee"
    mkdir -p "$BUILD_DIR/apps/admin/ee"
    rsync -a --delete "apps/admin/ee-community/" "$BUILD_DIR/apps/admin/ee/"
else
    rm -rf "$BUILD_DIR/apps/admin/ee"
    mkdir -p "$BUILD_DIR/apps/admin/ee"
    cat > "$BUILD_DIR/apps/admin/ee/README.md" <<'EOF'
Community build: enterprise implementation files are replaced with public-safe stubs.
EOF
fi

#3: replace web EE with public safe stubs
if [ -d "apps/web/ee-community" ]; then
    rm -rf "$BUILD_DIR/apps/web/ee"
    mkdir -p "$BUILD_DIR/apps/web/ee"
    rsync -a --delete "apps/web/ee-community/" "$BUILD_DIR/apps/web/ee/"
else
    rm -rf "$BUILD_DIR/apps/web/ee"
    mkdir -p "$BUILD_DIR/apps/web/ee"
    cat > "$BUILD_DIR/apps/web/ee/README.md" <<'EOF'
Community build: enterprise implementation files are replaced with public-safe stubs.
EOF
fi

#4: move final export to requested OUT path.
rm -rf "$OUT"
mkdir -p "$(dirname "$OUT")"
mv "$BUILD_DIR" "$OUT"

# BUILD_DIR was moved, so don't delete OUT during cleanup.
trap - EXIT

echo "Community export complete: $OUT"
    