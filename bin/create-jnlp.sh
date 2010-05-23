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
files=
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
		printf "# From $i\n\n$content\n\n" >> plugins.config &&
		jars="$jars $CODEBASE/$i" ||
		plugins="$plugins $CODEBASE/$i"
		;;
	*)
		jars="$jars $CODEBASE/$i"
		;;
	esac &&
	files="$files $i" || break
    ;;
    esac
done || {
	echo "Could not discover .jar files" >&2
	exit 1
}

if test -s plugins.config
then
	zip -9r configs.jar plugins.config
	files="$files configs.jar"
	plugins="$plugins $CODEBASE/configs.jar"
fi

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

rm -f plugins.config configs.jar

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
	<property name="jnlp_jars" value="$jars"/>
    </resources>

    <application-desc main-class="fiji.Main">
      <argument>-port0</argument>
    </application-desc>
</jnlp>
EOF
