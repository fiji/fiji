import os
import sys

import imagej
import scyjava

from pathlib import Path


# Credit: https://stackoverflow.com/a/640431/1207769
from ctypes import POINTER, c_int, cast, pythonapi
def in_interactive_inspect_mode():
    """Whether '-i' option is present or PYTHONINSPECT is not empty."""
    if os.environ.get('PYTHONINSPECT'): return True
    iflag_ptr = cast(pythonapi.Py_InteractiveFlag, POINTER(c_int))
    #NOTE: in Python 2.6+ ctypes.pythonapi.Py_InspectFlag > 0
    #      when PYTHONINSPECT set or '-i' is present
    return iflag_ptr.contents.value != 0


def launch_fiji():
    # Discern app directory.
    app_dir = Path(__file__).parent.parent.parent

    # Find the divider argument.
    divider = "--"
    try:
        div_index = sys.argv.index(divider)
    except AttributeError:
        div_index = -1

    # Validate argument syntax.
    if len(sys.argv) < 4 or div_index < 0 or div_index > len(sys.argv) - 2:
        print(
            "Usage: fiji.py path-to-libjvm [jvm-arg1 ... jvm-argN] "
            f"{divider} main-class [main-arg1 ... main-argN]"
        )
        sys.exit(1)

    # Parse out the arguments.
    libjvm_path = sys.argv[1]
    jvm_args = sys.argv[2:div_index]
    main_class = sys.argv[div_index+1]
    main_args = sys.argv[div_index+2:]

    # Isolate any classpath arguments.
    classpath_prefix = "-Djava.class.path="
    classpath_args = [arg for arg in jvm_args if arg.startswith(classpath_prefix)]
    jvm_args = [arg for arg in jvm_args if arg not in classpath_args]

    # Combine classpaths into a single list.
    classpath = [
        el
        for arg in classpath_args
        for el in arg[len(classpath_prefix):].split(os.path.pathsep)
    ]

    # Pass the JVM path to JPype.
    p = Path(libjvm_path).absolute()
    jvmpath = str(p)
    scyjava.config.add_kwargs(jvmpath=jvmpath)

    # Set JAVA_HOME to encourage use of the intended JVM,
    # e.g. by Maven when resolving remote artifacts.
    # Assume the JVM library is nestled beneath a
    # `lib` (Linux/macOS) or `bin` (Windows) folder.
    while p.name != "lib" and p.name != "bin" and p != p.parent:
        p = p.parent
    os.environ["JAVA_HOME"] = str(p.parent)

    # If we are in debug mode, activate PyImageJ's debug mode too.
    debug = "-Dscijava.log.level=debug" in jvm_args
    if debug:
        from imagej import doctor
        doctor.debug_to_stderr()

    scyjava.config.add_classpath(*classpath)
    scyjava.start_jvm(jvm_args)

    if main_class != "org.scijava.launcher.ClassLauncher":
        # Launching with an alternate main class; stop here.
        MainClass = scyjava.jimport(main_class)
        MainClass.main(main_args)
        return None

    # Do early startup actions: splash screen and java check.

    def tryTo(f):
        try:
            f()
        except Exception as e:
            if debug:
                print(scyjava.jstacktrace(e))

    Splash = scyjava.jimport("org.scijava.launcher.Splash")
    tryTo(lambda: Splash.show())

    Java = scyjava.jimport("org.scijava.launcher.Java")
    tryTo(lambda: Java.check())

    System = scyjava.jimport("java.lang.System")
    appName = str(System.getProperty("scijava.app.name") or "Fiji")
    tryTo(lambda: Splash.update(f"Launching {appName}..."))

    # Initialize ImageJ, wrapping the local Fiji directory.
    # NB: It's OK to pass `interactive` always, because when the
    # --headless flag is given, Fiji still ends up in headless mode.
    ij = imagej.init(app_dir, mode="interactive:force")

    # Sweet HACK ᕦ( ͡° ͜ʖ ͡°)ᕤ
    try:
        appFrame = ij.ui().getDefaultUI().getApplicationFrame()
        appFrame.getComponent().setTitle("(Fiji Is Just) PyImageJ")
    except Exception:
        # Too bad, so sad, we tried.
        pass

    # Perform launch actions (handle CLI args, show UI, etc.).
    ij.launch(main_args)

    return ij


# CRITICAL: Qt must run on main thread on macOS.

from qtpy.QtWidgets import QApplication
from qtpy.QtCore import Qt, QTimer
import threading
import sys

# Configure Qt for macOS before any QApplication creation
QApplication.setAttribute(Qt.AA_MacPluginApplication, True)
QApplication.setAttribute(Qt.AA_PluginApplication, True) 
QApplication.setAttribute(Qt.AA_DisableSessionManager, True)

# Create QApplication on main thread.
app = QApplication(sys.argv)

# Prevent Qt from quitting when last Qt window closes; we want Fiji to stay running.
app.setQuitOnLastWindowClosed(False)

# Launch Fiji in a background thread.
def run_fiji_in_background():
    ij = launch_fiji()

    if ij and not in_interactive_inspect_mode():
        # Block this thread until Fiji is disposed.
        from time import sleep
        ctx = ij.context()
        disposed = ctx.getClass().getDeclaredField("disposed")
        disposed.setAccessible(True)
        while True:
            if disposed.get(ctx):
                break
            sleep(0.1)
    
    # Signal main thread to quit Qt when Fiji closes.
    app.quit()

fiji_thread = threading.Thread(target=run_fiji_in_background, daemon=False)
fiji_thread.start()

# Run Qt event loop on main thread.
app.exec()

# Wait for Fiji cleanup.
if fiji_thread.is_alive():
    fiji_thread.join()
