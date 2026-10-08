FROM alpine:3.23 AS download

ARG TARGETARCH
ARG MC_RELEASE=RELEASE.2025-08-13T08-35-41Z

RUN apk add --no-cache ca-certificates curl && \
    case "${TARGETARCH}" in \
      amd64) expected_sha256=01f866e9c5f9b87c2b09116fa5d7c06695b106242d829a8bb32990c00312e891 ;; \
      arm64) expected_sha256=14c8c9616cfce4636add161304353244e8de383b2e2752c0e9dad01d4c27c12c ;; \
      *) echo "Unsupported MinIO Client architecture: ${TARGETARCH}" >&2; exit 1 ;; \
    esac && \
    curl --fail --location --show-error --silent --retry 3 \
      "https://github.com/minio/mc/releases/download/${MC_RELEASE}/mc.linux-${TARGETARCH}.${MC_RELEASE}" \
      --output /tmp/mc && \
    echo "${expected_sha256}  /tmp/mc" | sha256sum -c && \
    chmod 0755 /tmp/mc

FROM alpine:3.23

RUN apk add --no-cache ca-certificates

COPY --from=download /tmp/mc /usr/bin/mc

ENTRYPOINT ["/usr/bin/mc"]
