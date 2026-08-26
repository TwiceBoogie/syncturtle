FROM golang:1.24.8-alpine AS build

ARG MINIO_RELEASE=RELEASE.2025-10-15T17-29-55Z
ARG MINIO_COMMIT_PREFIX=9e49d5e

RUN apk add --no-cache bash git

WORKDIR /src

RUN git clone --depth 1 --branch "${MINIO_RELEASE}" https://github.com/minio/minio.git . && \
    actual_commit="$(git rev-parse HEAD)" && \
    case "${actual_commit}" in \
        "${MINIO_COMMIT_PREFIX}"*) ;; \
        *) echo "Unexpected MinIO commit ${actual_commit}" >&2; exit 1 ;; \
    esac && \
    ldflags="$(go run buildscripts/gen-ldflags.go)" && \
    CGO_ENABLED=0 go build -tags kqueue -trimpath --ldflags "${ldflags}" -o /out/minio .

FROM alpine:3.23

RUN apk add --no-cache ca-certificates curl tzdata && \
    mkdir -p /data

COPY --from=build /out/minio /usr/local/bin/minio

EXPOSE 9000 9001
VOLUME ["/data"]

ENTRYPOINT ["/usr/local/bin/minio"]
CMD ["server", "/data", "--console-address", ":9001"]
