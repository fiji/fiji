"""
Fiji Python Mode Startup Script
===============================

This script launches Fiji in Python mode, integrating ImageJ2/Fiji with
Python via PyImageJ and JPype. It provides a hybrid environment where:

- Java/Fiji runs in a background thread
- Python runs on the main thread with Qt event loop (on macOS)
- Both GUI systems (AWT/Swing and Qt) can coexist

Architecture Overview
---------------------

1. Argument Parsing
   - Receives JVM path, JVM arguments, main class, and application arguments
   - Configures JPype to use the specified JVM

2. Qt Integration (macOS)
   - On macOS, the CoreFoundation runloop must run on the main thread for AWT/Swing
   - Qt provides this runloop while also enabling Qt-based tools (napari, ndv, etc.)
   - Main thread: Qt event loop
   - Background thread: Fiji/ImageJ initialization and execution

3. ImageJ Initialization
   - Initializes PyImageJ in interactive mode
   - Launches the ImageJ UI and processes command-line arguments
   - Stores global reference (_ij) for access from exception handlers

4. Exception Handling
   - Python level: sys.excepthook and threading.Thread.run patch for better error messages
   - C++/Objective-C level (macOS): std::terminate handler to catch NSExceptions
   - Provides clear, actionable error messages instead of cryptic crashes

Threading Model
---------------

- WITHOUT Qt: Fiji runs on main thread, blocks until quit
- WITH Qt: Main thread runs Qt event loop, Fiji runs in background thread

The background thread approach allows Qt GUI operations to execute on the main
thread (required on macOS) while Fiji/Java code runs separately. This prevents
threading violations that would otherwise crash the application.

Exception Handling Strategy
---------------------------

Three layers of defensive error handling:

1. sys.excepthook: Catches uncaught Python exceptions on main thread
2. Thread.run patch: Catches uncaught Python exceptions in background threads
3. C++ terminate handler: Catches NSExceptions from Qt/Cocoa threading violations

The C++ handler is particularly important on macOS, where creating Qt/Cocoa
widgets on the wrong thread triggers NSInternalInconsistencyException.
Instead of crashing, we provide a helpful error message directing users
to use @ensure_main_thread from the superqt package.
"""

import logging
import os
import sys

import imagej
import scyjava

# Credit: https://stackoverflow.com/a/640431/1207769
from ctypes import POINTER, c_int, cast, pythonapi

from pathlib import Path

__version__ = "2.0.2"

_logger = logging.getLogger(__name__)

_ij = None
_cpp_terminate_callback = None  # Keep reference to prevent garbage collection


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

    # Save a global reference to the ImageJ2 gateway, in case of crash.
    global _ij
    _ij = ij

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
    """Execute a function and log any exceptions without crashing."""
    try:
        do_something()
    except Exception as e:
        _logger.warning(e)


def log_exception(context, exc_type=None, exc_value=None, exc_traceback=None):
    """
    Log an exception with context information.

    Args:
        context: Description of where the exception occurred (e.g., "main thread", "background thread")
        exc_type: Exception type (or None to use sys.exc_info())
        exc_value: Exception value (or None to use sys.exc_info())
        exc_traceback: Exception traceback (or None to use sys.exc_info())
    """
    import traceback

    if exc_type is None:
        exc_type, exc_value, exc_traceback = sys.exc_info()

    _logger.error(f"Unhandled exception in {context}:", exc_info=(exc_type, exc_value, exc_traceback))
    print(f"*** Unhandled exception in {context}:", file=sys.stderr)
    traceback.print_exception(exc_type, exc_value, exc_traceback)


def install_python_exception_handlers():
    """
    Install Python exception handlers for defensive error handling.

    This installs two complementary handlers:

    1. sys.excepthook: Catches uncaught exceptions in the main thread.
       - Primarily a safety net for post-Qt-event-loop exceptions
       - Qt bypasses this for its own exceptions (calls qFatal instead)
       - Unlikely to trigger in normal operation, but provides an extra layer of defense

    2. threading.Thread.run patch: Catches uncaught exceptions in background threads.
       - Critical for the Fiji background thread where Java↔Python calls happen
       - Logs exceptions from Java→Python callbacks that would otherwise be silent
       - Prevents threads from dying without any error message

    Both handlers log the exception with full traceback before propagating/exiting.
    """
    import threading
    import traceback

    # Install sys.excepthook for main thread exceptions.
    original_excepthook = sys.excepthook

    def fiji_excepthook(exc_type, exc_value, exc_traceback):
        """Handle uncaught exceptions in the main thread."""
        log_exception("main thread", exc_type, exc_value, exc_traceback)
        # Call original handler to maintain default behavior.
        original_excepthook(exc_type, exc_value, exc_traceback)

    sys.excepthook = fiji_excepthook

    # Patch threading.Thread.run to catch exceptions in background threads.
    original_thread_run = threading.Thread.run

    def patched_thread_run(self):
        """Handle uncaught exceptions in background threads."""
        try:
            original_thread_run(self)
        except Exception:
            log_exception(f"thread {self.name}")
            # Re-raise to maintain normal thread termination behavior.
            raise

    threading.Thread.run = patched_thread_run


def install_cpp_terminate_handler():
    """
    Install a C++ terminate handler to catch std::terminate() calls.

    This is particularly important on macOS to catch NSExceptions that occur
    when Qt/Cocoa operations are attempted on the wrong thread. Instead of
    crashing with a cryptic error, we provide a clear, actionable message.

    This catches exceptions at the C++/Objective-C level that Python exception
    handlers cannot reach (e.g., NSInternalInconsistencyException from Cocoa).
    """
    import ctypes

    try:
        # Load all symbols in the current process.
        libcxx = ctypes.CDLL(None)

        # Define the terminate handler signature: void (*)()
        TERMINATE_HANDLER = ctypes.CFUNCTYPE(None)

        def cpp_terminate_handler():
            """Handle C++ terminate() calls with a clear error message."""
            # Try to determine what caused terminate() to be called.
            exception_info = "unknown exception"
            is_nsexception = False

            try:
                # Try to get exception type using __cxa_current_exception_type.
                # This is GCC/Clang specific (Itanium C++ ABI).
                cxa_current_exception_type = libcxx.__cxa_current_exception_type
                cxa_current_exception_type.restype = ctypes.c_void_p

                type_info_ptr = cxa_current_exception_type()
                if type_info_ptr:
                    # Get the type name from std::type_info.
                    # type_info.name() is at offset 8 on 64-bit systems.
                    name_ptr = ctypes.c_void_p.from_address(type_info_ptr + 8).value
                    if name_ptr:
                        mangled_name = ctypes.c_char_p(name_ptr).value
                        if mangled_name:
                            exception_info = mangled_name.decode("utf-8")
                            # Check if it's an NSException.
                            if b"NSException" in mangled_name or b"NSInternalInconsistencyException" in mangled_name:
                                is_nsexception = True
            except Exception:
                # If introspection fails, assume it's NSException based on context.
                # We're on macOS with Qt, in a terminate handler - very likely NSException.
                is_nsexception = True

            if is_nsexception:
                error_msg = """
================================================================================
FATAL ERROR: Qt/Cocoa operation attempted on wrong thread
================================================================================

A Qt or Cocoa GUI operation was attempted from a non-main thread on macOS.
This is not allowed and causes the application to crash.

SOLUTION: Ensure Qt/Cocoa operations run on the main thread.

If you're using napari or other Qt-based tools from Fiji scripts, use the
@ensure_main_thread decorator from the 'superqt' package:

    from superqt import ensure_main_thread

    @ensure_main_thread
    def show_data(data):
        napari.imshow(data)

    # Now safe to call from any thread
    show_data(my_array)

For more information, see:
https://github.com/pyapp-kit/superqt

================================================================================
"""
            else:
                error_msg = f"""
================================================================================
FATAL ERROR: Uncaught exception in C++ code
================================================================================

An uncaught exception was thrown in C++ code, causing std::terminate() to be
called. Exception type: {exception_info}

Fiji must exit to prevent further corruption. Please report this issue if you
believe it is a bug.

================================================================================
"""

            print(error_msg, file=sys.stderr)
            _logger.error(f"C++ terminate() called - exception type: {exception_info}")

            # Also tell the user graphically.
            global _ij
            try:
                ui = _ij.ui() if _ij else None
                if ui:
                    ui.showDialog(error_msg, "Fiji")
            except BaseException:
                pass

            # Exit cleanly with a non-zero status code.
            # We can't continue because the JVM may be in an inconsistent state.
            sys.exit(186)  # 186 = gBI = Graphical Broken Interface ;_;

        # Store callback globally to prevent garbage collection.
        # If we don't keep a reference, Python will delete the callback object
        # and the C++ runtime will call a dangling pointer → crash!
        global _cpp_terminate_callback
        _cpp_terminate_callback = TERMINATE_HANDLER(cpp_terminate_handler)

        # Install the terminate handler using std::set_terminate.
        # The symbol is mangled as _ZSt13set_terminatePFvvE on macOS/Linux.
        libcxx._ZSt13set_terminatePFvvE.restype = ctypes.c_void_p
        libcxx._ZSt13set_terminatePFvvE.argtypes = [TERMINATE_HANDLER]
        libcxx._ZSt13set_terminatePFvvE(_cpp_terminate_callback)
        _logger.debug("Installed C++ terminate handler")

    except AttributeError:
        _logger.debug("Could not find std::set_terminate symbol")
    except Exception as e:
        _logger.debug(f"Failed to install C++ terminate handler: {e}")


def main(args):
    """Main entry point for Fiji Python mode."""
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

        # Install defensive Python exception handlers for better error reporting.
        # These handlers deal with exceptions on both main and non-main threads.
        install_python_exception_handlers()

        # On macOS, also install a C++ terminate handler to catch NSExceptions
        # that occur when Qt/Cocoa operations are attempted on the wrong thread.
        if sys.platform == "darwin":
            install_cpp_terminate_handler()

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
