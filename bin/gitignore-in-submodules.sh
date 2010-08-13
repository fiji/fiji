#!/bin/bash

# This script requires bash because of the <() construct

FIJI_ROOT="$(cd "$(dirname "$0")"/.. && pwd)"
EXCLUDE=.git/info/exclude

all_submodules () {
	git --git-dir="$FIJI_ROOT/.git" ls-files --stage |
	sed -n 's/^160000 .\{40\} 0.//p'
}

submodule_exists () {
	test -d "$FIJI_ROOT/$1/.git"
}

exclude_exists () {
	test -d "$FIJI_ROOT/$1/$EXCLUDE"
}

get_gitignore_from_submodule () {
	(cd "$FIJI_ROOT/$1/" &&
	 if test -f $EXCLUDE
	 then
		grep -v '^#' < $EXCLUDE || :
	 fi &&
	 git ls-files --exclude-standard --others --directory) |
	sed "s|^|/$1/|"
}

get_gitignore_for_submodule () {
	sed -n "s|^/$1/||p" < "$FIJI_ROOT/.gitignore"
}

update_submodules_exclude () {
	if test ! -f "$FIJI_ROOT/$1/$EXCLUDE"
	then
		: > "$FIJI_ROOT/$1/$EXCLUDE"
	fi &&
	NEW="$(comm -13 <(sort < "$FIJI_ROOT/$1/$EXCLUDE" | uniq) \
		<(get_gitignore_for_submodule "$1" | sort))" &&
	if test -n "$NEW"
	then
		echo "$NEW" >> "$FIJI_ROOT/$1/$EXCLUDE"
	fi
}

update_gitignore () {
	NEW="$(comm -13 <(sort < "$FIJI_ROOT/.gitignore" | uniq) \
		<(get_gitignore_from_submodule "$1" | sort))" &&
	if test -n "$NEW"
	then
		echo "$NEW" >> "$FIJI_ROOT/.gitignore"
	fi
}

die () {
	echo "$*" >&2
	exit 1
}

test $# = 0 && die "Usage: $0 (gitignore | submodule) [<submodule>...]"

case "$1" in
gitignore|submodule|ignoredirty)
	mode="$1" &&
	shift
	;;
*)
	die "Need to know what to update: 'gitignore' or 'submodule'"
	;;
esac

if test $# = 0
then
	set $(all_submodules)
fi &&
for submodule in "$@"
do
	if $(submodule_exists "$submodule")
	then
		case "$mode" in
		gitignore)
			update_gitignore "$submodule"
			;;
		submodule)
			update_submodules_exclude "$submodule"
			;;
		ignoredirty)
			git config submodule."$submodule".ignore dirty
			;;
		*)
			false
			;;
		esac || die "Could not handle submodule $submodule"
	fi
done
