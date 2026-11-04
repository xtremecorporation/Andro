#!/usr/bin/env sh
set -e
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle 8.13 is required. Install Gradle or run this project through GitHub Actions." >&2
exit 1
