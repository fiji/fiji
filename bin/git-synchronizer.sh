#!/bin/sh

# This script wants to synchronize multiple public Git repositories with each other

errors=
add_error () {
	errors="$(printf "%s\n\n%s\n\n" "$errors" "$*")"
}

url2remotename () {
	echo "${1%=*}" |
	sed 's/[^-A-Za-z0-9._]/_/g'
}

nullsha1=0000000000000000000000000000000000000000
find_deleted () {
	printf '%s\n%s\n%s\n' "$1" "$2" "$2" |
	sort -k 3 |
	uniq -u -f 2 |
	sed "s/^.\{40\}/$nullsha1/"
}

find_modified () {
	printf '%s\n%s\n' "$2" "$1" |
	sort -s -k 3 |
	uniq -u |
	uniq -d -f 2 |
	uniq -f 2
}

find_new () {
	printf '%s\n%s\n%s\n' "$1" "$1" "$2" |
	sort -k 3 |
	uniq -u -f 2
}

get_remote_branches () {
	name="$1"

	git for-each-ref refs/remotes/$name/\* |
	sed "s|\trefs/remotes/$name/|\t|"
}

fetch_from () {
	url="${1%=*}"
	pushurl="${1#*=}"
	name="$(url2remotename "$url")"

	if test "$url" != "$(git config remote.$name.url 2> /dev/null)"
	then
		git remote add $name $url >&2 || {
			add_error "Could not add remote $name ($url)"
			return 1
		}
		test -n "$pushurl" &&
		test "$pushurl" != "$url" &&
		git config remote.$name.pushURL "$pushurl"
	fi
	previous="$(get_remote_branches $name)"
	git fetch --prune $name >&2 || {
		add_error "Could not fetch $name"
		return 1
	}
	current="$(get_remote_branches $name)"

	find_deleted "$previous" "$current"

	# force modified branches
	find_modified "$previous" "$current" |
	sed 's/^/+/'

	find_new "$previous" "$current"
}

has_spaces () {
	test $# -gt 1
}

get_common_fast_forward () {
	test $# -le 1 && {
		echo "$*"
		return
	}
	head=
	while test $# -gt 0
	do
		commit=$1
		shift
		test -z "$(eval git rev-list --no-walk ^$commit $head $*)" && {
			echo $commit
			return
		}
		head="$head $commit"
	done
	echo $head
}

# Parameter check

test $# -lt 2 && {
	echo "Usage: $0 <Git-URL>[=<push-URL>] <Git-URL>[=<push-URL>]..." >&2
	exit 1
}

test -d .git ||
git init ||
exit

# Fetch

todo=
for urlpair
do
	url="${urlpair%=*}"
	has_spaces $url && {
		add_error "Error: Ignoring URL with spaces: $url"
		continue
	}

	echo "Getting updates from $url..."
	thistodo="$(fetch_from $urlpair)" || {
		add_error "$thistodo"
		continue
	}
	test -z "$thistodo" && continue
	printf "Updates from $url:\n%s\n" "$thistodo"
	todo="$(printf "%s\n%s\n" "$todo" "$thistodo")"
done

remote_branches="$(for url
do
	url="${url%=*}"
	has_spaces $url && continue
	name=$(url2remotename $url)
	git for-each-ref refs/remotes/$name/\* |
	sed "s|^\(.*\)\trefs/remotes/\($name\)/|\2 \1 |"
done)"

for ref in $(echo "$remote_branches" |
	sed 's/.* //' |
	sort |
	uniq)
do
	echo "$todo" | grep "	$ref$" > /dev/null 2>&1 && continue
	sha1="$(echo "$remote_branches" |
		sed -n "s|^[^ ]* \([^ ]*\) [^ ]* $ref$|\1|p" |
		sort |
		uniq)"
	sha1=$(eval get_common_fast_forward $sha1)
	case "$sha1" in
	*\ *)
		add_error "$(printf "Ref $ref is diverging:\n%s\n\n" "$(echo "$remote_branches" |
			grep " $ref$")")"
		continue
		;;
	*)

		if test $# = $(echo "$remote_branches" |
			grep  "$sha1 [^ ]* $ref$" |
			wc -l)
		then
			# all refs agree on one sha1
			continue
		fi
		;;
	esac
	echo "Need to fast-forward $ref to $sha1"
	todo="$(printf "%s\n%s\n" "$todo" "$sha1 commit $ref")"
done

# Verify

# normalize todo

todo="$(echo "$todo" |
	sort -k 3 |
	uniq |
	grep -v '^$')"

# test for disagreeing updates

refs=$(echo "$todo" |
	sed 's/^[^ ]* [^ ]*	//' |
	sort |
	uniq -d)
for ref in $refs
do
	sha1=$(echo "$todo" |
		sed -n "s|^\([^ ]*\) [^ ]*	$ref$|\1|p")
	sha1=$(get_common_fast_forward $sha1)
	has_spaces $sha1 ||
	todo="$(echo "$todo" |
		sed "s|^[^ ]* \([^ ]*	$ref\)$|$sha1 \1|" |
		uniq)"
done

disagreeing="$(echo "$todo" |
	sort -k 3 |
	uniq -D -f 2)"

if test -n "$disagreeing"
then
	add_error "$(printf "Incompatible updates:\n%s\n\n" "$disagreeing")"
fi

# Push

test -z "$todo" ||
for url
do
	url="${url%=*}"
	has_spaces $url && continue
	name="$(url2remotename $url)"
	pushopts=$(echo "$todo" |
		while read sha1 type ref
		do
			test -z "$sha1" && continue
			if echo "$disagreeing" | grep "	$ref$" > /dev/null 2>&1
			then
				continue
			fi
			remoteref=refs/remotes/$name/$ref
			if test $sha1 = $nullsha1
			then
				# to delete
				if git rev-parse $remoteref > /dev/null 2>&1
				then
					echo ":refs/heads/$ref"
				fi
			else
				if test ${sha1#+} != "$(git rev-parse $remoteref 2> /dev/null)"
				then
					echo "$sha1:refs/heads/$ref"
				fi
			fi
		done)
	test -z "$pushopts" && continue
	git push $name $pushopts ||
	add_error "Could not push to $url"
done

# Maybe error out

test -z "$errors" || {
	printf "\n\nErrors:\n%s\n" "$errors" >&2
	exit 1
}
