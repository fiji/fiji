#!/bin/sh

# <property
#    name="jnlp"
#    value="http://pacific.mpi-cbg.de/webstart/fiji/plugins/VIB_.jar"/>

RELATIVE_PATH="webstart/test/fiji"
FIJIPATH="/var/www/$RELATIVE_PATH"
CODEBASE="http://pacific.mpi-cbg.de/$RELATIVE_PATH"
JNLP_NAME="Fiji.jnlp"
EXCLUDES=" plugins/ij-ImageIO_.jar jars/jpedalSTD.jar jars/itext-1.3.jar "

plugins=
jars=
outpath="$FIJIPATH/../$JNLP_NAME"

test -d $FIJIPATH || exit

cd "$(dirname "$0")"/..

printf "" > plugins.config

# add jars from plugins, jars and misc folder
for i in $(find plugins jars -name \*.jar)
do
    case " $EXCLUDES " in
    *" $i "*) ;;
    *)
	case "$i" in
	plugins/*)
		content="$(unzip -p "$i" plugins.config 2> /dev/null)" &&
		printf "# From $i\n\n$content\n\n" >> plugins.config ||
		plugins="$plugins $CODEBASE/$i"
		;;
	esac &&
	jars="$jars $i" || break
    ;;
    esac
done || {
	echo "Could not discover .jar files" >&2
	exit 1
}

printf "" > class.map
for jar in $jars
do
	unzip -l -qq $jar |
	sed -n -e 's/\//./g' \
		-e 's|^.*:[0-9][0-9]   \(.*\)\.class$|\1 '$jar'|p' \
		>> class.map
done

zip -9r configs.jar plugins.config class.map
files="$jars configs.jar"
plugins="$plugins $CODEBASE/configs.jar"

test -e ImageJA/.jarsignerrc && (
	cd ImageJA &&
	for jar in $files
	do
		case "$jar" in
		*/*)
			mkdir -p $FIJIPATH/${jar%/*}
			;;
		esac &&
		jarsigner -signedjar $FIJIPATH/$jar $(cat .jarsignerrc) \
			../$jar dscho || break
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
