#!/bin/sh

set -eu

: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${APP_STORAGE_ACCESS_KEY:?APP_STORAGE_ACCESS_KEY is required}"
: "${APP_STORAGE_SECRET_KEY:?APP_STORAGE_SECRET_KEY is required}"
: "${APP_STORAGE_BUCKET:?APP_STORAGE_BUCKET is required}"

policy_file=/tmp/syncturtle-app-policy.json

cat >"${policy_file}" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetBucketLocation",
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads",
        "s3:ListBucketVersions"
      ],
      "Resource": ["arn:aws:s3:::${APP_STORAGE_BUCKET}"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:AbortMultipartUpload",
        "s3:DeleteObject",
        "s3:DeleteObjectVersion",
        "s3:GetObject",
        "s3:ListMultipartUploadParts",
        "s3:PutObject"
      ],
      "Resource": ["arn:aws:s3:::${APP_STORAGE_BUCKET}/*"]
    }
  ]
}
EOF

mc alias set local http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"
mc ready local
mc mb --ignore-existing "local/${APP_STORAGE_BUCKET}"
mc version enable "local/${APP_STORAGE_BUCKET}"
mc anonymous set none "local/${APP_STORAGE_BUCKET}"
mc admin user add local "${APP_STORAGE_ACCESS_KEY}" "${APP_STORAGE_SECRET_KEY}"
mc admin policy create local syncturtle-file-service "${policy_file}"
mc admin policy attach local syncturtle-file-service --user "${APP_STORAGE_ACCESS_KEY}"

version_info=$(mc version info "local/${APP_STORAGE_BUCKET}")
case "${version_info}" in
  *"versioning is enabled"*) ;;
  *)
    echo "MinIO bucket versioning verification failed" >&2
    exit 1
    ;;
esac

echo "MinIO bucket, versioning, private access, and file-service policy are ready"
