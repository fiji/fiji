/*
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2007 - 2015 Fiji
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

package sc.fiji;

/**
 * Launches Fiji.
 * 
 * @author Curtis Rueden
 * @see net.imagej.Main
 */
public final class Main {

	private Main() {
		// Prevent instantiation of utility class.
	}

	// -- Main method --

	public static void main(final String[] args) {
		// NB: If you set the plugins.dir system property to a valid Fiji
		// installation, you will have access to that installation's scripts
		// and ImageJ 1.x plugins.
		//
		// However, ImageJ1 will prioritize the plugin JARs in the ImageJ
		// installation's plugins folder over the JARs on the classpath!
		System.setProperty("plugins.dir", "/path/to/your/Fiji.app");

		net.imagej.Main.launch(args);
	}

}
