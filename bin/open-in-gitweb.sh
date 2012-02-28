#!/bin/sh

test ! -z "$BROWSER" || {
	for BROWSER in xdg-open start open
	do
		$BROWSER --help > /dev/null 2>&1
		test $? != 127 && break
	done || {
		echo "No suitable browser found" >&2
		exit 1
	}
}

get_url () {
	url=$(git config remote.origin.url)
	if test -z "$url"
	then
		branch=$(git rev-parse --symbolic-full-name HEAD) &&
		remote=$(git config branch.${branch#refs/heads/}.remote) &&
		if test -z "$remote"
		then
			remote=$(git config branch.master.remote)
		fi &&
		url=$(git config remote.$remote.url) &&
		test ! -z "$url"
	fi || {
		echo "Remote 'origin' not found: $(pwd)" >&2
		exit 1
	}

	case $url in
	repo.or.cz:*)
		echo "http://repo.or.cz/w/${url#*repo.or.cz:/srv/git/}?"
		;;
	ssh://repo.or.cz/*)
		echo "http://repo.or.cz/w/${url#*repo.or.cz/srv/git/}?"
		;;
	*)
		project=${url%/}
		case "$project" in
		*/.git)
			project=${project%/.git}
			project=${project##*/}/.git
			;;
		*.git)
			project=${project##*/}
			;;
		*)
			project=${project##*/}/.git
			;;
		esac

		case $project in
		imageja.git) project=ImageJA.git;;
		esac

		case $project in
		ImageJA.git|fiji.git)
			echo "http://github.com/fiji/${project%.git}"
			;;
		imagej2/.git|imagej.git)
			case $project in imagej2/.git) project=imagej;; esac
			project=${project%.git}
			echo "http://github.com/imagej/$project"
			;;
		*)
			echo "http://fiji.sc/cgi-bin/gitweb.cgi?p=$project"
			;;
		esac
		;;
	esac
}

test $# = 0 && set HEAD

for arg
do
	linenumber=$(expr "$arg" : '.*\(:[0-9][0-9]*\)')
	test -z "$linenumber" || {
		arg=${arg%$linenumber}
		linenumber=\#l${linenumber#:}
	}
	if test -f "$arg"
	then
		(cd "$(dirname "$arg")" &&
		HEAD=$(git rev-parse --symbolic-full-name HEAD) &&
		arg=$(git ls-files --full-name "${arg##*/}") &&
		url=$(get_url) &&
		case "$url" in
		*github.com*)
			case "$linenumber" in '#l'*) linenumber=#L${linenumber#\#l};; esac
			url="$url/blob/${HEAD#refs/heads/}/$arg$linenumber"
			;;
		*)
			url="$url;a=blob;f=$arg;hb=$HEAD$linenumber"
			;;
		esac &&
		$BROWSER "$url")
	else
		HEAD=$(git rev-parse --symbolic-full-name HEAD) &&
		arg=$(git rev-parse --verify "$arg") &&
		url=$(get_url) &&
		case "$url" in
		*github.com*)
			test -z "$arg" && arg=${HEAD#refs/heads/}
			url="$url/commit/$arg"
			;;
		*)
			url="$url;a=commitdiff;h=$arg;hb=$HEAD"
			;;
		esac &&
		$BROWSER "$url"
	fi || break
done
