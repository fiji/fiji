package textureByRef;

import voltex.VoltexGroup;
import voltex.VoltexVolume;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;

public class Volume_Editor implements PlugInFilter {
	
	private ImagePlus image;
	private VoltexVolume volume;
	
	public VoltexVolume getVolume() {
		return volume;
	}

	public void run(ImageProcessor arg0) {
		Image3DUniverse univ = new Image3DUniverse(512, 512);
		Content c = univ.addVoltex(image, null, image.getTitle(),
					0, new boolean[] {true, true, true}, 1);
		volume = ((VoltexGroup)c.getContent()).getRenderer().getVolume();
		univ.show();
	}

	public int setup(String arg0, ImagePlus arg1) {
		this.image = arg1;
		return DOES_8G | DOES_RGB;
	}

	public static void main(String[] args) {
		new ij.ImageJ();
		ImagePlus imp = NewImage.createRGBImage(
				"Edit volume", 250, 233, 57, NewImage.FILL_BLACK);
		imp.show();
		Volume_Editor vol = (Volume_Editor)IJ.
				runPlugIn("textureByRef.Volume_Editor", "");
		VoltexVolume volume = vol.getVolume();
		drawSpiral(volume, 128, 128);
	}

	public static void drawSpiral(VoltexVolume v, float cx, float cy) {
		final int circles = 3;
		final int dzPerCircle = 19;
		final int drPerCircle = 40;
		final int stepsPerCircle = 720;
		
		float r = 1;
		float x = cx + 1;
		float y = cy;
		float z = 0;
		float a = 0;
		
		final float daPerStep = 2 * (float)Math.PI / stepsPerCircle;
		final int steps = circles * stepsPerCircle;
		final float dzPerStep = (float)dzPerCircle / stepsPerCircle;
		final float drPerStep = (float)drPerCircle / stepsPerCircle;

		for(int s = 0; s < steps; s++) {
			v.set(Math.round(x), Math.round(y), Math.round(z), 0xffff0000);
			a += daPerStep;
			r += drPerStep;
			x = (float)(cx + r * Math.cos(a));
			y = (float)(cy + r * Math.sin(a));
			z += dzPerStep;
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
