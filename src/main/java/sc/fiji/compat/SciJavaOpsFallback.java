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

package sc.fiji.compat;

import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;

@Plugin(type = PreprocessorPlugin.class)
public class SciJavaOpsFallback extends AbstractPreprocessorPlugin {

	@Parameter
	private UIService ui;

	@Override
	public void process(Module module) {
		for (ModuleItem<?> input : module.getInfo().inputs()) {
			if ("OpEnvironment".equals(input.getType().getSimpleName()) && !module.isInputResolved(input.getName())) {
				// Unresolved OpEnvironment input!
				ui.showDialog(
					"<html>It looks like you are trying to use the <tt>OpEnvironment</tt> from<br>" +
						"<b>SciJava Ops</b>, but it does not appear to be installed.<br>" +
						"For instructions on how to do so, please see the<br>" +
						"<i>Installation</i> page of &lt;<a href=\"https://ops.scijava.org\">ops.scijava.org</a>&gt;.<br><br>" +
						"For help, please post a question on &lt;<a href=\"https://forum.image.sc\">forum.image.sc</a>&gt;.",
					"Fiji", DialogPrompt.MessageType.ERROR_MESSAGE);
				cancel(null);
			}
		}
	}
}
