#!/usr/bin/env sh
set -eu

JDK_VERSION="${JDK_VERSION:-21}"
JDK_ARCH="${JDK_ARCH:-x64}"
JDK_IMAGE_TYPE="${JDK_IMAGE_TYPE:-jdk}"
JDK_VENDOR="${JDK_VENDOR:-eclipse}"
OUTPUT_DIR="${OUTPUT_DIR:-docker/jdk}"
OUTPUT_FILE="${OUTPUT_FILE:-temurin-${JDK_VERSION}-${JDK_IMAGE_TYPE}-linux-${JDK_ARCH}.tar.gz}"
OUTPUT_PATH="${OUTPUT_DIR}/${OUTPUT_FILE}"
DOWNLOAD_URL="https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/ga/linux/${JDK_ARCH}/${JDK_IMAGE_TYPE}/hotspot/normal/${JDK_VENDOR}?project=jdk"

mkdir -p "${OUTPUT_DIR}"

echo "Downloading ${JDK_IMAGE_TYPE} ${JDK_VERSION} for linux/${JDK_ARCH}..."
curl --fail --location --show-error --output "${OUTPUT_PATH}" "${DOWNLOAD_URL}"
echo "Saved ${OUTPUT_PATH}"
