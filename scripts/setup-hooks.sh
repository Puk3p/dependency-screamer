#!/bin/sh
set -e

git config core.hooksPath .githooks
chmod +x .githooks/*

echo "Git hooks configured. Using .githooks/ directory."
echo ""
echo "Hooks installed:"
echo "  - pre-commit    compile check, conflict markers, secrets scan, spotless"
echo "  - commit-msg    conventional commits format"
echo "  - pre-push      branch naming, spotless check, spotbugs, tests, plugin verify"
