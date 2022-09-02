#!/bin/sh

set -e

# Copies the state of this Fiji working copy into the given Fiji.app directory.
# This will ***ERASE ANY EXISTING JAR FILES*** in the given Fiji.app directory!
# This script is intended for updating an existing Fiji.app, so that the new
# state can then be uploaded to the core update site via the ImageJ Updater.

die() { echo "$1"; exit 1; }

test $# -eq 1 || die "Usage: bin/populate-app.sh /path/to/Fiji.app"
dir=$1
test -d "$dir" || die "No such directory: $dir"
dir=$(cd "$1" && pwd)

cd "$(dirname "$0")/.."

set -x

# Remove old libraries.
find "$dir/jars" "$dir/plugins" -type f -name '*.jar' | \
  grep -v '\(STUB\|jars/xalan-\|jars/serializer-\)' | \
  while read jar; do rm "$jar"; done

# Copy over new libraries.
mvn -DskipTests -Denforcer.skip -Dscijava.app.directory="$dir"

# Relocate incorrectly located libraries.
mv -f "$dir/jars/metakit-"*.jar            "$dir/jars/bio-formats/"
mv -f "$dir/jars/ome-codecs-"*.jar         "$dir/jars/bio-formats/"
mv -f "$dir/jars/ome-common-"*.jar         "$dir/jars/bio-formats/"
mv -f "$dir/jars/ome-jai-"*.jar            "$dir/jars/bio-formats/"
mv -f "$dir/jars/ome-mdbtools-"*.jar       "$dir/jars/bio-formats/"
mv -f "$dir/jars/ome-poi-"*.jar            "$dir/jars/bio-formats/"
mv -f "$dir/jars/ome-xml-"*.jar            "$dir/jars/bio-formats/"
mv -f "$dir/jars/specification-"*.jar      "$dir/jars/bio-formats/"
mv -f "$dir/jars/Correct_3D_Drift-"*.jar   "$dir/plugins/"
mv -f "$dir/jars/KymographBuilder-"*.jar   "$dir/plugins/"
mv -f "$dir/jars/bigdataviewer_fiji-"*.jar "$dir/plugins/"

# Prune extraneous native libraries.
rm "$dir/jars/"*-android-*.jar \
   "$dir/jars/"*-solaris-*.jar \
   "$dir/jars/"*-linux-armv6*.jar \
   "$dir/jars/"*-linux-armhf*.jar \
   "$dir/jars/"*-linux-ppc64le*.jar
