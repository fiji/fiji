Setup:

  $ cd "$TESTDIR/../app" && cp jy-linux-x64 fiji-linux-x64 && cp ../configs/fiji.toml jaunch/ && mkdir -p jars plugins && touch jars/foo.jar jars/imagej-launcher-6.0.2.jar plugins/bar.jar

Test that the correct Java program actually tries to run.

  $ ./fiji-linux-x64 2>&1
  Error finding class net/imagej/launcher/ClassLauncher
  [4]

Test command line argument combinations.

  $ ./fiji-linux-x64 --help
  Usage: ./fiji-linux-x64 [<Java options>.. --] [<main arguments>..]
  
  Fiji launcher (Jaunch v* / * / *) (glob)
  Java options are passed to the Java Runtime,
  main arguments to the launched program (Fiji).
  
  In addition, the following options are supported:
  --print-ij-dir
                      print where Fiji thinks it is located
  --ij-dir <path>
                      set the ImageJ directory to <path> (used to find\njars/, plugins/ and macros/)
  --pass-classpath <classpath>
                      pass -classpath <classpath> to the main() method
  --full-classpath
                      call the main class with the full ImageJ class path
  --default-gc
                      do not use advanced garbage collector settings by default\n(-XX:+UseG1GC)
  --gc-g1
                      use the G1 garbage collector
  --debug-gc
                      show debug info about the garbage collector on stderr
  --no-splash
                      suppress showing a splash screen upon startup
  --dont-patch-ij1
                      do not try to runtime-patch ImageJ (implies --ij1)
  --ij2, --imagej
                      no effect -- here for backwards compatibility only
  --ij1
                      start in original ImageJ mode
  --legacy
                      start in legacy/Fiji1 mode
  --allow-multiple
                      do not reuse existing Fiji instance
  --plugins <dir>
                      use <dir> to discover plugins
  --run <plugin> [<arg>]
                      run <plugin> in Fiji, optionally with arguments
  --compile-and-run <path-to-.java-file>
                      compile and run <plugin> in Fiji
  --edit [<file>...]
                      edit the given file in the script editor
  --update
                      start the command-line version of the ImageJ updater
  --main-class <class name>
                      start the given class instead of Fiji
  --jar <jar path>
                      run the given JAR instead of Fiji
  --console, --attach-console
                      attempt to attach output to the calling console
  --new-console
                      ensure the launch of a new, dedicated console for output
  --set-icon <exe-file>,<ico-file>
                      add/replace the icon of the given program
  --freeze-classloader
                      TODO undocumented
  --compile-and-run
                      TODO undocumented
  --showUI
                      TODO undocumented
  --jdb
                      TODO undocumented
  --ijcp
                      TODO undocumented
  --help, -h
                      show this help
  --dry-run
                      show the command line, but do not run anything
  --info
                      informational output
  --debug
                      verbose output
  --system
                      do not try to run bundled Java
  --java-home <path>
                      specify JAVA_HOME explicitly
  --print-java-home
                      print path to the selected Java
  --print-java-info
                      print information about the selected Java
  --print-app-dir
                      print directory where the application is located
  --headless
                      run in text mode
  --heap, --mem, --memory <amount>
                      set Java's heap size to <amount> (e.g. 512M)
  --class-path, --classpath, -classpath, --cp, -cp <path>
                      append <path> to the class path
  --ext <path>
                      set Java's extension directory to <path>
  --debugger <port>[,suspend]
                      start Java in a mode so an IDE/debugger can attach to it

  $ ./fiji-linux-x64 --print-java-home
  /* (glob)

  $ ./fiji-linux-x64 --print-java-info 2>&1 | grep -v '^\* \(IMPLEMENTOR\|java\.\|jdk\.\|sun\.\|user\.\)' | LC_ALL=C sort
  \* JAVA_VERSION=* (glob)
  * OS_ARCH=amd64
  * OS_NAME=Linux
  \* OS_VERSION=* (glob)
  \* SOURCE=* (glob)
  * awt.toolkit=sun.awt.X11.XToolkit
  * file.encoding.pkg=sun.io
  \* file.encoding=* (glob)
  * file.separator=/
  * line.separator=
  * os.arch=amd64
  * os.name=Linux
  \* os.version=* (glob)
  * path.separator=:
  CPU arch: X64
  OS name: LINUX
  distro: * (glob)
  libjvm: /*/libjvm.so (glob)
  release file:
  root: /* (glob)
  system properties:
  version: * (glob)

  $ ./fiji-linux-x64 --dry-run --print-app-dir
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --headless
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --java-home /usr/lib/jvm/default-java
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --headless
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --system --heap 58m
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --class-path /tmp/lions.jar:/tmp/tigers.jar
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --jar-path /tmp/jars:/tmp/other-jars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --debugger 8765,suspend
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --system --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --system --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --system -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --headless
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --heap 58m
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --class-path /tmp/lions.jar:/tmp/tigers.jar
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --jar-path /tmp/jars:/tmp/other-jars
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --debugger 8765,suspend
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --ij-dir /tmp/Fiji.app
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --full-classpath
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --default-gc
  /usr/lib/jvm/default-java/bin/java --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --gc-g1
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --debug-gc
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC -verbose:gc --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --no-splash
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --dont-patch-ij1
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --ij2
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --ij1
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --legacy
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --allow-multiple
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --plugins /tmp/plugins
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --run Fizzbuzz 3,5,6,9,10,12,15
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --compile-and-run /tmp/imagej/Hello.java
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --edit /tmp/scripts/Hello.groovy
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --freeze-classloader
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --showUI
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java --jdb
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --java-home /usr/lib/jvm/default-java -ijcp /tmp/ijjars
  /usr/lib/jvm/default-java/bin/java -XX:+UseG1GC --add-opens=*=ALL-UNNAMED -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --headless --heap 58m
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --class-path /tmp/lions.jar:/tmp/tigers.jar
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --jar-path /tmp/jars:/tmp/other-jars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --debugger 8765,suspend
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --headless -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main -batch (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --class-path /tmp/lions.jar:/tmp/tigers.jar
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --jar-path /tmp/jars:/tmp/other-jars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --debugger 8765,suspend
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/*:*/plugins/bar.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --heap 58m -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Xmx58m -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --jar-path /tmp/jars:/tmp/other-jars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --debugger 8765,suspend
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --class-path /tmp/lions.jar:/tmp/tigers.jar -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar:/tmp/lions.jar:/tmp/tigers.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --debugger 8765,suspend
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jar-path /tmp/jars:/tmp/other-jars -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --ij-dir /tmp/Fiji.app
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debugger 8765,suspend -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -agentlib:jdwp=transport=dt_socket,server=y,address=localhost:8765,suspend -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij-dir /tmp/Fiji.app -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --full-classpath
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --pass-classpath /tmp/apples:/tmp/bananas:/tmp/kumquats -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --default-gc
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --gc-g1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --debug-gc
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --no-splash
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --full-classpath -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/*:*/plugins/bar.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --gc-g1
  /*/bin/java -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --debug-gc
  /*/bin/java -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --no-splash
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --dont-patch-ij1
  /*/bin/java -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --ij2
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --ij1
  /*/bin/java -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --legacy
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --allow-multiple
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --plugins /tmp/plugins
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --edit /tmp/scripts/Hello.groovy
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --freeze-classloader
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --showUI
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc --jdb
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --default-gc -ijcp /tmp/ijjars
  /*/bin/java -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --debug-gc
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --no-splash
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --ij2
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --ij1
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --legacy
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --allow-multiple
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --freeze-classloader
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --showUI
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 --jdb
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --gc-g1 -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -XX:+UseCompressedOops -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:NewRatio=5 -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --no-splash
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --ij2
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --ij1
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --legacy
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --allow-multiple
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --freeze-classloader
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --showUI
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc --jdb
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --debug-gc -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -verbose:gc -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --dont-patch-ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --ij1
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --no-splash -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --ij2
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --ij1
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --legacy
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ -port0 (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --showUI
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 --jdb
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --dont-patch-ij1 -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpatch.ij1=false -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --ij1
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --legacy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij2 -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --legacy
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ -port0 (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --showUI
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 --jdb
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --ij1 -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dsun.java.command=ImageJ -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -classpath jars/ij-*.jar ij.ImageJ (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --allow-multiple
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --legacy -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --plugins /tmp/plugins
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --allow-multiple -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins --run Fizzbuzz 3,5,6,9,10,12,15
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --plugins /tmp/plugins -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=/tmp/plugins -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15 --compile-and-run /tmp/imagej/Hello.java
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15 --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15 --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15 --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15 --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --run Fizzbuzz 3,5,6,9,10,12,15 -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --run Fizzbuzz 3,5,6,9,10,12,15 -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --compile-and-run /tmp/imagej/Hello.java --edit /tmp/scripts/Hello.groovy
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --compile-and-run /tmp/imagej/Hello.java --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --compile-and-run /tmp/imagej/Hello.java --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --compile-and-run /tmp/imagej/Hello.java --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --compile-and-run /tmp/imagej/Hello.java -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher /tmp/imagej/Hello.java -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --edit /tmp/scripts/Hello.groovy --freeze-classloader
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --edit /tmp/scripts/Hello.groovy --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --edit /tmp/scripts/Hello.groovy --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --edit /tmp/scripts/Hello.groovy -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher --edit /tmp/scripts/Hello.groovy -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --freeze-classloader --showUI
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --freeze-classloader --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --freeze-classloader -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --showUI --jdb
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --showUI -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

  $ ./fiji-linux-x64 --dry-run --jdb -ijcp /tmp/ijjars
  /*/bin/java -XX:+UseG1GC -Dpython.cachedir.skip=true -Dplugins.dir=* -Dimagej.splash=true -Dimagej.dir=* -Dij.dir=* -Dfiji.dir=* -Dfiji.executable=/*/fiji-linux-x64 -Dij.executable=/*/fiji-linux-x64 -Djava.library.path=*/lib/linux64 -Dscijava.context.strict=false -Dpython.console.encoding=UTF-8 -Djava.class.path=*/jars/imagej-launcher-6.0.2.jar -Xmx23g net.imagej.launcher.ClassLauncher -ijcp /tmp/ijjars -ijjarpath jars -ijjarpath plugins net.imagej.Main (glob)

