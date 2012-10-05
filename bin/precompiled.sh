#!/bin/sh

# This script uses the ImageJ Maven repository at http://maven.imagej.net/
# to fetch an artifact, or to determine the state of it.

# error out whenever a command fails
set -e

root_url=http://maven.imagej.net/content/repositories
ij_dir="$(cd "$(dirname "$0")"/.. && pwd)"

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

# Given a GAV parameter, return the URL to the corresponding .pom file

pom_url () {
	url="$(jar_url "$1")"
	echo "${url%.jar}.pom"
}

# Given a POM file, find its GAV parameter

gav_from_pom () {
	pom="$(cat "$1")"
	parent="$(extract_tag parent "$pom")"
	pom="${pom#*$parent}"
	pom="${pom%$(extract_tag dependencies "$pom")*}"
	pom="${pom%$(extract_tag profiles "$pom")*}"
	pom="${pom%$(extract_tag build "$pom")*}"
	groupId="$(extract_tag groupId "$pom")"
	test -n "$groupId" || groupId="$(extract_tag groupId "$parent")"
	artifactId="$(extract_tag artifactId "$pom")"
	version="$(extract_tag version "$pom")"
	test -n "$version" || version="$(extract_tag version "$parent")"
	echo "$groupId:$artifactId:$version"
}

# Given a GAV parameter possibly lacking a version, determine the latest version

latest_version () {
	metadata="$(curl -s "$(project_url "$1")"/maven-metadata.xml)"
	latest="$(extract_tag latest "$metadata")"
	test -n "$latest" || latest="$(extract_tag version "$metadata")"
	echo "$latest"
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

# Given a GAV parameter and a name, resolve a property (falling back to parents)

get_property () {
	gav="$1"
	key="$2"
	case "$key" in
	imagej1.version)
		latest_version net.imagej:ij
		return
		;;
	project.groupId)
		groupId "$gav"
		return
		;;
	project.version)
		version "$gav"
		return
		;;
	esac
	while test -n "$gav"
	do
		pom="$(curl -s "$(pom_url "$gav")")"
		properties="$(extract_tag properties "$pom")"
		property="$(extract_tag "$key" "$properties")"
		if test -n "$property"
		then
			echo "$property"
			return
		fi
		parent="$(extract_tag parent "$pom")"
		groupId="$(extract_tag groupId "$parent")"
		artifactId="$(extract_tag artifactId "$parent")"
		version="$(extract_tag version "$parent")"
		gav="$groupId:$artifactId:$version"
	done
	die "Could not resolve \${$2} in $1"
}

# Given a GAV parameter and a string, expand properties

expand () {
	gav="$1"
	string="$2"
	result=
	while true
	do
		case "$string" in
		*'${'*'}'*)
			result="$result${string%%\$\{*}"
			string="${string#*\$\{}"
			key="${string%\}*}"
			result="$result$(get_property "$gav" "$key")"
			string="${string#$key\}}"
			;;
		*)
			echo "$result$string"
			break
			;;
		esac
	done
}

# Given a GAV parameter, make a list of its dependencies (as GAV parameters)

get_dependencies () {
	pom="$(curl -s "$(pom_url "$1")")"
	while true
	do
		case "$pom" in
		*'<dependency>'*)
			dependency="$(extract_tag dependency "$pom")"
			scope="$(extract_tag scope "$dependency")"
			case "$scope" in
			''|compile)
				groupId="$(expand "$1" "$(extract_tag groupId "$dependency")")"
				artifactId="$(extract_tag artifactId "$dependency")"
				version="$(expand "$1" "$(extract_tag version "$dependency")")"
				echo "$groupId:$artifactId:$version"
				;;
			esac
			pom="${pom#*</dependency>}"
			;;
		*)
			break;
		esac
	done
}

# Given a GAV parameter and a space-delimited list of GAV parameters, expand
# the list by the first parameter and its dependencies (unless the list already
# contains said parameter)

get_all_dependencies () {
	case " $2 " in
	*" $1 "*)
		;; # list already contains the depdendency
	*)
		gav="$1"
		set "" "$2 $1"
		for dependency in $(get_dependencies "$gav")
		do
			set "" "$(get_all_dependencies "$dependency" "$2")"
		done
		;;
	esac
	echo "$2"
}

# Given a GAV parameter, download the .jar file

get_jar () {
	url="$(jar_url "$1")"
	tmpfile="$(tmpfile .jar)"
	curl -s "$url" > "$tmpfile"
	echo "$tmpfile"
}

# Given a GAV parameter, determine whether the .jar file is already in plugins/
# or jars/

is_jar_installed () {
	artifactId="$(artifactId "$1")"
	version="$(version "$1")"
	test -f "$ij_dir/plugins/$artifactId-$version.jar" ||
	test -f "$ij_dir/jars/$artifactId-$version.jar"
}

# Given a .jar file, determine whether it is an ImageJ 1.x plugin

is_ij1_plugin () {
	unzip -l "$1" plugins.config > /dev/null 2>&1
}

# Given a GAV parameter, download the .jar file and its dependencies as needed
# and install them into plugins/ or jars/, respectively

install_jar () {
	for gav in $(get_all_dependencies "$1")
	do
		if ! is_jar_installed "$gav"
		then
			tmp="$(get_jar "$gav")"
			name="$(artifactId "$gav")-$(version "$gav").jar"
			if is_ij1_plugin "$tmp"
			then
				mv "$tmp" "$ij_dir/plugins/$name"
			else
				mv "$tmp" "$ij_dir/jars/$name"
			fi
		fi
	done
}

# The main part

case "$1" in
commit)
	jar="$(get_jar "$2")"
	unzip -p "$jar" META-INF/MANIFEST.MF |
	sed -n -e 's/^Implementation-Build: *//pi' |
	tr -d '\r'
	rm "$jar"
	;;
deps|dependencies)
	get_dependencies "$2"
	;;
all-deps|all-dependencies)
	get_all_dependencies "$2" |
	tr ' ' '\n' |
	grep -v '^$'
	;;
latest-version)
	latest_version "$2"
	;;
gav-from-pom)
	gav_from_pom "$2"
	;;
install)
	install_jar "$2"
	;;
*)
	die "Usage: $0 [command] [argument...]"'

Commands:

commit <groupId>:<artifactId>:<version>
	Gets the commit from which the given artifact was built

dependencies <groupId>:<artifactId>:<version>
	Lists the direct dependencies of the given artifact

all-dependencies <groupId>:<artifactId>:<version>
	Lists all dependencies of the given artifact, including itself and
	transitive dependencies

latest-version <groupId>:<artifactId>[:<version>]
	Prints the current version of the given artifact (if "SNAPSHOT" is
	passed as version, it prints the current snapshot version rather
	than the release one)

gav-from-pom <pom.xml>
	Prints the GAV parameter described in the given pom.xml file

install <groupId>:<artifactId>:<version>
	Installs the given artifact and all its dependencies; ImageJ 1.x
	plugins will be installed into plugins/, all other .jar files into
	jars/
'
	;;
esac
