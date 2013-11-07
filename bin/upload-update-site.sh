#!/bin/sh

#
# upload-update-site.sh
#

# Uploads JARs from an external location to an ImageJ update site.
# See upload-bf4.sh and upload-bf5.sh for examples of usage.

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
