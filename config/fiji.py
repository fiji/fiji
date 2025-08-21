import logging
import os
import sys

import imagej
import scyjava

# Credit: https://stackoverflow.com/a/640431/1207769
from ctypes import POINTER, c_int, cast, pythonapi

from pathlib import Path

__version__ = "2.0.0"

_logger = logging.getLogger(__name__)


def in_interactive_inspect_mode():
    """Whether '-i' option is present or PYTHONINSPECT is not empty."""
    if os.environ.get("PYTHONINSPECT"):
        return True
    iflag_ptr = cast(pythonapi.Py_InteractiveFlag, POINTER(c_int))
    # NOTE: in Python 2.6+ ctypes.pythonapi.Py_InspectFlag > 0
    #      when PYTHONINSPECT set or '-i' is present
    return iflag_ptr.contents.value != 0


def launch_fiji(args):
    # Discern app directory.
    app_dir = Path(__file__).parent.parent.parent

    # Find the divider argument.
    divider = "--"
    try:
        div_index = args.index(divider)
    except AttributeError:
        div_index = -1

    # Validate argument syntax.
    if len(args) < 4 or div_index < 0 or div_index > len(args) - 2:
        print(
            "Usage: fiji.py path-to-libjvm [jvm-arg1 ... jvm-argN] "
            f"{divider} main-class [main-arg1 ... main-argN]"
        )
        sys.exit(1)

    # Parse out the arguments.
    libjvm_path = args[1]
    jvm_args = args[2:div_index]
    main_class = args[div_index + 1]
    main_args = args[div_index + 2 :]

    # Isolate any classpath arguments.
    classpath_prefix = "-Djava.class.path="
    classpath_args = [arg for arg in jvm_args if arg.startswith(classpath_prefix)]
    jvm_args = [arg for arg in jvm_args if arg not in classpath_args]

    # Combine classpaths into a single list.
    classpath = [
        el
        for arg in classpath_args
        for el in arg[len(classpath_prefix) :].split(os.path.pathsep)
    ]

    # Pass the JVM path to JPype.
    p = Path(libjvm_path).absolute()
    jvmpath = str(p)
    scyjava.config.add_kwargs(jvmpath=jvmpath)

    # Set JAVA_HOME to encourage use of the intended JVM,
    # e.g. by Maven when resolving remote artifacts.
    # Assumes the JVM library is nestled beneath a
    # `lib` (Linux/macOS) or `bin` (Windows) folder.
    while p.name != "lib" and p.name != "bin" and p != p.parent:
        p = p.parent
    if p != p.parent:
        os.environ["JAVA_HOME"] = str(p.parent)

    scyjava.config.add_classpath(*classpath)
    scyjava.start_jvm(jvm_args)

    # Unlock reflective access to all modules. Thanks, JPMS!
    try:
        major_version = scyjava.jvm_version()[0]
        if major_version >= 9:
            ReflectionUnlocker = scyjava.jimport("org.scijava.launcher.ReflectionUnlocker")
            ReflectionUnlocker.unlockAll()
    except Exception as e:
        _logger.warning(e)

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
            _logger.warning(e)

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


def maybe_block_until_quit(ij):
    """
    If we're not in interactive mode, we might need
    to block to prevent the process from shutting down.
    """
    if ij and not in_interactive_inspect_mode():
        # Block until the SciJava Context is disposed and/or Java has shut down.
        from time import sleep

        ctx = ij.context()
        disposed = ctx.getClass().getDeclaredField("disposed")
        disposed.setAccessible(True)
        while True:
            try:
                if disposed.get(ctx):
                    break
            except Exception as e:
                if not scyjava.jvm_started():
                    # JVM has already shut down.
                    break
                # Something else went wrong; log it.
                _logger.debug(e)
            sleep(0.1)


def try_to(do_something):
    try:
        do_something()
    except Exception as e:
        _logger.warning(e)


def main(args):
    # Set up logging.
    _debug = (
        "--debug" in args
        or "-Dscijava.log.level=debug" in args
        or os.environ.get("DEBUG")
    )
    logging.basicConfig(
        level=logging.DEBUG if _debug else logging.INFO,
        format="[%(levelname)s:%(name)s] %(message)s",
    )
    if _debug:
        # Activate PyImageJ's debug mode too.
        from imagej import doctor

        doctor.debug_to_stderr()

    # On macOS, the CoreFoundation runloop must run on the main thread. If not,
    # then Java GUI elements (AWT/Swing) won't work when invoked from JPype.
    # One easy way to start a CoreFoundation runloop from Python is with Qt --
    # which also has the advantage of enabling compatibility with Qt-based apps
    # such as napari and ndv.
    try:
        from qtpy.QtCore import Qt
        from qtpy.QtWidgets import QApplication

        use_qt = True
    except Exception as e:
        _logger.debug(e)
        use_qt = False

    if use_qt:
        import threading

        # Configure Qt for macOS before any QApplication creation.
        try_to(lambda: QApplication.setAttribute(Qt.AA_PluginApplication, True))
        try_to(lambda: QApplication.setAttribute(Qt.AA_DisableSessionManager, True))

        # Create QApplication on main thread.
        app = QApplication(args)

        # Prevent Qt from quitting when last Qt window closes; we want Fiji to stay running.
        try_to(lambda: app.setQuitOnLastWindowClosed(False))

        # Launch Fiji in a background thread.
        def run_fiji_in_background():
            ij = launch_fiji(args)
            maybe_block_until_quit(ij)

            # Signal main thread to quit Qt when Fiji closes.
            app.quit()

        fiji_thread = threading.Thread(target=run_fiji_in_background, daemon=False)
        fiji_thread.start()

        # Run Qt event loop on main thread.
        app.exec()

        # Wait for Fiji thread termination.
        if fiji_thread.is_alive():
            fiji_thread.join()

    else:
        # No Qt available; just launch Fiji directly and hope for the best.
        ij = launch_fiji(args)
        maybe_block_until_quit(ij)


if __name__ == "__main__":
    main(sys.argv)
