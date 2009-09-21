#!/bin/sh

compile () {
	git reset --hard $1 &&
	# make sure that the cross compilers are not removed
	for d in root-x86_64-pc-linux chroot-dapper-i386 livecd
	do
		test ! -d $d ||
		git update-index --add --cacheinfo \
			160000 1234567890123456789012345678901234567890 $d ||
		break
	done &&
	git clean -q -x -d -f &&
	# remove empty directories
	for d in $(git ls-files --other --directory)
	do
		rm -r $d || break
	done &&
	git reset &&
	./Build.sh
}

case "$1" in
--stdout)
	case "$(basename "$(cd "$(dirname "$0")"/.. && pwd)")" in
	nightly-build) ;; # okay
	*)
		echo "Refusing to run outside nightly-build/" >&2
		exit 1
		;;
	esac

	cd "$(dirname "$0")"/..

	compile ${2:-HEAD}
	;;
'')
	case "$(basename "$(cd "$(dirname "$0")"/.. && pwd)")" in
	nightly-build) ;; # okay
	*)
		exec "$0" HEAD
		;;
	esac

	cd "$(dirname "$0")"/..

	EMAIL=fiji-devel@googlegroups.com
	TMPFILE=.git/build.$$.out

	# make sure that JAVA_HOME is set to Java5
	export JAVA_HOME="$(ls -d "$(pwd)"/java/*/jdk1.5*)"

	(git fetch origin &&
	 compile origin/master) > $TMPFILE 2>&1  &&
	rm $TMPFILE || {
		mail -s "Fiji nightly build failed" \
			-a "Content-Type: text/plain; charset=UTF-8" \
			$EMAIL < $TMPFILE
		echo Failed: see $TMPFILE
	}
	;;
*)
	test -d nightly-build ||
	git clone . nightly-build &&
	cd nightly-build &&
	if test -z "$(find java -maxdepth 3 -type f)"
	then
		export JAVA_HOME=$(../fiji --print-java-home)
	fi &&
	git fetch .. "$1" &&
	compile FETCH_HEAD
	;;
esac
