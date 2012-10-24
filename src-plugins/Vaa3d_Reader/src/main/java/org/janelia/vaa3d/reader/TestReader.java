package org.janelia.vaa3d.reader;

import fiji.Debug; // requires fiji-lib as a dependency

/**
 * Class for testing and debugging v3draw image loading plugin.
 * 
 * Based on example from
 * http://albert.rierol.net/imagej_programming_tutorials.html#How%20to%20integrate%20a%20new%20file%20format%20reader%20and%20writer
 * 
 * @author Christopher M. Bruns
 *
 */
class TestReader {
	public static void main(String[] args) {
		Debug.run("Open v3draw...", "nonexistent.v3draw");
	}
}
