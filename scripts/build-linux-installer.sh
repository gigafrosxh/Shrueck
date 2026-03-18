#!/usr/bin/env bash
set -euo pipefail

platform_tag="${1:-linux-x64}"
app_version="${APP_VERSION:-1.2.0-SNAPSHOT}"
project_root="$(cd "$(dirname "$0")/.." && pwd)"
local_repo="$project_root/.m2/repository"
input_dir="$project_root/target/jpackage/input"
dist_dir="$project_root/dist/$platform_tag"
maven_cmd="$($project_root/scripts/resolve-maven.sh)"
package_version="$(printf '%s' "$app_version" | sed -E 's/^([0-9]+(\.[0-9]+){0,3}).*/\1/')"

if [[ -z "$package_version" || ! "$package_version" =~ ^[0-9]+(\.[0-9]+){0,3}$ ]]; then
    echo "APP_VERSION '$app_version' enthaelt keinen gueltigen numerischen Versionsprefix fuer jpackage." >&2
    exit 1
fi

rm -rf "$input_dir" "$dist_dir"
mkdir -p "$input_dir" "$dist_dir" "$local_repo"

cd "$project_root"
"$maven_cmd" -q "-Dmaven.repo.local=$local_repo" -DskipTests package dependency:copy-dependencies -DincludeScope=runtime "-DoutputDirectory=$input_dir"

main_jar="$(find "$project_root/target" -maxdepth 1 -type f -name '*.jar' ! -name '*sources*' ! -name '*javadoc*' | head -n 1)"
if [[ -z "$main_jar" ]]; then
    echo "Kein Haupt-JAR in target gefunden." >&2
    exit 1
fi

cp "$main_jar" "$input_dir/"

jpackage \
  --type deb \
  --dest "$dist_dir" \
  --input "$input_dir" \
  --name "shrueck-lan" \
  --app-version "$package_version" \
  --vendor "Shrueck" \
  --description "Shrueck LAN Multiplayer" \
  --main-jar "$(basename "$main_jar")" \
  --main-class at.shrueck.net.game.ShrueckGameLauncher \
  --java-options "-Dfile.encoding=UTF-8" \
  --linux-package-name "shrueck-lan" \
  --linux-deb-maintainer "build@shrueck.local" \
  --linux-menu-group "Games" \
  --linux-shortcut

artifact="$(find "$dist_dir" -maxdepth 1 -type f -name '*.deb' | head -n 1)"
if [[ -z "$artifact" ]]; then
    echo "Kein deb-Installer wurde erzeugt." >&2
    exit 1
fi

mv "$artifact" "$dist_dir/ShrueckLAN-$app_version-$platform_tag.deb"
echo "Installer erzeugt: $dist_dir/ShrueckLAN-$app_version-$platform_tag.deb"