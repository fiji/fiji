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

import java.awt.Image;

import sc.fiji.compat.FijiTools;

/**
 * Main entry point into Fiji.
 * 
 * @author Johannes Schindelin
 * @deprecated Use {@link net.imagej.Main} instead.
 */
@Deprecated
public class Main {
	protected Image icon;
	protected boolean debug;

	static {
		new IJ1Patcher().run();
	}

	public static void installRecentCommands() {
		FijiTools.runPlugInGently("fiji.util.Recent_Commands", "install");
	}
}
