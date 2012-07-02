package QuickPALM;

import ij.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.CurveFitter.*;
import java.awt.*;

public class Reconstruct_Dataset implements PlugIn 
{
	ImagePlus imp;
	ImageProcessor ip;
	
	MyDialogs dg = new MyDialogs();
	MyFunctions f = new MyFunctions();
	MyIO io = new MyIO();
	ReconstructionViewer viewer;

	java.lang.String imagedir = "";

	public void run(String arg) 
	{
		IJ.register(Reconstruct_Dataset.class);
		if (!dg.reconstructDataset()) return;
		
		if (f.ptable.getCounter()==0 || !f.ptable.columnExists(13))
		{
			IJ.error("Not able to detect a valid 'Particles Table', please load one");
			return;
		}
		
		//DirectoryChooser chooser = new DirectoryChooser("Choose directory to save the images on");
		//java.lang.String imagedir = chooser.getDirectory();
		
		viewer = new ReconstructionViewer("PALM/STORM Reconstruction", dg, f);
		//if (!dg.viewer_doSave) viewer.imp.show();
		viewer.imp.show();
		int zcounter=0;
		int fcounter=0;
		
		if (!dg.viewer_do3d && !dg.viewer_doMovie)
		{
			viewer.clear();
			viewer.draw(1, viewer.nframes, viewer.minZ, viewer.maxZ);
			return;
			//imp.show();
			//IJ.save(imp, imagedir+"imgrec.tif"); 
		}

		else if (dg.viewer_do3d && !dg.viewer_doMovie)
		{
			zcounter=0;
			for (double z=viewer.minZ;z<=viewer.maxZ;z+=dg.viewer_zstep)
			{
				viewer.clear();
				viewer.draw(1, viewer.nframes, z-dg.viewer_zstep/2, z+dg.viewer_zstep/2);
				save("imgrec_z"+zfill(zcounter)+".tif");
				zcounter++;
			}
		}

		else if(dg.viewer_doMovie)
		{
			zcounter=0;
			fcounter=0;
			//IJ.log("nframes="+viewer.nframes);
			for (int f=1;f<=viewer.nframes;f++)
			{
				if (f%dg.viewer_update!=0) continue;
				if (!dg.viewer_do3d)
				{
					viewer.clear();
					if (dg.viewer_accumulate==0)
						viewer.draw(1, f, viewer.minZ, viewer.maxZ);
					else
						viewer.draw((int) Math.round(f-dg.viewer_accumulate/2), (int) Math.round(f+dg.viewer_accumulate/2), viewer.minZ, viewer.maxZ);
					save("imgrec_f"+zfill(fcounter)+".tif");
				}
				else
				{
					zcounter=0;
					if (dg.viewer_accumulate==0)
					{
						for (double z=viewer.minZ;z<=viewer.maxZ;z+=dg.viewer_zstep)
						{
							viewer.clear();
							viewer.draw(1, f, z-dg.viewer_zstep/2, z+dg.viewer_zstep/2);
							save("imgrec_z"+zfill(zcounter)+"_f"+zfill(fcounter)+".tif");
							zcounter++;
						}
					}
					else
					{
						for (double z=viewer.minZ;z<=viewer.maxZ;z+=dg.viewer_zstep)
						{
							viewer.clear();
							if (dg.viewer_mergebellow!=0 && z==viewer.minZ)
								viewer.draw((int) Math.round(f-dg.viewer_accumulate/2), (int) Math.round(f+dg.viewer_accumulate/2), -9999999, z+dg.viewer_zstep/2);
							else if (dg.viewer_mergebellow!=0 && (z+dg.viewer_zstep)>viewer.maxZ)
								viewer.draw((int) Math.round(f-dg.viewer_accumulate/2), (int) Math.round(f+dg.viewer_accumulate/2), z-dg.viewer_zstep/2, 9999999);
							else
								viewer.draw((int) Math.round(f-dg.viewer_accumulate/2), (int) Math.round(f+dg.viewer_accumulate/2), z-dg.viewer_zstep/2, z+dg.viewer_zstep/2);
							save("imgrec_z"+zfill(zcounter)+"_f"+zfill(fcounter)+".tif");
							zcounter++;
						}
					}
				}
				fcounter++;
			}
		}
		if (viewer.imp!=null) viewer.imp.close();
		if (zcounter==0) zcounter=1;
		if (fcounter==0) fcounter=1;
		int nframes = zcounter*fcounter;
		//IJ.log("zcounter="+zcounter+" fcounter="+fcounter);
		IJ.run("Image Sequence...", "open="+imagedir+"imgrec_f000000.tif number="+nframes+" starting=1 increment=1 scale=100 file=[.tif] or=[] sort use");
	}
	
	void save(java.lang.String filename)
	{
		if (imagedir == "")
		{
			DirectoryChooser chooser = new DirectoryChooser("Choose directory to save the images on");
			imagedir = chooser.getDirectory();
		}
		IJ.save(imp, imagedir+filename);
	}
	
	java.lang.String zfill(int n)
	{
		java.lang.String txt=""+n;
		while (txt.length()<6)
			txt="0"+txt;
		return txt;
	}
}

