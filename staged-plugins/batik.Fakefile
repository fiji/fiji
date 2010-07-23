JAVAVERSION=1.5
all <- grantAll batik.jar

MAINCLASS(batik.jar)=org.apache.batik.apps.svgbrowser.Main
batik.jar <- batik-1.8pre/lib/batik-all.jar/ \
	lib/xerces_2_5_0.jar/ lib/pdf-transcoder.jar/ lib/xalan-2.6.0.jar/ \
	lib/xml-apis.jar/ lib/xml-apis-ext.jar/ lib/js.jar/
batik-1.8pre/lib/batik-all.jar[../fiji --ant \
	-Dant.build.javac.source=1.5 -Dant.build.javac.target=1.5 all-jar] <-

grantAll[../bin/grant-all.py batik/resources] <-
