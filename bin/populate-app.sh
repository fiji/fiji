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

# HACK: Install libraries with clashing artifactIds.
# org.antlr:antlr:3.5.2 vs antlr:antlr:2.7.7
mvn -q dependency:copy -DoutputDirectory="$dir/jars" -Dartifact=antlr:antlr:2.7.7
mv "$dir/jars/antlr-2.7.7.jar" "$dir/jars/antlr.antlr-2.7.7.jar"

# Install native libraries for all platforms.
mvn dependency:list |
  grep '\(macosx\|windows\|linux\)-' |
  grep -v '(optional)' |
  sed 's/.* \(.*\):[^:]*$/\1/' | while read native
do
  set +x
  g=${native%%:*}
  r=${native#*:}
  a=${r%%:*}
  r=${r#*:}
  p=${r%%:*}
  r=${r#*:}
  c=${r%%:*}
  r=${r#*:}
  v=${r%%:*}
  case "$g" in
    org.jogamp.*)
      win32=
      win64=natives-windows-amd64
      macosx=natives-macosx-universal
      linux32=
      linux64=natives-linux-amd64
      ;;
    org.bytedeco)
      win32=windows-x86
      win64=windows-x86_64
      macosx=macosx-x86_64
      linux32=linux-x86
      linux64=linux-x86_64
      ;;
    *)
      echo "[WARNING] Unknown native library '$native' -- not populating it for other platforms"
      continue
      ;;
  esac
  set -x
  test -z "$win32" || mvn -q dependency:copy -DoutputDirectory="$dir/jars/win32" -Dartifact=$g:$a:$v:jar:$win32
  test -z "$win64" || mvn -q dependency:copy -DoutputDirectory="$dir/jars/win64" -Dartifact=$g:$a:$v:jar:$win64
  test -z "$macosx" || mvn -q dependency:copy -DoutputDirectory="$dir/jars/macosx" -Dartifact=$g:$a:$v:jar:$macosx
  test -z "$linux32" || mvn -q dependency:copy -DoutputDirectory="$dir/jars/linux32" -Dartifact=$g:$a:$v:jar:$linux32
  test -z "$linux64" || mvn -q dependency:copy -DoutputDirectory="$dir/jars/linux64" -Dartifact=$g:$a:$v:jar:$linux64
done
