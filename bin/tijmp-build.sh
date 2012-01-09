#!/bin/sh

cd "$(dirname "$0")"/..

JAVA_HOME="$(./ImageJ --print-java-home 2>&1)" &&
case "$1" in
mingw*)
	JAVA_HOME_BUILD=$JAVA_HOME &&
	JAVA_HOME="$(cd ../java/win32/* && pwd)" &&
	PATH=$HOME/mingw32/bin:"$JAVA_HOME_BUILD"/bin:$PATH &&
	export PATH JAVA_HOME &&
	LDFLAGS='-no-undefined -Wl,--kill-at' &&
	CONFIGUREOPTIONS='--host=mingw32' &&
	SO=libtijmp-0.dll &&
	SOTARGET=tijmp.dll
	;;
'')
	export JAVA_HOME &&
	LDFLAGS="-module" &&
	SO=libtijmp.so &&
	SOTARGET=tijmp.so
	;;
*)
	echo "Unknown platform: $1"
	exit 1
	;;
esac &&
if test ! -d tijmp
then
	git clone git://fiji.sc/tijmp
fi &&
(cd tijmp &&
 if ! grep no-undefined src/Makefile.am 2>/dev/null > /dev/null
 then
	sed -i.bak -e "s/\(LDFLAGS = \).*/\1$LDFLAGS/" src/Makefile.am &&
	sed -i.bak -e 's/cygwin\*)/cygwin*|mingw*)/' configure.ac &&
	libtoolize --force &&
	aclocal &&
	automake --add-missing --copy &&
	autoconf
 fi &&
 ./configure $CONFIGUREOPTIONS &&
 make &&
 cp src/.libs/$SO $SOTARGET
) || {
	echo "Sorry, tijmp could not be compiled"
	exit 1
}
