#!/bin/sh

# This script uses the ImageJ Maven repository at http://maven.imagej.net/
# to fetch an artifact, or to determine the state of it.

root_url=http://maven.imagej.net/content/repositories

die () {
	echo "$*" >&2
	exit 1
}

# Parse <groupId>:<artifactId>:<version> triplets (i.e. GAV parameters)

groupId () {
	echo "${1%%:*}"
}

artifactId () {
	result="${1#*:}"
	echo "${result%%:*}"
}

version () {
	result="${1#*:}"
	case "$result" in
	*:*)
		echo "${1##*:}"
		;;
	esac
}

# Given an xml, extract the first <tag>

extract_tag () {
	result="${2#*<$1>}"
	case "$result" in
	"$2")
		;;
	*)
		echo "${result%%</$1>*}"
		;;
	esac
}

# Given a GAV parameter, determine the base URL of the project

project_url () {
	gav="$1"
	artifactId="$(artifactId "$gav")"
	infix="$(groupId "$gav" | tr . /)/$artifactId"
	version="$(version "$gav")"
	case "$version" in
	*SNAPSHOT)
		echo "$root_url/snapshots/$infix"
		;;
	*)
		echo "$root_url/releases/$infix"
		;;
	esac
}

# Given a GAV parameter, determine the URL of the .jar file

jar_url () {
	gav="$1"
	artifactId="$(artifactId "$gav")"
	version="$(version "$gav")"
	infix="$(groupId "$gav" | tr . /)/$artifactId/$version"
	case "$version" in
	*-SNAPSHOT)
		url="$root_url/snapshots/$infix/maven-metadata.xml"
		metadata="$(curl -s "$url")"
		timestamp="$(extract_tag timestamp "$metadata")"
		buildNumber="$(extract_tag buildNumber "$metadata")"
		version=${version%-SNAPSHOT}-$timestamp-$buildNumber
		echo "$root_url/snapshots/$infix/$artifactId-$version.jar"
		;;
	*)
		echo "$root_url/releases/$infix/$artifactId-$version.jar"
		;;
	esac
}

# Generate a temporary file; not thread-safe

tmpfile () {
	i=1
	while test -f /tmp/precompiled.$i"$1"
	do
		i=$(($i+1))
	done
	echo /tmp/precompiled.$i"$1"
}

# Given a GAV parameter, make a list of its dependencies (as GAV parameters)

get_dependencies () {
	url="$(jar_url "$1")"
	pom="$(curl -s "${url%.jar}.pom")"
	while true
	do
		case "$pom" in
		*'<dependency>'*)
			dependency="$(extract_tag dependency "$pom")"
			groupId="$(extract_tag groupId "$dependency")"
			test '${project.groupId}' = $groupId &&
			groupId="$(groupId "$1")"
			artifactId="$(extract_tag artifactId "$dependency")"
			version="$(extract_tag version "$dependency")"
			test '${project.version}' = $version &&
			version="$(version "$1")"
			echo "$groupId:$artifactId:$version"
			pom="${pom#*</dependency>}"
			;;
		*)
			break;
		esac
	done
}

# Given a GAV parameter, download the .jar file

get_jar () {
	url="$(jar_url "$1")"
	tmpfile="$(tmpfile .jar)"
	curl -s "$url" > "$tmpfile"
	echo "$tmpfile"
}

# The main part

case "$1" in
commit)
	jar="$(get_jar "$2")"
	unzip -p "$jar" META-INF/MANIFEST.MF |
	sed -n -e 's/^Implementation-Build: *//pi'
	rm "$jar"
	;;
deps|dependencies)
	get_dependencies "$2"
	;;
latest-version)
	metadata="$(curl -s "$(project_url "$2")"/maven-metadata.xml)"
	latest="$(extract_tag latest "$metadata")"
	test -n "$latest" || latest="$(extract_tag version "$metadata")"
	echo "$latest"
	;;
*)
	die "Usage: $0 [command] [argument...]"'

Commands:

commit <groupId>:<artifactId>:<version>
	Gets the commit from which the given artifact was built
'
	;;
esac
