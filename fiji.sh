#!/bin/sh
dir=$(cd "$(dirname "$0")" && pwd)
"$dir"/python/macos-arm64/bin/python \
"$dir"/config/jaunch/fiji.py \
"$dir"/java/macos-arm64/zulu21.42.19-ca-jdk21.0.7-macosx_aarch64/lib/libjli.dylib \
-XX:+UseG1GC \
--module-path="$dir"/jars/macos-arm64 \
--add-modules=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.nio=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.desktop/sun.awt=ALL-UNNAMED \
--add-opens=java.desktop/javax.swing=ALL-UNNAMED \
--add-opens=java.desktop/sun.swing=ALL-UNNAMED \
--add-opens=java.desktop/com.sun.java.swing=ALL-UNNAMED \
--add-opens=java.desktop/java.awt=ALL-UNNAMED \
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
--add-opens=java.base/java.net=ALL-UNNAMED \
--add-opens=java.base/java.time=ALL-UNNAMED \
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens=java.base/sun.util.calendar=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.collections=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.logging.jfr=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.property=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.property.adapter=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED \
--add-exports=javafx.base/javafx.beans=ALL-UNNAMED \
--add-exports=javafx.base/javafx.beans.binding=ALL-UNNAMED \
--add-exports=javafx.base/javafx.beans.property=ALL-UNNAMED \
--add-exports=javafx.base/javafx.beans.property.adapter=ALL-UNNAMED \
--add-exports=javafx.base/javafx.beans.value=ALL-UNNAMED \
--add-exports=javafx.base/javafx.collections=ALL-UNNAMED \
--add-exports=javafx.base/javafx.collections.transformation=ALL-UNNAMED \
--add-exports=javafx.base/javafx.event=ALL-UNNAMED \
--add-exports=javafx.base/javafx.util=ALL-UNNAMED \
--add-exports=javafx.base/javafx.util.converter=ALL-UNNAMED \
--add-exports=javafx.controls/com.sun.javafx.charts=ALL-UNNAMED \
--add-exports=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
--add-exports=javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED \
--add-exports=javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED \
--add-exports=javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED \
--add-exports=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
--add-exports=javafx.controls/javafx.scene.chart=ALL-UNNAMED \
--add-exports=javafx.controls/javafx.scene.control.cell=ALL-UNNAMED \
--add-exports=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED \
--add-exports=javafx.controls/javafx.scene.control=ALL-UNNAMED \
--add-exports=javafx.fxml/com.sun.javafx.fxml.builder=ALL-UNNAMED \
--add-exports=javafx.fxml/com.sun.javafx.fxml.expression=ALL-UNNAMED \
--add-exports=javafx.fxml/com.sun.javafx.fxml=ALL-UNNAMED \
--add-exports=javafx.fxml/javafx.fxml=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.glass.events=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.glass.ui.delegate=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.glass.utils=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.animation=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.application.preferences=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.beans.event=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.css.parser=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.cursor=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.effect=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.embed=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.font.coretext=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.font.directwrite=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.font.freetype=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.font=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.geom.transform=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.geom=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.geometry=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio.bmp=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio.common=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio.gif=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio.ios=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio.jpeg=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio.png=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.iio=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.image=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.menu=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.perf=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.print=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.runtime.async=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.runtime.eula=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.canvas=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.layout.region=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.layout=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.paint=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.shape=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.text=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.transform=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.text=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.marlin.stats=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.marlin=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.openpisces=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.pisces=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.image=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.j2d.paint=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.j2d.print=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.j2d=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.paint=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.ps=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.shader=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.shape=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.sw=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.scenario.animation.shared=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.scenario.animation=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.scenario.effect.light=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.scenario.effect=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.scenario=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.util.reentrant=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.animation=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.application=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.concurrent=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.css.converter=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.css=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.geometry=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.print=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.canvas=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.effect=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.image=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.input=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.layout=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.paint=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.robot=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.shape=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.text=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.scene.transform=ALL-UNNAMED \
--add-exports=javafx.graphics/javafx.stage=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.javafx.media=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.javafx.scene.media=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia.control=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia.effects=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia.events=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia.locator=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia.logging=ALL-UNNAMED \
--add-exports=javafx.media/com.sun.media.jfxmedia.track=ALL-UNNAMED \
--add-exports=javafx.media/javafx.scene.media=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.java.scene.web=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.fxml.builder.web=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.scene.web=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.scene.web.behavior=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.sg.prism.web=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.webkit.prism=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.webkit.prism.theme=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.webkit=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.javafx.webkit.theme=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.dom=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.event=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.graphics=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.network.about=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.network.data=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.network=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.perf=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.plugin=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.security=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit.text=ALL-UNNAMED \
--add-exports=javafx.web/com.sun.webkit=ALL-UNNAMED \
--add-exports=javafx.web/javafx.scene.web=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.glass.ui.mac=ALL-UNNAMED \
--add-exports=javafx.graphics/com.sun.prism.es2=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
--add-opens=javafx.base/com.sun.javafx.event=ALL-UNNAMED \
--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
--add-opens=javafx.controls/com.sun.javafx.scene.control.inputmap=ALL-UNNAMED \
--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
--add-opens=javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED \
--add-opens=javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED \
-Dpython.cachedir.skip=true \
-Dplugins.dir="$dir" \
-Dimagej.dir="$dir" \
-Dij.dir="$dir" \
-Dfiji.dir="$dir" \
-Dfiji.executable="$dir"/Fiji.app/Contents/MacOS/fiji-macos-arm64 \
-Dij.executable="$dir"/Fiji.app/Contents/MacOS/fiji-macos-arm64 \
-Djava.library.path="$dir"/lib/macos-arm64 \
-Djna.library.path="$dir"/lib/macos-arm64 \
-Dscijava.app.name=Fiji \
-Dscijava.app.directory="$dir" \
-Dscijava.app.splash-image="$dir"/images/icon.png \
-Dscijava.app.java-version-minimum=1.8 \
-Dscijava.app.java-version-recommended=21 \
-Dscijava.app.look-and-feel=com.formdev.flatlaf.FlatLightLaf \
-Dscijava.app.config-file="$dir"/config/jaunch/fiji.cfg \
-Dscijava.app.java-root="$dir"/java/macos-arm64 \
-Dscijava.app.java-links=https://downloads.imagej.net/java/jdk-urls.txt \
-Dscijava.app.java-platform=macos-arm64 \
-Dscijava.context.strict=false \
-Djavafx.allowjs=true \
-Dsun.java2d.uiScale=true \
-Dpython.console.encoding=UTF-8 \
-Dawt.toolkit=gnu.java.awt.peer.qt.QtToolkit \
-Djava.class.path=\
"$dir"/jars/Correct_3D_Drift-1.0.7.jar:\
"$dir"/jars/FilamentDetector-2.0.1.jar:\
"$dir"/jars/JWlz-1.4.0.jar:\
"$dir"/jars/Kappa-2.0.0.jar:\
"$dir"/jars/KymographBuilder-3.0.0.jar:\
"$dir"/jars/OMEVisual-2.0.0.jar:\
"$dir"/jars/ST4-4.3.4.jar:\
"$dir"/jars/T2-NIT-1.1.4.jar:\
"$dir"/jars/T2-TreelineGraph-1.1.4.jar:\
"$dir"/jars/TrackMate-7.14.0.jar:\
"$dir"/jars/VIB-lib-2.2.0.jar:\
"$dir"/jars/VectorGraphics2D-0.13.jar:\
"$dir"/jars/VectorString-3.0.0.jar:\
"$dir"/jars/adapter-rxjava2-2.9.0.jar:\
"$dir"/jars/ahocorasick-0.2.4.jar:\
"$dir"/jars/aircompressor-0.21.jar:\
"$dir"/jars/alphanumeric-comparator-1.4.1.jar:\
"$dir"/jars/animal-sniffer-annotations-1.23.jar:\
"$dir"/jars/annotations-13.0.jar:\
"$dir"/jars/ant-1.10.15.jar:\
"$dir"/jars/ant-launcher-1.10.15.jar:\
"$dir"/jars/antlr-3.5.3.jar:\
"$dir"/jars/antlr-runtime-3.5.3.jar:\
"$dir"/jars/antlr.antlr-2.7.7.jar:\
"$dir"/jars/api-0.14.0.jar:\
"$dir"/jars/api-common-2.37.1.jar:\
"$dir"/jars/app-launcher-2.3.1.jar:\
"$dir"/jars/appose-0.3.0.jar:\
"$dir"/jars/args4j-2.33.jar:\
"$dir"/jars/asm-9.7.jar:\
"$dir"/jars/asm-analysis-9.7.jar:\
"$dir"/jars/asm-commons-9.7.jar:\
"$dir"/jars/asm-tree-9.7.jar:\
"$dir"/jars/asm-util-9.7.jar:\
"$dir"/jars/auto-value-annotations-1.11.0.jar:\
"$dir"/jars/autocomplete-3.3.1.jar:\
"$dir"/jars/aws-java-sdk-core-1.12.772.jar:\
"$dir"/jars/aws-java-sdk-kms-1.12.772.jar:\
"$dir"/jars/aws-java-sdk-s3-1.12.772.jar:\
"$dir"/jars/base-18.09.0.jar:\
"$dir"/jars/base64-2.3.8.jar:\
"$dir"/jars/batch-processor-0.4.2.jar:\
"$dir"/jars/batik-anim-1.17.jar:\
"$dir"/jars/batik-awt-util-1.17.jar:\
"$dir"/jars/batik-bridge-1.17.jar:\
"$dir"/jars/batik-constants-1.17.jar:\
"$dir"/jars/batik-css-1.17.jar:\
"$dir"/jars/batik-dom-1.17.jar:\
"$dir"/jars/batik-ext-1.17.jar:\
"$dir"/jars/batik-gvt-1.17.jar:\
"$dir"/jars/batik-i18n-1.17.jar:\
"$dir"/jars/batik-parser-1.17.jar:\
"$dir"/jars/batik-script-1.17.jar:\
"$dir"/jars/batik-shared-resources-1.17.jar:\
"$dir"/jars/batik-svg-dom-1.17.jar:\
"$dir"/jars/batik-svggen-1.17.jar:\
"$dir"/jars/batik-util-1.17.jar:\
"$dir"/jars/batik-xml-1.17.jar:\
"$dir"/jars/bcpkix-jdk15on-1.62.jar:\
"$dir"/jars/bcpkix-jdk18on-1.78.1.jar:\
"$dir"/jars/bcprov-jdk15on-1.62.jar:\
"$dir"/jars/bcprov-jdk18on-1.78.1.jar:\
"$dir"/jars/bcutil-jdk18on-1.78.1.jar:\
"$dir"/jars/bigdataviewer-core-10.6.4.jar:\
"$dir"/jars/bigdataviewer-n5-1.0.2.jar:\
"$dir"/jars/bigdataviewer-vistools-1.0.0-beta-36.jar:\
"$dir"/jars/bigdataviewer_fiji-6.4.1.jar:\
"$dir"/jars/bij-1.0.0.jar:\
"$dir"/jars/blas-0.8.jar:\
"$dir"/jars/bounce-0.18.jar:\
"$dir"/jars/bsh-2.1.1.jar:\
"$dir"/jars/bytelist-1.0.15.jar:\
"$dir"/jars/caffeine-2.9.3.jar:\
"$dir"/jars/cdm-core-5.6.0.jar:\
"$dir"/jars/checker-qual-3.48.0.jar:\
"$dir"/jars/classgraph-4.8.162.jar:\
"$dir"/jars/client-0.14.0.jar:\
"$dir"/jars/clojure-1.12.0.jar:\
"$dir"/jars/codemodel-2.6.jar:\
"$dir"/jars/collections-generic-4.01.jar:\
"$dir"/jars/commons-beanutils-1.9.4.jar:\
"$dir"/jars/commons-codec-1.17.1.jar:\
"$dir"/jars/commons-collections-3.2.2.jar:\
"$dir"/jars/commons-collections4-4.5.0-M2.jar:\
"$dir"/jars/commons-compress-1.27.1.jar:\
"$dir"/jars/commons-io-2.17.0.jar:\
"$dir"/jars/commons-lang-2.6.jar:\
"$dir"/jars/commons-lang3-3.17.0.jar:\
"$dir"/jars/commons-logging-1.3.4.jar:\
"$dir"/jars/commons-math-2.2.jar:\
"$dir"/jars/commons-math3-3.6.1.jar:\
"$dir"/jars/commons-pool2-2.12.0.jar:\
"$dir"/jars/commons-text-1.12.0.jar:\
"$dir"/jars/commons-vfs2-2.0.jar:\
"$dir"/jars/compiler-interface-1.3.5.jar:\
"$dir"/jars/conscrypt-openjdk-uber-2.5.2.jar:\
"$dir"/jars/converter-jackson-2.9.0.jar:\
"$dir"/jars/datasets-0.8.1906.jar:\
"$dir"/jars/directories-26.jar:\
"$dir"/jars/dirgra-0.3.jar:\
"$dir"/jars/dl-modelrunner-0.6.2-SNAPSHOT.jar:\
"$dir"/jars/ejml-all-0.41.jar:\
"$dir"/jars/ejml-cdense-0.41.jar:\
"$dir"/jars/ejml-core-0.41.jar:\
"$dir"/jars/ejml-ddense-0.41.jar:\
"$dir"/jars/ejml-dsparse-0.41.jar:\
"$dir"/jars/ejml-fdense-0.41.jar:\
"$dir"/jars/ejml-fsparse-0.41.jar:\
"$dir"/jars/ejml-simple-0.41.jar:\
"$dir"/jars/ejml-zdense-0.41.jar:\
"$dir"/jars/error_prone_annotations-2.32.0.jar:\
"$dir"/jars/f2jutil-0.8.jar:\
"$dir"/jars/failureaccess-1.0.2.jar:\
"$dir"/jars/ffmpeg-6.1.1-1.5.10.jar:\
"$dir"/jars/fiji-2.16.1-SNAPSHOT.jar:\
"$dir"/jars/fiji-lib-2.1.3.jar:\
"$dir"/jars/filters-2.0.235.jar:\
"$dir"/jars/flatlaf-3.5.1.jar:\
"$dir"/jars/fontchooser-2.5.2.jar:\
"$dir"/jars/formats-api-8.3.0.jar:\
"$dir"/jars/formats-bsd-8.3.0.jar:\
"$dir"/jars/formats-gpl-8.3.0.jar:\
"$dir"/jars/gapic-google-cloud-storage-v2-2.34.0-alpha.jar:\
"$dir"/jars/gax-2.54.1.jar:\
"$dir"/jars/gax-grpc-2.54.1.jar:\
"$dir"/jars/gax-httpjson-2.54.1.jar:\
"$dir"/jars/gcc-runtime-0.8.1906.jar:\
"$dir"/jars/gluegen-rt-2.5.0.jar:\
"$dir"/jars/google-api-client-2.7.0.jar:\
"$dir"/jars/google-api-services-cloudresourcemanager-v1-rev20240128-2.0.0.jar:\
"$dir"/jars/google-api-services-storage-v1-rev20240209-2.0.0.jar:\
"$dir"/jars/google-auth-library-credentials-1.27.0.jar:\
"$dir"/jars/google-auth-library-oauth2-http-1.27.0.jar:\
"$dir"/jars/google-cloud-core-2.33.0.jar:\
"$dir"/jars/google-cloud-core-grpc-2.33.0.jar:\
"$dir"/jars/google-cloud-core-http-2.33.0.jar:\
"$dir"/jars/google-cloud-resourcemanager-1.38.0.jar:\
"$dir"/jars/google-cloud-storage-2.34.0.jar:\
"$dir"/jars/google-http-client-1.45.0.jar:\
"$dir"/jars/google-http-client-apache-v2-1.45.0.jar:\
"$dir"/jars/google-http-client-appengine-1.45.0.jar:\
"$dir"/jars/google-http-client-gson-1.45.0.jar:\
"$dir"/jars/google-http-client-jackson2-1.45.0.jar:\
"$dir"/jars/google-http-client-xml-1.45.0.jar:\
"$dir"/jars/google-oauth-client-1.36.0.jar:\
"$dir"/jars/gpars-1.2.1.jar:\
"$dir"/jars/grDevices-0.8.1906.jar:\
"$dir"/jars/graphics-0.8.1906.jar:\
"$dir"/jars/groovy-4.0.23.jar:\
"$dir"/jars/groovy-dateutil-4.0.23.jar:\
"$dir"/jars/groovy-json-4.0.23.jar:\
"$dir"/jars/groovy-jsr223-4.0.23.jar:\
"$dir"/jars/groovy-swing-4.0.23.jar:\
"$dir"/jars/groovy-templates-4.0.23.jar:\
"$dir"/jars/groovy-xml-4.0.23.jar:\
"$dir"/jars/grpc-alts-1.68.0.jar:\
"$dir"/jars/grpc-api-1.68.0.jar:\
"$dir"/jars/grpc-auth-1.68.0.jar:\
"$dir"/jars/grpc-context-1.68.0.jar:\
"$dir"/jars/grpc-core-1.68.0.jar:\
"$dir"/jars/grpc-google-cloud-storage-v2-2.34.0-alpha.jar:\
"$dir"/jars/grpc-googleapis-1.68.0.jar:\
"$dir"/jars/grpc-grpclb-1.68.0.jar:\
"$dir"/jars/grpc-inprocess-1.68.0.jar:\
"$dir"/jars/grpc-netty-shaded-1.68.0.jar:\
"$dir"/jars/grpc-protobuf-1.68.0.jar:\
"$dir"/jars/grpc-protobuf-lite-1.61.1.jar:\
"$dir"/jars/grpc-rls-1.61.1.jar:\
"$dir"/jars/grpc-services-1.68.0.jar:\
"$dir"/jars/grpc-stub-1.68.0.jar:\
"$dir"/jars/grpc-util-1.61.1.jar:\
"$dir"/jars/grpc-xds-1.68.0.jar:\
"$dir"/jars/gson-2.10.1.jar:\
"$dir"/jars/guava-33.3.1-jre.jar:\
"$dir"/jars/httpclient-4.5.14.jar:\
"$dir"/jars/httpcore-4.4.16.jar:\
"$dir"/jars/httpmime-4.5.14.jar:\
"$dir"/jars/httpservices-5.6.0.jar:\
"$dir"/jars/icu4j-75.1.jar:\
"$dir"/jars/ij-1.54p.jar:\
"$dir"/jars/ij1-patcher-1.2.9-SNAPSHOT.jar:\
"$dir"/jars/image4j-0.7.jar:\
"$dir"/jars/imagej-2.16.0.jar:\
"$dir"/jars/imagej-common-2.1.1.jar:\
"$dir"/jars/imagej-deprecated-0.2.0.jar:\
"$dir"/jars/imagej-launcher-6.0.2.jar:\
"$dir"/jars/imagej-legacy-2.0.2-SNAPSHOT.jar:\
"$dir"/jars/imagej-mesh-0.8.2.jar:\
"$dir"/jars/imagej-mesh-io-0.1.2.jar:\
"$dir"/jars/imagej-notebook-0.7.1.jar:\
"$dir"/jars/imagej-ops-2.2.0.jar:\
"$dir"/jars/imagej-plugins-batch-0.1.1.jar:\
"$dir"/jars/imagej-plugins-commands-0.8.2.jar:\
"$dir"/jars/imagej-plugins-tools-0.3.1.jar:\
"$dir"/jars/imagej-plugins-uploader-ssh-0.3.2.jar:\
"$dir"/jars/imagej-plugins-uploader-webdav-0.3.3.jar:\
"$dir"/jars/imagej-scripting-0.8.4.jar:\
"$dir"/jars/imagej-ui-awt-0.3.1.jar:\
"$dir"/jars/imagej-ui-swing-1.2.2.jar:\
"$dir"/jars/imagej-updater-2.0.1.jar:\
"$dir"/jars/imglib2-7.1.4.jar:\
"$dir"/jars/imglib2-algorithm-0.18.0.jar:\
"$dir"/jars/imglib2-algorithm-fft-0.2.1.jar:\
"$dir"/jars/imglib2-algorithm-gpl-0.3.1.jar:\
"$dir"/jars/imglib2-cache-1.0.0-beta-19.jar:\
"$dir"/jars/imglib2-ij-2.0.3.jar:\
"$dir"/jars/imglib2-label-multisets-0.15.1.jar:\
"$dir"/jars/imglib2-realtransform-4.0.4.jar:\
"$dir"/jars/imglib2-roi-0.15.1.jar:\
"$dir"/jars/invokebinder-1.10.jar:\
"$dir"/jars/ion-java-1.0.2.jar:\
"$dir"/jars/istack-commons-runtime-3.0.12.jar:\
"$dir"/jars/itextpdf-5.5.13.4.jar:\
"$dir"/jars/ivy-2.5.2.jar:\
"$dir"/jars/j2objc-annotations-3.0.0.jar:\
"$dir"/jars/j3dcore-1.6.0-scijava-2.jar:\
"$dir"/jars/j3dutils-1.6.0-scijava-2.jar:\
"$dir"/jars/jackrabbit-webdav-2.21.22.jar:\
"$dir"/jars/jackson-annotations-2.18.0.jar:\
"$dir"/jars/jackson-core-2.18.0.jar:\
"$dir"/jars/jackson-databind-2.18.0.jar:\
"$dir"/jars/jackson-dataformat-cbor-2.18.0.jar:\
"$dir"/jars/jackson-dataformat-yaml-2.18.0.jar:\
"$dir"/jars/jackson-jq-1.0.0-preview.20191208.jar:\
"$dir"/jars/jai-codec-1.1.3.jar:\
"$dir"/jars/jai-core-1.1.3.jar:\
"$dir"/jars/jakarta.activation-api-1.2.2.jar:\
"$dir"/jars/jakarta.xml.bind-api-2.3.3.jar:\
"$dir"/jars/jama-1.0.3.jar:\
"$dir"/jars/java-cup-11b-20160615.jar:\
"$dir"/jars/java-sizeof-0.0.5.jar:\
"$dir"/jars/java3d-core-1.7.2.jar:\
"$dir"/jars/java3d-utils-1.7.2.jar:\
"$dir"/jars/javaGeom-0.11.1.jar:\
"$dir"/jars/javacpp-1.5.10.jar:\
"$dir"/jars/javassist-3.30.2-GA.jar:\
"$dir"/jars/javax.annotation-api-1.3.2.jar:\
"$dir"/jars/javax.servlet-api-3.1.0.jar:\
"$dir"/jars/jaxb-runtime-2.3.5.jar:\
"$dir"/jars/jblosc-1.0.1.jar:\
"$dir"/jars/jclipboardhelper-0.1.0.jar:\
"$dir"/jars/jcodings-1.0.58.jar:\
"$dir"/jars/jcommander-1.48.jar:\
"$dir"/jars/jcommon-1.0.24.jar:\
"$dir"/jars/jdatepicker-1.3.2.jar:\
"$dir"/jars/jdom2-2.0.6.1.jar:\
"$dir"/jars/jep-2.4.2.jar:\
"$dir"/jars/jffi-1.3.13-native.jar:\
"$dir"/jars/jffi-1.3.13.jar:\
"$dir"/jars/jfilechooser-bookmarks-0.1.6.jar:\
"$dir"/jars/jfreechart-1.5.5.jar:\
"$dir"/jars/jfreesvg-3.4.3.jar:\
"$dir"/jars/jgoodies-common-1.7.0.jar:\
"$dir"/jars/jgoodies-forms-1.7.2.jar:\
"$dir"/jars/jgrapht-core-1.4.0.jar:\
"$dir"/jars/jgraphx-4.2.2.jar:\
"$dir"/jars/jhdf5-19.04.1.jar:\
"$dir"/jars/jheaps-0.14.jar:\
"$dir"/jars/jhotdraw-7.6.0.jar:\
"$dir"/jars/jitk-tps-3.0.4.jar:\
"$dir"/jars/jline-2.14.6.jar:\
"$dir"/jars/jline-native-3.27.0.jar:\
"$dir"/jars/jline-reader-3.27.0.jar:\
"$dir"/jars/jline-terminal-3.27.0.jar:\
"$dir"/jars/jline-terminal-jna-3.27.0.jar:\
"$dir"/jars/jmespath-java-1.12.772.jar:\
"$dir"/jars/jna-5.14.0.jar:\
"$dir"/jars/jna-platform-5.13.0.jar:\
"$dir"/jars/jnr-a64asm-1.0.0.jar:\
"$dir"/jars/jnr-constants-0.10.4.jar:\
"$dir"/jars/jnr-enxio-0.16.jar:\
"$dir"/jars/jnr-ffi-2.2.16.jar:\
"$dir"/jars/jnr-netdb-1.2.0.jar:\
"$dir"/jars/jnr-posix-3.1.19.jar:\
"$dir"/jars/jnr-unixsocket-0.17.jar:\
"$dir"/jars/jnr-x86asm-1.0.2.jar:\
"$dir"/jars/joda-time-2.13.0.jar:\
"$dir"/jars/jogl-all-2.5.0.jar:\
"$dir"/jars/joml-1.10.8.jar:\
"$dir"/jars/joni-2.2.1.jar:\
"$dir"/jars/jpedalSTD-2.80b11.jar:\
"$dir"/jars/jply-0.2.1.jar:\
"$dir"/jars/jruby-core-9.1.17.0.jar:\
"$dir"/jars/jruby-stdlib-9.4.12.0.jar:\
"$dir"/jars/jsch-0.1.55.jar:\
"$dir"/jars/json-20240303.jar:\
"$dir"/jars/jsoup-1.7.2.jar:\
"$dir"/jars/jsr166y-1.7.0.jar:\
"$dir"/jars/jsr305-3.0.2.jar:\
"$dir"/jars/jtransforms-2.4.jar:\
"$dir"/jars/jung-api-2.0.1.jar:\
"$dir"/jars/jung-graph-impl-2.0.1.jar:\
"$dir"/jars/jxrlib-all-0.2.4.jar:\
"$dir"/jars/jython-slim-2.7.4.jar:\
"$dir"/jars/jzlib-1.1.3.jar:\
"$dir"/jars/kotlin-stdlib-1.9.22.jar:\
"$dir"/jars/kotlin-stdlib-common-1.8.22.jar:\
"$dir"/jars/kotlin-stdlib-jdk7-1.9.22.jar:\
"$dir"/jars/kotlin-stdlib-jdk8-1.9.22.jar:\
"$dir"/jars/kryo-5.6.0.jar:\
"$dir"/jars/labkit-pixel-classification-0.1.18.jar:\
"$dir"/jars/labkit-ui-0.4.0.jar:\
"$dir"/jars/languagesupport-3.3.0.jar:\
"$dir"/jars/lapack-0.8.jar:\
"$dir"/jars/legacy-imglib1-1.1.11.jar:\
"$dir"/jars/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar:\
"$dir"/jars/logback-classic-1.2.12.jar:\
"$dir"/jars/logback-core-1.2.12.jar:\
"$dir"/jars/lz4-java-1.9-inv.jar:\
"$dir"/jars/markdownj-0.3.0-1.0.2b4.jar:\
"$dir"/jars/maven-scm-api-1.4.jar:\
"$dir"/jars/maven-scm-provider-svn-commons-1.4.jar:\
"$dir"/jars/maven-scm-provider-svnexe-1.4.jar:\
"$dir"/jars/mbknor-jackson-jsonschema_2.12-1.0.34.jar:\
"$dir"/jars/metadata-extractor-2.19.0.jar:\
"$dir"/jars/metakit-5.3.9.jar:\
"$dir"/jars/methods-0.8.1906.jar:\
"$dir"/jars/miglayout-core-5.3.jar:\
"$dir"/jars/miglayout-swing-5.3.jar:\
"$dir"/jars/mines-jtk-20151125.jar:\
"$dir"/jars/minimaven-2.2.2.jar:\
"$dir"/jars/minio-5.0.2.jar:\
"$dir"/jars/minlog-1.3.1.jar:\
"$dir"/jars/modulator-1.0.jar:\
"$dir"/jars/mpicbg-1.6.0.jar:\
"$dir"/jars/mtj-1.0.4.jar:\
"$dir"/jars/multiverse-core-0.7.0.jar:\
"$dir"/jars/n5-3.5.1.jar:\
"$dir"/jars/n5-aws-s3-4.3.0.jar:\
"$dir"/jars/n5-blosc-1.1.1.jar:\
"$dir"/jars/n5-google-cloud-5.1.0.jar:\
"$dir"/jars/n5-hdf5-2.2.1.jar:\
"$dir"/jars/n5-ij-4.4.1.jar:\
"$dir"/jars/n5-imglib2-7.0.2.jar:\
"$dir"/jars/n5-universe-2.3.0.jar:\
"$dir"/jars/n5-zarr-1.5.1.jar:\
"$dir"/jars/n5-zstandard-1.0.2.jar:\
"$dir"/jars/nailgun-server-0.9.1.jar:\
"$dir"/jars/nashorn-core-15.6.jar:\
"$dir"/jars/native-lib-loader-2.5.0.jar:\
"$dir"/jars/netlib-java-0.9.3-renjin-patched-2.jar:\
"$dir"/jars/netty-buffer-4.1.73.Final.jar:\
"$dir"/jars/netty-codec-4.1.73.Final.jar:\
"$dir"/jars/netty-common-4.1.73.Final.jar:\
"$dir"/jars/netty-handler-4.1.73.Final.jar:\
"$dir"/jars/netty-resolver-4.1.73.Final.jar:\
"$dir"/jars/netty-tcnative-classes-2.0.46.Final.jar:\
"$dir"/jars/netty-transport-4.1.73.Final.jar:\
"$dir"/jars/object-inspector-0.1.jar:\
"$dir"/jars/objenesis-3.4.jar:\
"$dir"/jars/ojalgo-45.1.1.jar:\
"$dir"/jars/okhttp-4.12.0.jar:\
"$dir"/jars/okio-3.9.1.jar:\
"$dir"/jars/okio-jvm-3.9.1.jar:\
"$dir"/jars/ome-codecs-1.1.1.jar:\
"$dir"/jars/ome-common-6.1.0.jar:\
"$dir"/jars/ome-jai-0.1.5.jar:\
"$dir"/jars/ome-mdbtools-5.3.4.jar:\
"$dir"/jars/ome-poi-5.3.10.jar:\
"$dir"/jars/ome-xml-6.5.0.jar:\
"$dir"/jars/op-finder-0.1.4.jar:\
"$dir"/jars/opencensus-api-0.31.1.jar:\
"$dir"/jars/opencensus-contrib-http-util-0.31.1.jar:\
"$dir"/jars/opencensus-proto-0.2.0.jar:\
"$dir"/jars/opencsv-5.9.jar:\
"$dir"/jars/options-1.4.jar:\
"$dir"/jars/pal-optimization-2.0.1.jar:\
"$dir"/jars/parsington-3.1.0.jar:\
"$dir"/jars/perf4j-0.9.16.jar:\
"$dir"/jars/perfmark-api-0.27.0.jar:\
"$dir"/jars/picocli-4.7.6.jar:\
"$dir"/jars/plexus-utils-4.0.2.jar:\
"$dir"/jars/postgresql-42.7.4.jar:\
"$dir"/jars/prettytime-4.0.1.Final.jar:\
"$dir"/jars/proto-google-cloud-resourcemanager-v3-1.38.0.jar:\
"$dir"/jars/proto-google-cloud-storage-v2-2.34.0-alpha.jar:\
"$dir"/jars/proto-google-common-protos-2.45.1.jar:\
"$dir"/jars/proto-google-iam-v1-1.40.1.jar:\
"$dir"/jars/protobuf-java-4.28.2.jar:\
"$dir"/jars/protobuf-java-util-4.28.2.jar:\
"$dir"/jars/re2j-1.7.jar:\
"$dir"/jars/reactive-streams-1.0.3.jar:\
"$dir"/jars/reflectasm-1.11.9.jar:\
"$dir"/jars/regexp-1.3.jar:\
"$dir"/jars/reload4j-1.2.25.jar:\
"$dir"/jars/renjin-appl-0.8.1906.jar:\
"$dir"/jars/renjin-core-0.8.1906.jar:\
"$dir"/jars/renjin-gnur-runtime-0.8.1906.jar:\
"$dir"/jars/renjin-script-engine-0.8.1906.jar:\
"$dir"/jars/retrofit-2.9.0.jar:\
"$dir"/jars/rhino-1.7.14.jar:\
"$dir"/jars/rsyntaxtextarea-3.5.1.jar:\
"$dir"/jars/rxjava-2.0.0.jar:\
"$dir"/jars/samj-0.0.3-SNAPSHOT.jar:\
"$dir"/jars/samj-BDV-0.0.6-SNAPSHOT.jar:\
"$dir"/jars/scala-asm-9.4.0-scala-1.jar:\
"$dir"/jars/scala-library-2.13.10.jar:\
"$dir"/jars/scala3-compiler_3-3.3.0.jar:\
"$dir"/jars/scala3-interfaces-3.3.0.jar:\
"$dir"/jars/scala3-library_3-3.3.0.jar:\
"$dir"/jars/scifio-0.46.0.jar:\
"$dir"/jars/scifio-bf-compat-4.1.1.jar:\
"$dir"/jars/scifio-cli-0.6.1.jar:\
"$dir"/jars/scifio-hdf5-0.2.2.jar:\
"$dir"/jars/scifio-jai-imageio-1.1.1.jar:\
"$dir"/jars/scifio-labeling-0.3.1.jar:\
"$dir"/jars/scifio-lifesci-0.9.0.jar:\
"$dir"/jars/scifio-ome-xml-0.17.1.jar:\
"$dir"/jars/scijava-common-2.99.2.jar:\
"$dir"/jars/scijava-config-2.0.3.jar:\
"$dir"/jars/scijava-io-http-0.3.0.jar:\
"$dir"/jars/scijava-links-1.0.0.jar:\
"$dir"/jars/scijava-listeners-1.0.0-beta-3.jar:\
"$dir"/jars/scijava-optional-1.0.1.jar:\
"$dir"/jars/scijava-plot-0.2.0.jar:\
"$dir"/jars/scijava-plugins-commands-0.2.5.jar:\
"$dir"/jars/scijava-plugins-platforms-0.3.1.jar:\
"$dir"/jars/scijava-plugins-text-markdown-0.1.3.jar:\
"$dir"/jars/scijava-plugins-text-plain-0.1.4.jar:\
"$dir"/jars/scijava-search-3.0.0.jar:\
"$dir"/jars/scijava-table-1.0.2.jar:\
"$dir"/jars/scijava-ui-awt-0.1.7.jar:\
"$dir"/jars/scijava-ui-swing-1.0.3.jar:\
"$dir"/jars/scijava-vecmath-1.6.0-scijava-2.jar:\
"$dir"/jars/script-editor-1.1.0.jar:\
"$dir"/jars/script-editor-jython-1.1.0.jar:\
"$dir"/jars/script-editor-scala-0.2.1.jar:\
"$dir"/jars/scripting-beanshell-1.0.0.jar:\
"$dir"/jars/scripting-clojure-0.1.6.jar:\
"$dir"/jars/scripting-groovy-1.0.0.jar:\
"$dir"/jars/scripting-java-1.0.0.jar:\
"$dir"/jars/scripting-javascript-1.0.0.jar:\
"$dir"/jars/scripting-jruby-0.3.1.jar:\
"$dir"/jars/scripting-jython-1.0.1.jar:\
"$dir"/jars/scripting-python-0.4.0.jar:\
"$dir"/jars/scripting-renjin-0.2.3.jar:\
"$dir"/jars/scripting-scala-0.3.2.jar:\
"$dir"/jars/serializer-2.7.2.jar:\
"$dir"/jars/service-0.14.0.jar:\
"$dir"/jars/slf4j-api-1.7.36.jar:\
"$dir"/jars/snakeyaml-2.3.jar:\
"$dir"/jars/specification-6.5.0.jar:\
"$dir"/jars/spim_data-2.3.5.jar:\
"$dir"/jars/sqlite-jdbc-3.49.1.0.jar:\
"$dir"/jars/stats-0.8.1906.jar:\
"$dir"/jars/swing-checkbox-tree-1.0.2.jar:\
"$dir"/jars/swing-worker-1.1.jar:\
"$dir"/jars/swingx-1.6.1.jar:\
"$dir"/jars/tasty-core_3-3.3.0.jar:\
"$dir"/jars/threetenbp-1.7.0.jar:\
"$dir"/jars/trakem2-transform-1.0.1.jar:\
"$dir"/jars/trove4j-3.0.3.jar:\
"$dir"/jars/turbojpeg-8.3.0.jar:\
"$dir"/jars/txw2-2.3.5.jar:\
"$dir"/jars/udunits-4.3.18.jar:\
"$dir"/jars/ui-behaviour-2.0.8.jar:\
"$dir"/jars/unsafe-fences-1.0.jar:\
"$dir"/jars/util-interface-1.3.0.jar:\
"$dir"/jars/utils-0.8.1906.jar:\
"$dir"/jars/validation-api-2.0.1.Final.jar:\
"$dir"/jars/vecmath-1.7.2.jar:\
"$dir"/jars/weave_jy2java-2.1.1.jar:\
"$dir"/jars/weka-dev-3.9.6.jar:\
"$dir"/jars/xalan-2.7.2.jar:\
"$dir"/jars/xchart-3.5.4.jar:\
"$dir"/jars/xerbla-0.8.jar:\
"$dir"/jars/xercesImpl-2.12.2.jar:\
"$dir"/jars/xml-apis-1.4.01.jar:\
"$dir"/jars/xml-apis-ext-1.3.04.jar:\
"$dir"/jars/xmlgraphics-commons-2.9.jar:\
"$dir"/jars/xmlunit-1.5.jar:\
"$dir"/jars/xmpcore-6.1.11.jar:\
"$dir"/jars/xpp3-1.1.4c.jar:\
"$dir"/jars/xz-1.10.jar:\
"$dir"/jars/zstd-jni-1.5.6-6.jar:\
"$dir"/jars/linux-arm64/gluegen-rt-2.5.0-natives-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-base-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-controls-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-fxml-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-graphics-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-media-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-swing-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/javafx-web-23.0.2-linux-aarch64.jar:\
"$dir"/jars/linux-arm64/jogl-all-2.5.0-natives-linux-aarch64.jar:\
"$dir"/jars/linux64/ffmpeg-6.1.1-1.5.10-linux-x86_64.jar:\
"$dir"/jars/linux64/gluegen-rt-2.5.0-natives-linux-amd64.jar:\
"$dir"/jars/linux64/javafx-base-23.0.2-linux.jar:\
"$dir"/jars/linux64/javafx-controls-23.0.2-linux.jar:\
"$dir"/jars/linux64/javafx-fxml-23.0.2-linux.jar:\
"$dir"/jars/linux64/javafx-graphics-23.0.2-linux.jar:\
"$dir"/jars/linux64/javafx-media-23.0.2-linux.jar:\
"$dir"/jars/linux64/javafx-swing-23.0.2-linux.jar:\
"$dir"/jars/linux64/javafx-web-23.0.2-linux.jar:\
"$dir"/jars/linux64/jogl-all-2.5.0-natives-linux-amd64.jar:\
"$dir"/jars/macos-arm64/ffmpeg-6.1.1-1.5.10-macosx-arm64.jar:\
"$dir"/jars/macos-arm64/javafx-base-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos-arm64/javafx-controls-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos-arm64/javafx-fxml-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos-arm64/javafx-graphics-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos-arm64/javafx-media-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos-arm64/javafx-swing-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos-arm64/javafx-web-23.0.2-mac-aarch64.jar:\
"$dir"/jars/macos64/ffmpeg-6.1.1-1.5.10-macosx-x86_64.jar:\
"$dir"/jars/macos64/javafx-base-23.0.2-mac.jar:\
"$dir"/jars/macos64/javafx-controls-23.0.2-mac.jar:\
"$dir"/jars/macos64/javafx-fxml-23.0.2-mac.jar:\
"$dir"/jars/macos64/javafx-graphics-23.0.2-mac.jar:\
"$dir"/jars/macos64/javafx-media-23.0.2-mac.jar:\
"$dir"/jars/macos64/javafx-swing-23.0.2-mac.jar:\
"$dir"/jars/macos64/javafx-web-23.0.2-mac.jar:\
"$dir"/jars/macosx/gluegen-rt-2.5.0-natives-macosx-universal.jar:\
"$dir"/jars/macosx/jogl-all-2.5.0-natives-macosx-universal.jar:\
"$dir"/jars/win32/ffmpeg-5.1.2-1.5.8-windows-x86.jar:\
"$dir"/jars/win32/gluegen-rt-2.4.0-rc-20210111-natives-windows-i586.jar:\
"$dir"/jars/win32/jogl-all-2.4.0-rc-20210111-natives-windows-i586.jar:\
"$dir"/jars/win64/ffmpeg-6.1.1-1.5.10-windows-x86_64.jar:\
"$dir"/jars/win64/gluegen-rt-2.5.0-natives-windows-amd64.jar:\
"$dir"/jars/win64/javafx-base-23.0.2-win.jar:\
"$dir"/jars/win64/javafx-controls-23.0.2-win.jar:\
"$dir"/jars/win64/javafx-fxml-23.0.2-win.jar:\
"$dir"/jars/win64/javafx-graphics-23.0.2-win.jar:\
"$dir"/jars/win64/javafx-media-23.0.2-win.jar:\
"$dir"/jars/win64/javafx-swing-23.0.2-win.jar:\
"$dir"/jars/win64/javafx-web-23.0.2-win.jar:\
"$dir"/jars/win64/jogl-all-2.5.0-natives-windows-amd64.jar:\
"$dir"/plugins/3D_Blob_Segmentation-4.0.0.jar:\
"$dir"/plugins/3D_Objects_Counter-2.0.1.jar:\
"$dir"/plugins/3D_Viewer-5.0.0.jar:\
"$dir"/plugins/AnalyzeSkeleton_-3.4.2.jar:\
"$dir"/plugins/Anisotropic_Diffusion_2D-2.0.1.jar:\
"$dir"/plugins/Archipelago_Plugins-0.5.4.jar:\
"$dir"/plugins/Arrow_-2.0.2.jar:\
"$dir"/plugins/Auto_Local_Threshold-1.11.0.jar:\
"$dir"/plugins/Auto_Threshold-1.18.0.jar:\
"$dir"/plugins/BalloonSegmentation_-3.0.1.jar:\
"$dir"/plugins/Bug_Submitter-2.1.1.jar:\
"$dir"/plugins/CPU_Meter-2.0.2.jar:\
"$dir"/plugins/Calculator_Plus-2.0.1.jar:\
"$dir"/plugins/Cell_Counter-3.0.0.jar:\
"$dir"/plugins/Colocalisation_Analysis-3.1.0.jar:\
"$dir"/plugins/Color_Histogram-2.0.7.jar:\
"$dir"/plugins/Color_Inspector_3D-2.5.0.jar:\
"$dir"/plugins/Colour_Deconvolution-3.0.3.jar:\
"$dir"/plugins/CorrectBleach_-2.1.0.jar:\
"$dir"/plugins/Descriptor_based_registration-2.1.8.jar:\
"$dir"/plugins/Dichromacy_-2.1.2.jar:\
"$dir"/plugins/Directionality_-2.3.0.jar:\
"$dir"/plugins/FS_Align_TrakEM2-2.0.4.jar:\
"$dir"/plugins/Feature_Detection-2.0.3.jar:\
"$dir"/plugins/Fiji_Archipelago-2.0.1.jar:\
"$dir"/plugins/Fiji_Developer-2.0.8.jar:\
"$dir"/plugins/Fiji_Package_Maker-2.1.1.jar:\
"$dir"/plugins/Fiji_Plugins-3.2.0.jar:\
"$dir"/plugins/FlowJ_-2.0.1.jar:\
"$dir"/plugins/Graph_Cut-1.0.2.jar:\
"$dir"/plugins/Gray_Morphology-2.3.5.jar:\
"$dir"/plugins/H5J_Loader_Plugin-1.1.5.jar:\
"$dir"/plugins/HDF5_Vibez-1.1.1.jar:\
"$dir"/plugins/Helmholtz_Analysis-2.0.2.jar:\
"$dir"/plugins/IJ_Robot-2.0.1.jar:\
"$dir"/plugins/IO_-4.3.0.jar:\
"$dir"/plugins/Image_5D-2.0.2.jar:\
"$dir"/plugins/Image_Expression_Parser-3.0.1.jar:\
"$dir"/plugins/Interactive_3D_Surface_Plot-3.0.1.jar:\
"$dir"/plugins/IsoData_Classifier-2.0.1.jar:\
"$dir"/plugins/Kuwahara_Filter-2.0.1.jar:\
"$dir"/plugins/KymographBuilder-3.0.0.jar:\
"$dir"/plugins/LSM_Reader-4.1.2.jar:\
"$dir"/plugins/LSM_Toolbox-4.1.2.jar:\
"$dir"/plugins/Lasso_and_Blow_Tool-2.0.2.jar:\
"$dir"/plugins/Linear_Kuwahara-2.0.1.jar:\
"$dir"/plugins/LocalThickness_-5.0.0.jar:\
"$dir"/plugins/MTrack2_-2.0.1.jar:\
"$dir"/plugins/M_I_P-2.0.1.jar:\
"$dir"/plugins/Manual_Tracking-2.1.1.jar:\
"$dir"/plugins/Multi_Kymograph-3.0.1.jar:\
"$dir"/plugins/PIV_analyser-2.0.0.jar:\
"$dir"/plugins/QuickPALM_-1.1.2.jar:\
"$dir"/plugins/RATS_-2.0.2.jar:\
"$dir"/plugins/Reconstruct_Reader-2.0.5.jar:\
"$dir"/plugins/SPIM_Opener-2.0.2.jar:\
"$dir"/plugins/SPIM_Registration-5.0.26.jar:\
"$dir"/plugins/Samples_-2.0.3.jar:\
"$dir"/plugins/Series_Labeler-2.0.1.jar:\
"$dir"/plugins/Siox_Segmentation-1.0.5.jar:\
"$dir"/plugins/Skeletonize3D_-2.1.1.jar:\
"$dir"/plugins/SplineDeformationGenerator_-2.0.2.jar:\
"$dir"/plugins/Stack_Manipulation-2.1.2.jar:\
"$dir"/plugins/Statistical_Region_Merging-2.0.1.jar:\
"$dir"/plugins/Stitching_-3.1.9.jar:\
"$dir"/plugins/Sync_Win-1.7-fiji4.jar:\
"$dir"/plugins/Thread_Killer-2.0.1.jar:\
"$dir"/plugins/Time_Lapse-2.1.1.jar:\
"$dir"/plugins/Time_Stamper-2.1.0.jar:\
"$dir"/plugins/ToAST_-25.0.2.jar:\
"$dir"/plugins/TopoJ_-2.0.1.jar:\
"$dir"/plugins/Trainable_Segmentation-4.0.0.jar:\
"$dir"/plugins/TrakEM2_-2.0.0.jar:\
"$dir"/plugins/TrakEM2_Archipelago-2.0.4.jar:\
"$dir"/plugins/VIB_-4.0.0.jar:\
"$dir"/plugins/Vaa3d_Reader-2.0.3.jar:\
"$dir"/plugins/Vaa3d_Writer-1.0.3.jar:\
"$dir"/plugins/Video_Editing-2.0.1.jar:\
"$dir"/plugins/View5D_-2.5.4.jar:\
"$dir"/plugins/Volume_Calculator-3.0.0.jar:\
"$dir"/plugins/Volume_Viewer-2.01.4.jar:\
"$dir"/plugins/bUnwarpJ_-2.6.13.jar:\
"$dir"/plugins/bigdataviewer_fiji-6.3.0.jar:\
"$dir"/plugins/bigwarp_fiji-9.3.1.jar:\
"$dir"/plugins/bio-formats_plugins-8.3.0.jar:\
"$dir"/plugins/blockmatching_-2.1.4.jar:\
"$dir"/plugins/ij_ridge_detect-1.4.1.jar:\
"$dir"/plugins/level_sets-1.0.2.jar:\
"$dir"/plugins/mpicbg_-1.6.0.jar:\
"$dir"/plugins/n5-viewer_fiji-6.1.2.jar:\
"$dir"/plugins/panorama_-3.0.2.jar:\
"$dir"/plugins/register_virtual_stack_slices-3.0.8.jar:\
"$dir"/plugins/registration_3d-2.0.1.jar:\
"$dir"/plugins/samj-IJ-0.0.3-SNAPSHOT.jar:\
"$dir"/plugins/trakem2_tps-2.0.0.jar:\
"$dir"/plugins/z_spacing-1.1.3.jar \
-Xmx96g \
-- \
org.scijava.launcher.ClassLauncher
