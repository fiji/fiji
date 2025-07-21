/*
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

package sc.fiji.compat;

import net.imagej.legacy.plugin.LegacyPostRefreshMenus;

import org.scijava.plugin.Plugin;

/**
 * {@link LegacyPostRefreshMenus} plugin that ensures the
 * {@link sc.fiji.compat.MenuRefresher} runs after the {@code Refresh Menus} command is
 * executed.
 *
 * @author Mark Hiner
 */
@Plugin(type = LegacyPostRefreshMenus.class)
public class DefaultLegacyPostRefreshMenus implements LegacyPostRefreshMenus {

	@Override
	public void run() {
		new MenuRefresher().run();
	}

}
