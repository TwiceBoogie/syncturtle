#!/bin/sh

set -eu

repository_root=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
environment_dir="$repository_root/development/environment"
secret_dir="$repository_root/development/secrets"

root_environment="$repository_root/.env"
host_nginx_environment="$environment_dir/.env.host-nginx"
host_direct_environment="$environment_dir/.env.host-direct"

private_key="$secret_dir/jwt-private-key.pem"
public_key="$secret_dir/jwt-public-key.pem"
public_key_der_temporary="$secret_dir/.jwt-public-key.der.$"
private_public_key_der_temporary="$secret_dir/.jwt-private-public-key.der.$"
public_key_digest_temporary="$secret_dir/.jwt-public-key.digest.$"
private_key_digest_temporary="$secret_dir/.jwt-private-public-key.digest.$"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

generate_secret() {
  openssl rand -hex 32
}

generate_base64url_secret() {
  openssl rand -base64 32 \
    | tr '+/' '-_' \
    | tr -d '=\n'
}

cleanup_temporary_key_files() {
  rm -f \
    "$public_key_der_temporary" \
    "$private_public_key_der_temporary" \
    "$public_key_digest_temporary" \
    "$private_key_digest_temporary"
}

escape_sed_replacement() {
  printf '%s' "$1" | sed 's/[&|\\]/\\&/g'
}

replace_marker() {
  replacement_file=$1
  replacement_marker=$2
  replacement_value=$3
  replacement_temporary_file="$replacement_file.tmp.$$"

  escaped_replacement=$(escape_sed_replacement "$replacement_value")

  sed \
    "s|__${replacement_marker}__|${escaped_replacement}|g" \
    "$replacement_file" \
    > "$replacement_temporary_file"

  mv "$replacement_temporary_file" "$replacement_file"
  chmod 600 "$replacement_file"
}

read_value() {
  value_file=$1
  value_key=$2

  sed -n "s/^${value_key}=//p" "$value_file"
}

copy_template_once() {
  copy_template=$1
  copy_destination=$2

  if [ -e "$copy_destination" ]; then
    echo "Preserving existing file: $copy_destination"
    return
  fi

  cp "$copy_template" "$copy_destination"
  chmod 600 "$copy_destination"

  echo "Created: $copy_destination"
}

ensure_generated_value() {
  value_file=$1
  value_key=$2
  generation_marker=$3
  expected_value=$4

  if grep -q "__${generation_marker}__" "$value_file"; then
    replace_marker \
      "$value_file" \
      "$generation_marker" \
      "$expected_value"

    return
  fi

  existing_value=$(read_value "$value_file" "$value_key")

  if [ -z "$existing_value" ]; then
    echo "Missing required value in $value_file: $value_key" >&2
    exit 1
  fi

  if [ "$existing_value" != "$expected_value" ]; then
    echo "$value_key does not match the generated installation value in $value_file" >&2
    echo "Refusing to silently replace existing JWT key metadata." >&2
    exit 1
  fi
}

derive_der_key_kid() {
  der_file=$1
  digest_file=$2

  if ! openssl dgst \
    -sha256 \
    -binary \
    -out "$digest_file" \
    "$der_file"; then
    echo "Failed to hash DER-encoded JWT public key." >&2
    return 1
  fi

  if ! encoded_digest=$(openssl base64 -A -in "$digest_file"); then
    echo "Failed to base64-encode JWT public-key digest." >&2
    return 1
  fi

  printf '%s' "$encoded_digest" \
    | tr '+/' '-_' \
    | tr -d '=\n'
}

derive_public_key_kid() {
  public_key_file=$1

  if ! openssl pkey \
    -pubin \
    -pubout \
    -in "$public_key_file" \
    -outform DER \
    -out "$public_key_der_temporary"; then
    echo "Failed to encode JWT public key as SubjectPublicKeyInfo DER." >&2
    return 1
  fi

  derive_der_key_kid \
    "$public_key_der_temporary" \
    "$public_key_digest_temporary"
}

derive_private_key_kid() {
  private_key_file=$1

  if ! openssl pkey \
    -in "$private_key_file" \
    -pubout \
    -outform DER \
    -out "$private_public_key_der_temporary"; then
    echo "Failed to derive JWT public key from private key." >&2
    return 1
  fi

  derive_der_key_kid \
    "$private_public_key_der_temporary" \
    "$private_key_digest_temporary"
}

require_command openssl
require_command sed
require_command grep
require_command tr
require_command rm

umask 077
trap cleanup_temporary_key_files 0 1 2 15

mkdir -p "$environment_dir" "$secret_dir"

copy_template_once \
  "$repository_root/.env.example" \
  "$root_environment"

copy_template_once \
  "$environment_dir/.env.host-nginx.example" \
  "$host_nginx_environment"

copy_template_once \
  "$environment_dir/.env.host-direct.example" \
  "$host_direct_environment"

markers="
GRAFANA_ADMIN_PASSWORD
POSTGRES_ROOT_PASSWORD
INSTANCE_DB_PASSWORD
INSTANCE_MIGRATOR_DB_PASSWORD
USER_DB_PASSWORD
USER_MIGRATOR_DB_PASSWORD
WORKSPACE_DB_PASSWORD
WORKSPACE_MIGRATOR_DB_PASSWORD
FILE_DB_PASSWORD
FILE_MIGRATOR_DB_PASSWORD
EMAIL_DB_PASSWORD
EMAIL_MIGRATOR_DB_PASSWORD
REDIS_PASSWORD
MINIO_ROOT_PASSWORD
APP_STORAGE_SECRET_KEY
APP_SECURITY_ENCRYPTION_SECRET_KEY
APP_SECURITY_CSRF_SIGNING_KEY
"

for marker in $markers; do
  if grep -q "__GENERATE_${marker}__" "$root_environment"; then
    if [ "$marker" = "APP_SECURITY_CSRF_SIGNING_KEY" ]; then
      value=$(generate_base64url_secret)
    else
      value=$(generate_secret)
    fi

    replace_marker \
      "$root_environment" \
      "GENERATE_$marker" \
      "$value"
  else
    value=$(read_value "$root_environment" "$marker")

    if [ -z "$value" ]; then
      echo "Missing required value in $root_environment: $marker" >&2
      exit 1
    fi
  fi

  replace_marker \
    "$host_nginx_environment" \
    "GENERATE_$marker" \
    "$value"

  replace_marker \
    "$host_direct_environment" \
    "GENERATE_$marker" \
    "$value"

  if grep -q "^${marker}=" "$host_nginx_environment"; then
    host_nginx_value=$(read_value "$host_nginx_environment" "$marker")
    host_direct_value=$(read_value "$host_direct_environment" "$marker")

    if [ "$host_nginx_value" != "$value" ] \
      || [ "$host_direct_value" != "$value" ]; then
      echo "Generated environment files disagree for shared value: $marker" >&2
      echo "Existing files were preserved; reconcile only the stale generated file." >&2
      exit 1
    fi
  fi
done

if [ ! -e "$private_key" ] && [ ! -e "$public_key" ]; then
  openssl genpkey \
    -algorithm RSA \
    -pkeyopt rsa_keygen_bits:2048 \
    -out "$private_key"

  openssl pkey \
    -in "$private_key" \
    -pubout \
    -out "$public_key"

  chmod 600 "$private_key" "$public_key"

  echo "Created local JWT key pair under: $secret_dir"
elif [ ! -e "$private_key" ] || [ ! -e "$public_key" ]; then
  echo "Refusing to replace a partial JWT key pair in $secret_dir" >&2
  exit 1
else
  echo "Preserving existing JWT key pair under: $secret_dir"
fi

if ! openssl pkey -pubin -pubcheck -noout -in "$public_key" >/dev/null; then
  echo "JWT public key failed validation." >&2
  exit 1
fi

if ! openssl pkey -check -noout -in "$private_key" >/dev/null; then
  echo "JWT private key failed validation." >&2
  exit 1
fi

public_key_kid=$(derive_public_key_kid "$public_key")
private_key_kid=$(derive_private_key_kid "$private_key")

if [ "$public_key_kid" != "$private_key_kid" ]; then
  echo "JWT private and public keys do not belong to the same key pair." >&2
  echo "Refusing to continue with inconsistent JWT key material." >&2
  exit 1
fi

passport_kid="$public_key_kid"

ensure_generated_value \
  "$root_environment" \
  "APP_PASSPORT_KID" \
  "GENERATE_APP_PASSPORT_KID" \
  "$passport_kid"

ensure_generated_value \
  "$host_nginx_environment" \
  "APP_PASSPORT_KID" \
  "GENERATE_APP_PASSPORT_KID" \
  "$passport_kid"

ensure_generated_value \
  "$host_direct_environment" \
  "APP_PASSPORT_KID" \
  "GENERATE_APP_PASSPORT_KID" \
  "$passport_kid"

host_private_key_location="file:$private_key"
host_public_key_location="file:$public_key"

ensure_generated_value \
  "$host_nginx_environment" \
  "APP_PASSPORT_PRIVATE_KEY_LOCATION" \
  "GENERATE_APP_PASSPORT_PRIVATE_KEY_LOCATION" \
  "$host_private_key_location"

ensure_generated_value \
  "$host_nginx_environment" \
  "APP_PASSPORT_PUBLIC_KEY_LOCATION" \
  "GENERATE_APP_PASSPORT_PUBLIC_KEY_LOCATION" \
  "$host_public_key_location"

ensure_generated_value \
  "$host_direct_environment" \
  "APP_PASSPORT_PRIVATE_KEY_LOCATION" \
  "GENERATE_APP_PASSPORT_PRIVATE_KEY_LOCATION" \
  "$host_private_key_location"

ensure_generated_value \
  "$host_direct_environment" \
  "APP_PASSPORT_PUBLIC_KEY_LOCATION" \
  "GENERATE_APP_PASSPORT_PUBLIC_KEY_LOCATION" \
  "$host_public_key_location"

if grep -R \
  '__GENERATE_' \
  "$root_environment" \
  "$host_nginx_environment" \
  "$host_direct_environment" \
  >/dev/null 2>&1; then
  echo "One or more generated environment files still contain unresolved markers." >&2
  exit 1
fi

echo "Local environment initialization complete."
echo "Generated environment and JWT key files are intentionally Git-ignored."
