#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
tools_dir="$project_root/.tools"
maven_version="${MAVEN_VERSION:-3.9.10}"
maven_home="$tools_dir/apache-maven-$maven_version"
local_maven="$maven_home/bin/mvn"
archive="$tools_dir/apache-maven-$maven_version-bin.tar.gz"
url="https://archive.apache.org/dist/maven/maven-3/$maven_version/binaries/apache-maven-$maven_version-bin.tar.gz"

if command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    exit 0
fi

if [[ -x "$local_maven" ]]; then
    echo "$local_maven"
    exit 0
fi

mkdir -p "$tools_dir"
curl -fsSL "$url" -o "$archive"
tar -xzf "$archive" -C "$tools_dir"
rm -f "$archive"

echo "$local_maven"