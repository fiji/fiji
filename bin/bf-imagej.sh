#!/bin/sh

#
# bf-imagej.sh
#

# Uploads Bio-Formats daily builds to the Bio-Formats update site.

# -- Parameters --

JAR_URL="http://hudson.openmicroscopy.org.uk/job/BIOFORMATS-stable/lastSuccessfulBuild/artifact/artifacts"
JAR_FILES="
jars/bio-formats
jars/bio-formats/jai_imageio
jars/bio-formats/scifio
jars/loci-common
jars/mdbtools-java
jars/metakit
jars/ome-xml
jars/poi-loci
plugins/loci_plugins
"
UPDATE_SITE_NAME="Bio-Formats daily builds"
UPDATE_SITE_URL="http://sites.imagej.net/Bio-Formats/"
UPDATE_SITE_USER="Bio-Formats"

# -- Constants --

FIJI_ARCHIVE=fiji-linux64.tar.gz
FIJI_URL=http://jenkins.imagej.net/job/Stable-Fiji/lastSuccessfulBuild/artifact/$FIJI_ARCHIVE
FIJI_DIR=Fiji.app

# -- Derived variables --

PASSWD_FILE="$HOME/$UPDATE_SITE_USER-WebDAV.passwd"

# -- Download and unpack Fiji if it is not already present --

if [ ! -d "$FIJI_DIR" ]
then
	wget -nv "$FIJI_URL"
	tar xf "$FIJI_ARCHIVE"
	rm "$FIJI_ARCHIVE"
	"$FIJI_DIR"/ImageJ-linux64 --update add-update-site "$UPDATE_SITE_NAME" \
		"$UPDATE_SITE_URL" \
		"webdav:$UPDATE_SITE_USER:$(cat "$PASSWD_FILE")" .
fi

cd "$FIJI_DIR"

# -- First, make sure that Fiji is up-to-date --

./ImageJ-linux64 --update update-force-pristine

# -- Download JAR files and install into local Fiji --

FILES_TO_UPLOAD=""
for jar in $JAR_FILES
do
	echo
	echo "--== $jar ==--"
	echo

	FILENAME="$(echo $jar | sed 's/.*\///').jar"
	REMOTE_URL="$JAR_URL/$FILENAME"
	RM_PATHS="$jar.jar $jar-[0-9]*.jar"

	wget -nv "$REMOTE_URL"
	rm -rf $RM_PATHS
	mv "$FILENAME" "$jar.jar"

	FILES_TO_UPLOAD="$FILES_TO_UPLOAD $jar.jar"
done

# -- Upload files to the update site! --

./ImageJ-linux64 --update upload \
	--update-site "$UPDATE_SITE_NAME" --force-shadow $FILES_TO_UPLOAD
