#!/bin/sh

get_project () {
	project=$(git config remote.origin.url) || exit
	project=${project%/}
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
	imageja.git) echo ImageJA.git;;
	*) echo $project;;
	esac
}

url="http://pacific.mpi-cbg.de/cgi-bin/gitweb.cgi?p="
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
		arg=$(git ls-files --full-name "${arg##*/}") &&
		project=$(get_project) &&
		firefox "$url$project;a=blob;f=$arg;hb=HEAD$linenumber")
	else
		project=$(get_project) &&
		firefox "$url$project;a=commitdiff;h=$arg"
	fi || break
done
