#!/bin/sh

template=2.11.0
version=$1
commit=$2

test "$version" -a "$commit" || {
  echo "[ERROR] Please specify Fiji version, plus hash of following 'Bump to next develoment cycle' commit."
  exit 1
}

set -e
dir=$(dirname "$0")
cd "$dir/.."

git checkout "$commit" -- pom.xml src

for platform in win64 macosx linux64 nojre
do
  # Unpack the template.
  rm -rf Fiji.app

  zip=fiji-$template-$platform.zip
  test -f "$zip" || curl -fLO "https://downloads.imagej.net/fiji/releases/$template/$zip"
  unzip "$zip"

  # Populate with JARs.
  bin/populate-app.sh Fiji.app
  rm Fiji.app/jars/fiji-[0-9]*.jar
  mvn dependency:copy -DoutputDirectory=Fiji.app/jars -Dartifact="sc.fiji:fiji:$version"

  # Delete non-matching platform folders.
  for d in Fiji.app/jars/*/ Fiji.app/lib/*/
  do
    if [ "$platform" != nojre \
      -a "$d" != Fiji.app/jars/bio-formats/ \
      -a "$d" != Fiji.app/jars/"$platform"/ \
      -a "$d" != Fiji.app/lib/"$platform"/ ]
    then
      # Non-matching platform folder.
      rm -rf "$d"
    fi
  done

  # Bundle it up.
  zip -r9 "fiji-$version-$platform.zip" Fiji.app
done

git restore --staged -- pom.xml src
git checkout -- pom.xml src
