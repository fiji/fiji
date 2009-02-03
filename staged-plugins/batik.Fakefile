JAVAVERSION=1.5
all <- grantAll batik.jar

MAINCLASS(batik.jar)=org.apache.batik.apps.svgbrowser.Main
batik.jar <- sources/**/*.java \
	resources/ resources/**/* \
	lib/js.jar/ lib/xerces_2_5_0.jar/ lib/pdf-transcoder.jar/ \
	lib/xml-apis-ext.jar/ lib/xalan-2.6.0.jar/ lib/xml-apis.jar/

grantAll[../scripts/grant-all.py batik/resources] <-
