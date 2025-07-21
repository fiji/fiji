/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2025 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import net.imagej.patcher.LegacyInjector;

import org.scijava.util.AppUtils;
import org.scijava.util.FileUtils;

/**
 * Patch ij.jar using Javassist, handle headless mode, too.
 * 
 * @author Johannes Schindelin
 * @deprecated Use {@code net.imagej:ij1-patcher} instead.
 */
@Deprecated
public class IJ1Patcher implements Runnable {
	private static boolean alreadyPatched;
	static boolean ij1PatcherFound, previousIJ1PatcherFound;

	@Override
	public void run() {
		if (alreadyPatched || "false".equals(System.getProperty("patch.ij1")))
			return;
		try {
			String ijDirProperty = System.getProperty("imagej.dir");
			if (ijDirProperty == null) ijDirProperty = System.getProperty("ij.dir");
			final File jars = ijDirProperty == null ? null : new File(ijDirProperty, "jars");
			if (jars == null || FileUtils.getAllVersions(jars, "imagej-legacy.jar").length > 0) {
				LegacyInjector.preinit();
				ij1PatcherFound = true;
			}
			else if (FileUtils.getAllVersions(jars, "ij-legacy.jar").length > 0) try {
				Thread.currentThread().setContextClassLoader(
						getClass().getClassLoader());
				fallBackToPreviousLegacyEnvironment(ClassPool.getDefault());
			}
			catch (Throwable t) {
				t.printStackTrace();
				throw new NoClassDefFoundError();
			}
			else {
				throw new NoClassDefFoundError();
			}
		} catch (NoClassDefFoundError e) {
			fallBackToPreviousPatcher();
		}
		alreadyPatched = true;
	}

	private void fallBackToPreviousPatcher() {
		try {
			Thread.currentThread().setContextClassLoader(
					getClass().getClassLoader());
			final ClassPool pool = ClassPool.getDefault();

			try {
				fallBackToPreviousLegacyEnvironment(pool);
				return;
			} catch (Throwable t) {
				t.printStackTrace();
				// ignore; fall back to previous patching method
			}

			CtClass clazz = pool.makeClass("fiji.$TransientFijiEditor");
			clazz.addInterface(pool
					.get("imagej.legacy.LegacyExtensions$LegacyEditorPlugin"));
			clazz.addConstructor(CtNewConstructor.make(new CtClass[0],
					new CtClass[0], clazz));
			clazz.addMethod(CtNewMethod.make(
					"public boolean open(java.io.File path) {"
							+ "  return fiji.FijiTools.openFijiEditor(path);"
							+ "}", clazz));
			clazz.addMethod(CtNewMethod
					.make("public boolean create(java.lang.String title, java.lang.String body) {"
							+ "  return fiji.FijiTools.openFijiEditor(title, body);"
							+ "}", clazz));
			clazz.toClass();

			compileAndRun(
					pool,
					"imagej.legacy.LegacyExtensions.setAppName(\"(Fiji Is Just) ImageJ\");"
							+ "imagej.legacy.LegacyExtensions.setIcon(new java.io.File(\""
							+ AppUtils.getBaseDirectory(Main.class)
							+ "/images/icon.png\"));"
							+ "imagej.legacy.LegacyExtensions.setLegacyEditor(new fiji.$TransientFijiEditor());"
							+
							/*
							 * make sure to run some Fiji-specific stuff after
							 * Help>Refresh Menus, e.g. installing all scripts
							 * into the menu
							 */
							"imagej.legacy.LegacyExtensions.runAfterRefreshMenus(new fiji.MenuRefresher());"
							+
							/* make sure that ImageJ2's LegacyInjector runs */
							"imagej.legacy.DefaultLegacyService.preinit();");
			return;
		} catch (NoClassDefFoundError e) {
			// ignore: probably have newer ImageJ2 in class path
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fallBackToPreviousLegacyEnvironment(final ClassPool pool)
			throws NotFoundException, CannotCompileException,
			InstantiationException, IllegalAccessException {
		compileAndRun(
				pool,
				"imagej.patcher.LegacyInjector.preinit();"
						// need to have a matching legacy service
						+ "new imagej.patcher.LegacyEnvironment(getClass().getClassLoader(),"
						+ " java.awt.GraphicsEnvironment.isHeadless());");
		previousIJ1PatcherFound = true;
	}

	static void fallBackToPreviousLegacyEnvironmentMain(final String... args)
			throws SecurityException, NoSuchMethodException,
			ClassNotFoundException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final ClassLoader loader = Thread.currentThread()
				.getContextClassLoader();
		final Method get = loader.loadClass("imagej.patcher.LegacyEnvironment")
				.getMethod("getPatchedImageJ1");
		final Object patched = get.invoke(null);
		final Method main = patched.getClass()
				.getMethod("main", String[].class);
		main.invoke(patched, (Object) args);
	}

	private int counter = 1;

	private void compileAndRun(final ClassPool pool, final String code)
			throws NotFoundException, CannotCompileException,
			InstantiationException, IllegalAccessException {
		CtClass clazz;
		clazz = pool.makeClass("fiji.$TransientFijiPatcher" + counter++);
		clazz.addInterface(pool.get("java.lang.Runnable"));
		clazz.addMethod(CtNewMethod.make("public void run() {" + code + "}",
				clazz));
		Runnable run = (Runnable) clazz.toClass().newInstance();
		run.run();
	}
}
