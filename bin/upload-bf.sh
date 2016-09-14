#!/bin/sh

#
# upload-bf.sh
#

# Uploads Bio-Formats development builds to the Bio-Formats update site.

export JAR_URL="https://ci.openmicroscopy.org/job/BIOFORMATS-DEV-latest/lastSuccessfulBuild/artifact/artifacts/"

export JAR_FILES="
jars/bio-formats/formats-api
jars/bio-formats/formats-bsd
jars/bio-formats/formats-common
jars/bio-formats/formats-gpl
jars/bio-formats/jai_imageio
jars/bio-formats/mdbtools-java
jars/bio-formats/metakit
jars/bio-formats/ome-jxr
jars/bio-formats/ome-poi
jars/bio-formats/ome-xml
jars/bio-formats/specification
jars/bio-formats/turbojpeg
plugins/bio-formats_plugins
"

export UPDATE_SITE_NAME="Bio-Formats"
export UPDATE_SITE_USER="Bio-Formats"
export UPDATE_SITE_URL="http://sites.imagej.net/$UPDATE_SITE_USER/"

sh "$(dirname "$0")"/upload-update-site.sh
