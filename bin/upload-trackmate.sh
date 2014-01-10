#!/bin/sh

#
# upload-trackmate.sh
#

# Uploads TrackMate to the TrackMate-dev update site.

update_site="TrackMate-dev"
webdav_user="Jenkins-TrackMate"
url="http://sites.imagej.net/$update_site/"

groupId=sc.fiji
artifactId=TrackMate_

maven_helper=modules/scijava-common/bin/maven-helper.sh
version="$(sh $maven_helper latest-version $groupId:$artifactId:SNAPSHOT)"

# determine correct launcher to launch MiniMaven and the Updater
case "$(uname -s),$(uname -m)" in
Linux,x86_64) launcher=ImageJ-linux64;;
Linux,*) launcher=ImageJ-linux32;;
Darwin,*) launcher=Contents/MacOS/ImageJ-tiger;;
MING*,*) launcher=ImageJ-win32.exe;;
*) echo "Unknown platform" >&2; exit 1;;
esac

# initialize Fiji.app/ directory
curl -O http://update.imagej.net/bootstrap.js
jrunscript bootstrap.js update-force-pristine

cat > pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
		http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>sc.fiji</groupId>
	<artifactId>dummy</artifactId>
	<version>0.0.0-SNAPSHOT</version>

	<dependencies>
		<dependency>
			<groupId>$groupId</groupId>
			<artifactId>$artifactId</artifactId>
			<version>$version</version>
		</dependency>
	</dependencies>

	<!-- NB: for project parent -->
	<repositories>
		<repository>
			<id>imagej.releases</id>
			<url>http://maven.imagej.net/content/repositories/releases</url>
		</repository>
		<repository>
			<id>imagej.snapshots</id>
			<url>http://maven.imagej.net/content/repositories/snapshots</url>
		</repository>
	</repositories>

</project>
EOF
mkdir -p target
touch target/dummy-0.0.0-SNAPSHOT.jar

# install plugin and dependencies using MiniMaven
./$launcher --mini-maven -Dimagej.app.directory="$(pwd)" install
rm jars/dummy-0.0.0-SNAPSHOT.jar

# work-around: re-install miglayout-swing.jar
./$launcher --update update jars/miglayout-3.7.3.1-swing.jar

# upload complete update site
./$launcher --update remove-update-site $update_site
./$launcher --update add-update-site $update_site $url "webdav:$webdav_user:$(cat "$HOME/$webdav_user.passwd")" .
./$launcher --update upload-complete-site --force --force-shadow $update_site
./$launcher --update edit-update-site $update_site $url
