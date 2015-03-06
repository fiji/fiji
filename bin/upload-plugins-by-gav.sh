#!/bin/sh

# Downloads the newest snapshot version for one or more plugins,
# installs it with dependencies to Fiji, and uploads the modified
# state to the specified update site.
#
# DEPENDENCIES: maven-helper.sh in bin/

die () {
	echo "$*" >&2
	exit 1
}

test $# -ge 3 ||
die "Usage: $0 <update-site> <webdav-user> <groupId1>:<artifactId1> [<groupId2>:<artifactId2> ...]"

update_site="$1"
webdav_user="$2"
url="http://sites.imagej.net/$update_site/"

shift
shift

# initialize Fiji.app/ directory
curl -O http://update.imagej.net/bootstrap.js
jrunscript bootstrap.js update-force-pristine

# determine correct launcher to launch MiniMaven and the Updater
case "$(uname -s),$(uname -m)" in
Linux,x86_64) launcher=ImageJ-linux64;;
Linux,*) launcher=ImageJ-linux32;;
Darwin,*) launcher=Contents/MacOS/ImageJ-tiger;;
MING*,*) launcher=ImageJ-win32.exe;;
*) echo "Unknown platform" >&2; exit 1;;
esac

maven_helper=bin/maven-helper.sh
imagejMavenVer="$(sh $maven_helper latest-version net.imagej:imagej-maven-plugin:RELEASE)"

# install all gavs
while test $# -gt 0
do
	groupId="${1%%:*}"
	artifactId="${1##*:}"

	version="$(sh $maven_helper latest-version $groupId:$artifactId:SNAPSHOT)"

	# delete the artifact, just in case
	rm -f "plugins/$artifactId-$version.jar"

	# install plugin and dependencies using the imagej-maven-plugin
	mvn -Ddelete.other.versions=true -Dforce=true -Dimagej.app.directory=$(pwd) \
		net.imagej:imagej-maven-plugin:$imagejMavenVer:install-artifact \
		-Dartifact=$groupId:$artifactId:$version

	shift
done

# upload complete update site
password=
if test -f "$HOME/$webdav_user.passwd"
then
	password=":$(cat "$HOME/$webdav_user.passwd")"
	echo "Please switch to .netrc method"
fi
./$launcher --update edit-update-site $update_site $url "webdav:$webdav_user$password" .
./$launcher --update upload-complete-site --force --force-shadow $update_site
test -z "$password" ||
./$launcher --update edit-update-site $update_site $url
