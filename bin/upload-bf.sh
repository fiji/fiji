#!/bin/sh

#
# upload-bf.sh
#

# Uploads Bio-Formats development builds to the Bio-Formats update site.

export JAR_URL="http://ci.openmicroscopy.org/job/BIOFORMATS-5.0-latest/lastSuccessfulBuild/artifact/artifacts/"

export JAR_FILES="
jars/bio-formats/formats-api
jars/bio-formats/formats-bsd
jars/bio-formats/formats-common
jars/bio-formats/formats-gpl
jars/bio-formats/jai_imageio
jars/bio-formats/mdbtools-java
jars/bio-formats/metakit
jars/bio-formats/ome-poi
jars/bio-formats/ome-xml
jars/bio-formats/specification
jars/bio-formats/turbojpeg
plugins/bio-formats_plugins
"

# Excluded due to already-matching version:
#jars/JWlz-1.4.0
#jars/forms-1.3.0
#jars/joda-time-2.2
#jars/kryo-2.21-shaded
#jars/logback-classic-1.0.9
#jars/logback-core-1.0.9
#jars/netcdf-4.3.19
#jars/perf4j-0.9.13
#jars/serializer-2.7.1
#jars/slf4j-api-1.7.2
#jars/xalan-2.7.1

# Excluded due to naming mismatch:
#jars/commons-logging

# Excluded due to version downgrade:
#jars/native-lib-loader-2.0-SNAPSHOT
#jars/xml-apis-1.3.02

export UPDATE_SITE_NAME="Bio-Formats"
export UPDATE_SITE_USER="Bio-Formats"
export UPDATE_SITE_URL="http://sites.imagej.net/$UPDATE_SITE_USER/"

sh "$(dirname "$0")"/upload-update-site.sh
