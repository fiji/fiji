#!/bin/sh

#
# upload-bf5.sh
#

# Uploads Bio-Formats develop builds to the Bio-Formats-5 update site.

export JAR_URL="http://hudson.openmicroscopy.org.uk/job/BIOFORMATS-trunk/lastSuccessfulBuild/artifact/artifacts"
export JAR_FILES="
jars/JWlz
jars/bio-formats
jars/bio-formats/jai_imageio
jars/bio-formats/scifio
jars/bio-formats/specification
jars/bio-formats/turbojpeg
jars/joda-time-2.2
jars/kryo-2.21-shaded
jars/loci-common
jars/mdbtools-java
jars/metakit
jars/native-lib-loader-2.0-SNAPSHOT
jars/ome-xml
jars/poi-loci
jars/slf4j-api-1.7.2
plugins/loci_plugins
"
export UPDATE_SITE_NAME="Bio-Formats 5"
export UPDATE_SITE_USER="Bio-Formats-5"
export UPDATE_SITE_URL="http://sites.imagej.net/$UPDATE_SITE_USER/"

sh "$(dirname "$0")"/upload-update-site.sh
