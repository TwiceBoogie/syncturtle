#!/usr/bin/env bash
set -euo pipefail

OUT="${1:-dist/community}"

rm -rf "$OUT"
mkdir -p "$OUT"

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
    --exclude "apps/**/ee/**" \
    --exclude ".github/workflows/**" \
    --exclude ".github/dependabot.yml" \
    ./ "$OUT/"

#2: replace EE with public safe stubs
if [ -d "apps/web/ee-community" ]; then
    mkdir -p "$OUT/apps/web/ee"
    rsync -a --delete "apps/web/ee-community/" "$OUT/apps/web/ee/"
else
    rm -rf "$OUT/apps/web/ee"
    mkdir -p "$OUT/apps/web/ee"
    printf "Community build: EE stubs live here.\n" > "$OUT/apps/web/ee/README.md"
fi

if [ -d "apps/admin/ee-community" ]; then
    mkdir -p "$OUT/apps/admin/ee"
    rsync -a --delete "apps/admin/ee-community/" "$OUT/apps/admin/ee/"
else
    rm -rf "$OUT/apps/admin/ee"
    mkdir -p "$OUT/apps/admin/ee"
    printf "Community build: EE stubs live here.\n" > "$OUT/apps/admin/ee/README.md"
fi  
    