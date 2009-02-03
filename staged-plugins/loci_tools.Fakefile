JAVAVERSION=1.5
all <- loci_tools.jar

MAINCLASS(loci_tools.jar)=loci.ome.notes.Notes
CLASSPATH=../jars/jai_imageio.jar
loci_tools.jar <- \
	jar/ome-java.jar/ jar/velocity-dep-1.5.jar/ jar/forms-1.0.4.jar/ \
	jar/mdbtools-java.jar/ jar/poi-loci.jar/ jar/slf4j-jdk14.jar/ \
	jar/bufr-1.1.00.jar/ jar/grib-5.1.03.jar/ jar/netcdf-4.0.jar/ \
	jar/visad-lite.jar/ \
	loci/formats/**/*.java loci/formats/*.txt loci/plugins/**/*.java \
	loci/ome/**/*.java

