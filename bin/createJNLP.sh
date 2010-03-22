#!/bin/sh

# <property 
#    name="jnlp" 
#    value="http://pacific.mpi-cbg.de/webstart/fiji/plugins/VIB_.jar"/>

FIJIPATH="/var/www/webstart/fiji"
CODEBASE="http://pacific.mpi-cbg.de/webstart/fiji"
JNLP_NAME="Fiji.jnlp"
EXCLUDES=" plugins/ij-ImageIO_.jar jars/jpedalSTD.jar jars/itext-1.3.jar "

plugins=""
jars=""
outpath="$FIJIPATH/../$JNLP_NAME"
cwd=$(pwd)

test -d $FIJIPATH || exit

# add jars from plugins folder
cd $FIJIPATH
for i in $(find plugins -name \*.jar); do
    case " $EXCLUDES " in
    *" $i "*) ;;
    *)
        plugins="$plugins $CODEBASE/$i";
        jars="$jars\n<jar href=\"$i\"/>"
    ;;
    esac
done

# add jars from jars folder
for i in $(find jars -name \*.jar); do
    case " $EXCLUDES " in
    *" $i "*) ;;
    *)
        plugins="$plugins $CODEBASE/$i";
        jars="$jars\n<jar href=\"$i\"/>"
    ;;
    esac
done
cd $cwd

plugins=$(echo $plugins | sed -e 's/^ //');

echo '<?xml version="1.0" encoding="utf-8"?>
<!-- Test Deployment -->

<jnlp spec="1.0+"
    codebase="'$CODEBASE'/"
    href="../'$JNLP_NAME'">

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
    	<extension href="http://download.java.net/media/java3d/webstart/release/java3d-latest.jnlp"/>
    	<property name="jnlp" value="'$plugins'"/>
    </resources>

    <application-desc main-class="ij.ImageJ">
    </application>
</jnlp>
' > $outpath

echo '
<?xml version="1.0" encoding="utf-8"?>
<jnlp spec="1.0+"
    codebase="'$CODEBASE'/"
    href="../jpedal.jnlp">
    <information>
        <title>JavaHelp</title>
        <vendor>Sun Microsystems, Inc.</vendor>
        </information>
    <resources>
        <jar href="jars/jpedalSTD.jar"/>
        <jar href="jars/itext-1.3.jar"/>
    </resources>
    <component-desc/>
</jnlp> 
' > $FIJIPATH/../jpedal.jnlp

