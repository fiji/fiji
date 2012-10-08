#!/bin/sh

# This script is intended to be run by Jenkins after a full, successful build.
# It employs precompiled.sh to find out whether a given artifact's source
# code has changed since the latest deployment.

# error out whenever a command fails
set -e

ij_dir="$(cd "$(dirname "$0")/.." && pwd)"
helper="$ij_dir/bin/precompiled.sh"

for pom in $(git ls-files \*pom.xml)
do
	dir=${pom%pom.xml}
	test -d "$dir"/target || continue
	gav="$("$helper" gav-from-pom "$pom")"
	commit="$("$helper" commit "$gav")"
	git diff --quiet "$commit".. -- "$dir" ||
	(cd "$dir" &&
	 echo "Deploying $dir" &&
	 mvn deploy)
done
