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
files=./ij.jar
outpath="$FIJIPATH/../$JNLP_NAME"

test -d $FIJIPATH || exit

# add jars from plugins, jars and misc folder
for i in $(find plugins jars misc -name \*.jar)
do
    case " $EXCLUDES " in
    *" $i "*) ;;
    *)
        plugins="$plugins $CODEBASE/$i" &&
        jars="$jars\
<jar href=\"$i\"/>" &&
	files="$files $i" || break
    ;;
    esac
done || {
	echo "Could not discover .jar files" >&2
	exit 1
}

test -e ImageJA/.jarsignerrc && (
	cd ImageJA &&
	for jar in $files
	do
		mkdir -p $FIJIPATH/${jar%/*} &&
		jarsigner -signedjar $FIJIPATH/$jar $(cat .jarsignerrc) \
			../$jar dscho || break
	done
) || {
	echo "Could not sign a .jar" >&2
	exit 1
}

plugins=${plugins# }

echo '<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE jnlp PUBLIC "-//Sun Microsystems, Inc//DTD JNLP Discriptor 1.1//EN" "http://kano.net/dtd/jnlp-1.5.dtd">
<jnlp spec="1.0+" codebase="'$CODEBASE'/">

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
        '$jars'
    	<jar href="ij.jar" main="true"/>
    	<extension href="http://download.java.net/media/java3d/webstart/release/java3d-latest.jnlp"/>
    	<property name="jnlp" value="'$plugins'"/>
    </resources>

    <application-desc main-class="fiji.Main"/>
</jnlp>
' > $outpath
