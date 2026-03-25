#!/bin/sh
#
# Sets up local git hooks from .githooks/ directory.
# Run this once after cloning the repository.
#

set -e

echo "📦 Setting up git hooks..."

git config core.hooksPath .githooks
chmod +x .githooks/*

echo "✅ Git hooks configured. Using .githooks/ directory."
echo ""
echo "Hooks installed:"
echo "  - pre-commit    → compile check, conflict markers, secrets scan"
echo "  - commit-msg    → conventional commits format"
echo "  - pre-push      → tests, plugin verification, branch naming"
