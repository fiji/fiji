#!/bin/sh

# <property
#    name="jnlp"
#    value="http://pacific.mpi-cbg.de/webstart/fiji/plugins/VIB_.jar"/>

mode=current
RELATIVE_PATH="webstart/fiji"
JNLP_NAME="../Fiji.jnlp"
case "$1" in
--updater)
	mode=updater
	RELATIVE_PATH="webstart/fiji-stable"
	JNLP_NAME="../Fiji-stable.jnlp"
	;;
esac

FIJIPATH="/var/www/$RELATIVE_PATH"
CODEBASE="http://pacific.mpi-cbg.de/$RELATIVE_PATH"
EXCLUDES="plugins/Fiji_Updater.jar"

plugins=
jars=
files=
outpath="$FIJIPATH/$JNLP_NAME"

test -d $FIJIPATH ||
mkdir -p $FIJIPATH ||
exit

cd "$(dirname "$0")"/.. ||
exit

printf "" > plugins.config

set_target () {
	target=${1#/var/www/update/} &&
	target=${target%-*[0-9]}
}

# add jars from plugins, jars and misc folder
for jar in $(case "$mode" in
	current)
		find plugins jars -name \*.jar
		;;
	updater)
		./fiji --jar plugins/Fiji_Updater.jar --list-current |
		grep -e '^plugins/' -e '^jars/' |
		sed -n -e 's|^|/var/www/update/|' -e '/\.jar-/p'
		;;
	esac |
	LANG=C sort)
do
	set_target $jar &&
	case " $EXCLUDES " in
	*" $target "*) ;;
	*)
		case "$target" in
		plugins/*)
			content="$(unzip -p "$jar" plugins.config 2> /dev/null)" &&
			printf "# From $target\n\n$content\n\n" >> plugins.config ||
			plugins="$plugins $CODEBASE/$target"
			;;
		esac &&
		files="$files $jar" &&
		jars="$jars $target" || break
		;;
	esac
done || {
	echo "Could not discover .jar files" >&2
	exit 1
}

printf "" > class.map
for jar in $files
do
	set_target $jar &&
	unzip -l -qq $jar |
	sed -n -e 's/\//./g' \
		-e 's|^.*:[0-9][0-9]   \(.*\)\.class$|\1 '$target'|p' \
		>> class.map
done

zip -9r configs.jar plugins.config class.map
files="$files configs.jar"
plugins="$plugins $CODEBASE/configs.jar"

test -e ImageJA/.git/jarsignerrc && (
	cd ImageJA &&
	for jar in $files
	do
		set_target $jar &&
		if test $jar = ${jar#/}
		then
			jar=../$jar
		fi &&
		if test -f $FIJIPATH/$target &&
			test ! $jar -nt $FIJIPATH/$target
		then
			continue
		fi &&
		case "$target" in
		*/*)
			mkdir -p $FIJIPATH/${target%/*}
			;;
		esac &&
		echo "Signing $target..." &&
		jarsigner -signedjar $FIJIPATH/$target $(cat .git/jarsignerrc) \
			$jar dscho || break
	done
) || {
	echo "Could not sign a .jar" >&2
	exit 1
}

rm -f plugins.config configs.jar class.map

plugins=${plugins# }

cat > $outpath << EOF
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE jnlp PUBLIC "-//Sun Microsystems, Inc//DTD JNLP Discriptor 1.1//EN" "http://kano.net/dtd/jnlp-1.5.dtd">
<jnlp spec="1.0+" codebase="$CODEBASE/">

    <information>
	<title>Fiji via Web Start</title>
	<vendor>Fiji development team</vendor>
	<homepage href="http://pacific.mpi-cbg.de/wiki/index.php/Main_Page"/>
	<description>ImageJ based image processing platform</description>
	<icon href="http://pacific.mpi-cbg.de/fiji.png"/>
	<offline-allowed/>
      </information>

    <security>
	<all-permissions/>
    </security>

    <resources>
    	<j2se version="1.5+" initial-heap-size="64m"/>
	<jar href="jars/ij.jar"/>
	<jar href="jars/Fiji.jar" main="true"/>
    	<extension href="http://download.java.net/media/java3d/webstart/release/java3d-latest.jnlp"/>
	<property name="jnlp" value="$plugins"/>
	<property name="jnlp_class_map" value="$CODEBASE/configs.jar"/>
    </resources>

    <application-desc main-class="fiji.Main">
      <argument>-port0</argument>
    </application-desc>
</jnlp>
EOF
