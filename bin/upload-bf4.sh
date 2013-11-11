#!/bin/sh

#
# upload-bf4.sh
#

# Uploads Bio-Formats dev_4_4 builds to the Bio-Formats update site.

export JAR_URL="http://hudson.openmicroscopy.org.uk/job/BIOFORMATS-stable/lastSuccessfulBuild/artifact/artifacts"
export JAR_FILES="
jars/bio-formats
jars/bio-formats/jai_imageio
jars/bio-formats/scifio
jars/loci-common
jars/mdbtools-java
jars/metakit
jars/netcdf-4.0.03
jars/ome-xml
jars/poi-loci
jars/slf4j-api-1.7.2
plugins/loci_plugins
"
export UPDATE_SITE_NAME="Bio-Formats 4"
export UPDATE_SITE_USER="Bio-Formats"
export UPDATE_SITE_URL="http://sites.imagej.net/$UPDATE_SITE_USER/"

sh "$(dirname "$0")"/upload-update-site.sh
