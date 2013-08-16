#!/bin/sh

# This script helps with merges of formerly unrelated repositories into
# subdirectories of fiji.git.

die () {
	echo "$*" >&2
	exit 1
}

test $# = 2 ||
die "Usage: $0 <remote> <branch>"

# Make sure that the working directory is not dirty
git update-index --ignore-submodules --refresh &&
git diff-files --quiet --ignore-submodules &&
git diff-index --cached --quiet HEAD -- ||
die "There are uncommitted changes!"

# Fetch the remtoe
git fetch "$1" ||
die "Error fetching $1"

# Determine which commit to merge
TO_MERGE="$(git rev-parse "$1/$2")" ||
die "No branch $1/$2"

# Figure out the last common commit between the remote and the current branch
if ! MERGE_BASE="$(git merge-base $TO_MERGE HEAD)"
then
	# We haven't merged this remote branch at all!
	cat >&2 << EOF
The branch $1/$2 was never merged into the current branch. If you want to
merge it, please specify the directory into which you want it merged,
otherwise please just hit Ctrl+C:

EOF
	printf "subdirectory: " >&2
	read SUBDIRECTORY
	test -n "$SUBDIRECTORY" ||
	exit 1

	git merge -m "Renaming merge of '$1/$2' into $SUBDIRECTORY" \
		-s ours "$1/$2" &&
	git read-tree --prefix="$SUBDIRECTORY" HEAD^2 &&
	git stash -k &&
	git commit --amend -s ||
	die "Could not merge $1/$2"

	exit 0
fi

# Determine the latest time we merged
LATEST_MERGE="$(git rev-list --parents HEAD..."$1/$2" |
	sed -n "s/^\([^ ]*\) \([^ ]*\) $MERGE_BASE$/\1/p" |
	head -n 1)" &&
test -n "$LATEST_MERGE" ||
die "Could not find the latest merge with $1/$2"

# Figure out what subdirectory we merged into; there might be a couple of false
# positives, so let's have a majority vote
SUBDIRECTORY="$(git diff -M --raw $LATEST_MERGE^2..$LATEST_MERGE 2> /dev/null |
	sed -n 's/^.* R100	//p' |
	while read oldname newname
	do
		case "$newname" in
		*/"$oldname")
			echo ${newname%/$oldname};;
		esac
	done |
	sort |
	uniq -c |
	sort -n -r |
	sed -n '1s/^ *[0-9]* *//p')" &&
test -n "$SUBDIRECTORY" ||
die "Could not detect the rename"

# Fake previously merged and current remote commit as if they were in
# the specified subdirectory
FAKE_INDEX="$(git rev-parse --show-cdup).git/RENAME_INDEX" &&
FAKE_COMMIT=$LATEST_MERGE^2 &&
for COMMIT in $LATEST_MERGE^2 "$1/$2"
do
	rm -f "$FAKE_INDEX" &&
	GIT_INDEX_FILE="$FAKE_INDEX" git read-tree --prefix="$SUBDIRECTORY" "$COMMIT" &&
	FAKE_TREE="$(GIT_INDEX_FILE="$FAKE_INDEX" git write-tree)" &&
	FAKE_COMMIT="$(echo "fake $COMMIT" |
		git commit-tree $FAKE_TREE -p $FAKE_COMMIT)" ||
	die "Could not make intermediate renaming commit"
done

# Perform the merge
git merge -m "Renaming merge of '$1/$2' into $SUBDIRECTORY" -s ours "$1/$2" &&
git cherry-pick -n $FAKE_COMMIT &&
git commit --amend -s -C HEAD ||
die "Could not perform renaming merge"
