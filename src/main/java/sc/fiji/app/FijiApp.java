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

package sc.fiji.app;

import org.scijava.app.AbstractApp;
import org.scijava.app.App;
import org.scijava.plugin.Plugin;

/**
 * Application metadata about Fiji.
 * 
 * @author Curtis Rueden
 * @see org.scijava.app.AppService
 */
@Plugin(type = App.class, name = FijiApp.NAME)
public class FijiApp extends AbstractApp {

	public static final String NAME = "Fiji";

	@Override
	public String getTitle() {
		return NAME;
	}

	@Override
	public String getGroupId() {
		return "sc.fiji";
	}

	@Override
	public String getArtifactId() {
		return "fiji";
	}

}
