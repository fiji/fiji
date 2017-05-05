#!/bin/sh

#
# upload-update-site.sh
#

# Uploads JARs from an external location to an ImageJ update site.
#
# Here is how this script was once used to update the Bio-Formats update site:
#
#   export JAR_URL="https://ci.openmicroscopy.org/job/BIOFORMATS-DEV-latest/lastSuccessfulBuild/artifact/artifacts/"
#
#   export JAR_FILES="
#   jars/bio-formats/formats-api
#   jars/bio-formats/formats-bsd
#   jars/bio-formats/formats-common
#   jars/bio-formats/formats-gpl
#   jars/bio-formats/jai_imageio
#   jars/bio-formats/mdbtools-java
#   jars/bio-formats/metakit
#   jars/bio-formats/ome-jxr
#   jars/bio-formats/ome-poi
#   jars/bio-formats/ome-xml
#   jars/bio-formats/specification
#   jars/bio-formats/turbojpeg
#   plugins/bio-formats_plugins
#   "
#
#   export UPDATE_SITE_NAME="Bio-Formats"
#   export UPDATE_SITE_USER="Bio-Formats"
#   export UPDATE_SITE_URL="http://sites.imagej.net/$UPDATE_SITE_USER/"
#
#   sh "$(dirname "$0")"/upload-update-site.sh

# -- Constants --

FIJI_ARCHIVE=fiji-linux64.tar.gz
FIJI_URL=http://jenkins.imagej.net/job/Stable-Fiji/lastSuccessfulBuild/artifact/$FIJI_ARCHIVE
FIJI_DIR=Fiji.app

# -- Check parameters --

if [ -z "$JAR_URL" ]; then echo "No JAR_URL."; exit 1; fi
if [ -z "$JAR_FILES" ]; then echo "No JAR_FILES."; exit 2; fi
if [ -z "$UPDATE_SITE_NAME" ]; then echo "No UPDATE_SITE_NAME."; exit 3; fi
if [ -z "$UPDATE_SITE_URL" ]; then echo "No UPDATE_SITE_URL."; exit 4; fi
if [ -z "$UPDATE_SITE_USER" ]; then echo "No UPDATE_SITE_USER."; exit 5; fi

PASSWD_FILE="$HOME/$UPDATE_SITE_USER-WebDAV.passwd"

# -- Download and unpack Fiji if it is not already present --

if [ -d "$FIJI_DIR" ]
then
	FIJI_INITIALIZED=1
fi

if [ -z "$FIJI_INITIALIZED" ]
then
	wget -nv "$FIJI_URL"
	tar xf "$FIJI_ARCHIVE"
	rm "$FIJI_ARCHIVE"
fi

# -- Identify ImageJ launcher executable --

OS_NAME="$(uname)"
if [ "$OS_NAME" = "Linux" ]
then
	OS_ARCH="$(uname -m)"
	if [ "$OS_ARCH" = "x86_64" ]
	then
		EXE="ImageJ-linux64"
	else
		EXE="ImageJ-linux32"
	fi
elif [ "$OS_NAME" == "Darwin" ]
then
	EXE="Contents/MacOS/ImageJ-macosx"
else
	echo "Unsupported OS: $OS_NAME"
	exit 6
fi
if [ ! -e "$FIJI_DIR/$EXE" ]
then
	echo "Cannot find ImageJ launcher: $EXE"
	exit 7
fi

cd "$FIJI_DIR"

# -- First, make sure that Fiji is up-to-date --

if [ -z "$FIJI_INITIALIZED" ]
then
	./$EXE --update add-update-site "$UPDATE_SITE_NAME" "$UPDATE_SITE_URL"
fi
./$EXE --update update-force-pristine

# -- Download JAR files and install into local Fiji --

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
done

# -- Upload files to the update site! --

./$EXE --update edit-update-site "$UPDATE_SITE_NAME" "$UPDATE_SITE_URL" \
	"webdav:$UPDATE_SITE_USER:$(cat "$PASSWD_FILE")" .
./$EXE --update upload-complete-site --force-shadow "$UPDATE_SITE_NAME"
./$EXE --update edit-update-site "$UPDATE_SITE_NAME" "$UPDATE_SITE_URL"
