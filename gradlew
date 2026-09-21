#!/usr/bin/env sh
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle was not found in PATH." >&2
  echo "Install Gradle or use the GitHub Actions build." >&2
  exit 1
fi
exec gradle "$@"
