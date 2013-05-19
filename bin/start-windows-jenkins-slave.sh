#!/bin/sh

echo "$@"
set -x

export MSYSGIT_HOME=/c/msysgit
export PATH=$MSYSGIT_HOME/bin:$MSYSGIT_HOME/mingw/bin:$PATH

STARTUP_MENU="$APPDATA/Microsoft/Windows/Start Menu/Programs/Startup"
STARTUP_MENU="$(echo "$STARTUP_MENU" | sed -e 's/^C:/\/c/' -e 's/\\/\//g')"
STARTUP_ENTRY="$STARTUP_MENU/Jenkins-Windows-Slave.lnk"

# make it auto-start
test -f "$STARTUP_ENTRY" || {
	/share/msysGit/create-shortcut.exe --arguments \
		'-x -c "/src/fiji/bin/start-windows-jenkins-slave.sh"' \
		/bin/sh.exe \
		"$STARTUP_ENTRY"
	printf "Installed auto-start entry:\n%s\n" "$STARTUP_ENTRY" >&2
	read a
	exit
}

SYSROOT_BIN=/src/mingw-w64/sysroot/bin
test -x $SYSROOT_BIN/x86_64-w64-mingw32-gcc.exe ||
sh /src/mingw-w64/release-easy.sh
test -x $SYSROOT_BIN/gcc.exe ||
cp $SYSROOT_BIN/x86_64-w64-mingw32-gcc.exe $SYSROOT_BIN/gcc.exe
export PATH=$SYSROOT_BIN:$PATH

export FIJI_HOME=$MSYSGIT_HOME/src/fiji
test -d "$FIJI_HOME" ||
(cd "${FIJI_HOME%/fiji}" &&
 git clone https://github.com/fiji/fiji)
export JAVA_HOME=$FIJI_HOME/java/win64/jdk1.6.0_24
test -d "$JAVA_HOME" ||
(cd "$FIJI_HOME" &&
 ./Build.sh jars/fiji-compat.jar)
export MAVEN_HOME=$FIJI_HOME/bin/apache-maven-3.0.4
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
test -d "$MAVEN_HOME" ||
"$FIJI_HOME"/bin/maven.sh --help

JENKINS_URL=http://jenkins.imagej.net/
NODE_NAME=Windows

cd /bin
test -f slave.jar ||
curl -O $JENKINS_URL/jnlpJars/slave.jar

while true
do
	java -Xdebug \
		-jar ./slave.jar \
		-cp "$FIJI_HOME/jars"/jna*.jar \
		-jnlpUrl $JENKINS_URL/computer/$NODE_NAME/slave-agent.jnlp
	sleep 30
done

echo "This is not supposed to happen"
