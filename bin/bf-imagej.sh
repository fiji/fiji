#!/bin/sh

#
# bf-imagej.sh
#

# Uploads Bio-Formats daily builds to the Bio-Formats update site.

# -- Constants --

FIJI_ARCHIVE=fiji-linux64.tar.gz
FIJI_URL=http://jenkins.imagej.net/job/Stable-Fiji/lastSuccessfulBuild/artifact/$FIJI_ARCHIVE
FIJI_DIR=Fiji.app

BF_URL=http://hudson.openmicroscopy.org.uk/job/BIOFORMATS-stable/lastSuccessfulBuild/artifact/artifacts
BF_FILES="
jars/bio-formats
jars/bio-formats/scifio
jars/loci-common
jars/mdbtools-java
jars/metakit
jars/ome-xml
jars/poi-loci
plugins/loci_plugins
"
BF_UPDATE_SITE="Bio-Formats daily builds"
BF_PASSWD_FILE="~/Bio-Formats-WebDAV.passwd"

# -- Download and unpack Fiji if it is not already present --

if [ ! -d "$FIJI_DIR" ]
then
	wget "$FIJI_URL"
	tar xf "$FIJI_ARCHIVE"
	rm "$FIJI_ARCHIVE"
	cd "$FIJI_DIR"
	./ImageJ-linux64 --update edit-update-site "$BF_UPDATE_SITE" \
		http://sites.imagej.net/Bio-Formats/ \
		"webdav:Bio-Formats:$(cat "$BF_PASSWD_FILE")" .
	cd ..
fi

cd "$FIJI_DIR"

# -- Download latest Bio-Formats stable build and install into local Fiji --

FILES_TO_UPLOAD=""
for jar in $BF_FILES
do
	echo
	echo "--== $jar ==--"
	echo

	FILENAME="$(echo $jar | sed 's/.*\///').jar"
	REMOTE_URL="$BF_URL/$FILENAME"
	RM_PATH="$jar.jar $jar-[0-9]*.jar"

	wget $REMOTE_URL
	rm -rf $RM_PATH
	mv $FILENAME $jar.jar

	FILES_TO_UPLOAD="$FILES_TO_UPLOAD $jar.jar"
done

# -- Upload files to the Bio-Formats update site! --

echo ./ImageJ-linux64 --update upload \
	--update-site "$BF_UPDATE_SITE" --force-shadow $FILES_TO_UPLOAD
