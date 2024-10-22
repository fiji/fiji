#!/bin/sh

# This shell script insists to macOS Gatekeeper that
# Fiji really is an OK program to run rather than trash.

dir=$(cd "$(dirname "$0")/.." && pwd)
echo "Fixing $dir..."
set -e
sudo chflags -R nouchg "$dir"
sudo xattr -rd com.apple.quarantine "$dir"
echo "Done. Hopefully it's runnable now!"
