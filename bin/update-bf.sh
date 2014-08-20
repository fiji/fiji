#!/bin/sh

# update-bf.sh - A script to update the version of the Bio-Formats plugins.

set -e

fiji_home="$1"
new_version="$2"
if [ -z "$new_version" ]
then
  echo "Usage: update-bf.sh <fiji_home> <new_version>"
  exit 1
fi
if [ ! -d "$fiji_home/jars/bio-formats" ]
then
  echo "Not a Fiji installation: '$fiji_home'"
  exit 2
fi

echo Downloading $new_version artifacts...
mvn dependency:get -Dartifact=ome:bio-formats_plugins:$new_version -DremoteRepositories=ome::::http://artifacts.openmicroscopy.org/artifactory/repo

repo="$HOME/.m2/repository"

echo Copying $new_version artifacts...
cd $fiji_home
for f in jars/bio-formats/*.jar plugins/bio-formats_plugins*.jar
do
  dest_dir="$(dirname "$f")"
  filename="$(basename "$f")"
  artifact="${filename%-[0-9].*}"
  source="$repo/ome/$artifact/$new_version/$artifact-$new_version.jar"
  cp "$source" "$dest_dir" && rm "$f"
done
