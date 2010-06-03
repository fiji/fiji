package mpicbg.spim.registration.threshold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.vecmath.Point3d;

import mpicbg.imglib.cursor.LocalizableByDimCursor3D;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.integer.IntType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;

public class ConnectedComponent
{	
	// stores label groups which can contain more than one label
	// each label is stored only once in this list
	public ArrayList <int[]> distinctLabels = new ArrayList<int[]>();
	
	// stores properties of the respective components (which have a label)
	private ArrayList <ComponentProperties> components = null;
	
	private HashMap<Integer, Integer> labelGroups = new HashMap<Integer, Integer>();
	
	// equalizes the labels and counts the number of pixels well as min and max coordinates
	public void equalizeLabels( final Image<IntType> connectedComponents )
	{		
		final int w = connectedComponents.getDimension( 0 );
		final int h = connectedComponents.getDimension( 1 );
		final int d = connectedComponents.getDimension( 2 );

		components = new ArrayList<ComponentProperties>();

		for (int i = 0; i < distinctLabels.size(); i++)
			components.add(new ComponentProperties());
		
		final LocalizableByDimCursor3D<IntType> cursor = (LocalizableByDimCursor3D<IntType>) connectedComponents.createLocalizableByDimCursor();
		
		for (int z = 0; z < d; z++)
		{
			int count = 0;
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
				{
					cursor.setPosition( x, y, z );
					final int pixel = cursor.getType().get();					
					
					if ( pixel > 0 )
					{
						cursor.getType().set( getLabelGroup(pixel) + 1 ); // starts with 0 which is the background per definition
						
						cursor.setPosition( x, y, z );
						ComponentProperties compProp = components.get( cursor.getType().get() - 1 );
						
						compProp.size++;
						
						compProp.label = cursor.getType().get();
						
						if (x < compProp.minX) compProp.minX = x;
						if (y < compProp.minY) compProp.minY = y;
						if (z < compProp.minZ) compProp.minZ = z;

						if (x > compProp.maxX) compProp.maxX = x;
						if (y > compProp.maxY) compProp.maxY = y;
						if (z > compProp.maxZ) compProp.maxZ = z;
					}
					
					count++;
				}
		}
		
		cursor.close();

		//PrintWriter out = fileAccess.openFileWrite("components.txt");
		//out.println("size" + "\t" + "sizeX" + "\t" + "sizeY" + "\t" + "sizeZ");

		for (Iterator <ComponentProperties>i = components.iterator(); i.hasNext(); )
		{
			ComponentProperties compProp = i.next();
			
			compProp.sizeX = compProp.maxX - compProp.minX + 1;
			compProp.sizeY = compProp.maxY - compProp.minY + 1;
			compProp.sizeZ = compProp.maxZ - compProp.minZ + 1;

			//out.println(compProp.size + "\t" + compProp.sizeX + "\t" + compProp.sizeY + "\t" + compProp.sizeZ);
		}
		
		//out.close();					
	}
	
	public ArrayList<ComponentProperties> getBeads( final Image<IntType> connectedComponents, final Image<FloatType> img, 
													final int minSize, final int maxSize, final int minBlackBorder, 
													final boolean useCenterOfMass, final double circularityFactor)
	{
		final int w = connectedComponents.getDimension( 0 );
		final int h = connectedComponents.getDimension( 1 );
		final int d = connectedComponents.getDimension( 2 );
		//OldFloatArray3D spheres = new OldFloatArray3D(connectedComponents.width, connectedComponents.height, connectedComponents.depth);
		//OldFloatArray3D psf = new OldFloatArray3D(21, 21, 21);
		//OldFloatArray3D count = new OldFloatArray3D(21, 21, 21);

		// Remove regions that are do not match the criteria		
		for (int i = 0; i < components.size();)
		{
			ComponentProperties compProp = components.get(i);
			
			//double minVolume = (4.0/3.0) * Math.PI * (compProp.sizeX/2.0)  * (compProp.sizeY/2.0)  * (compProp.sizeZ/2.0);
		
			// to small or too large or Volume too small relative to the bounding box
			if (compProp.size < minSize || compProp.size > maxSize)// || compProp.size < minVolume*circularityFactor)
			{
				components.remove(i);					
			}
			else
			{
				boolean isIsolated = true;
				
				// should not touch the image edges
				if (compProp.minX - minBlackBorder < 0 || compProp.maxX + minBlackBorder >= w ||
					compProp.minY - minBlackBorder < 0 || compProp.maxY + minBlackBorder >= h ||
					compProp.minZ - minBlackBorder < 0 || compProp.maxZ + minBlackBorder >= d )
				{
					isIsolated = false;
				}						

				float countX = 0;
				float countY = 0;
				float countZ = 0;
				
				compProp.center = new Point3d(0,0,0);
				
				final LocalizableByDimCursor3D<FloatType> cursor = (LocalizableByDimCursor3D<FloatType>) img.createLocalizableByDimCursor();
				
				float maxIntensity = -Float.MAX_VALUE;
				Point3d maxCenter = new Point3d();
				
				// and it furthermore has to be isolated from other components				
				for (int z = compProp.minZ - minBlackBorder; z <= compProp.maxZ + minBlackBorder && isIsolated; z++)
					for (int y = compProp.minY - minBlackBorder; y <= compProp.maxY + minBlackBorder; y++)
						for (int x = compProp.minX - minBlackBorder; x <= compProp.maxX + minBlackBorder; x++)
						{
							/*int label = connectedComponents.get(x, y, z); 
							if (label != compProp.label && label != 0)
							{
								isIsolated = false;
								break;
							}
							else*/
							{
								cursor.setPosition(x, y, z);
								float value = cursor.getType().get();

								if (useCenterOfMass)
								{
									compProp.center.x += x * value;								
									compProp.center.y += y * value;
									compProp.center.z += z * value;
									countX += value;
									countY += value;
									countZ += value;
								}
								else
								{
									// select pixel with maximum brightness
									if (value > maxIntensity)
									{
										maxIntensity = value;
										maxCenter.x = x;
										maxCenter.y = y;
										maxCenter.z = z;
									}
								}

							}
						}
				
				cursor.close();
				
				if (!isIsolated)
				{
					components.remove(i);
				}
				else
				{
					if (useCenterOfMass)
					{
						compProp.center.x /= countX;//(compProp.maxX - compProp.minX)/2.0f + compProp.minX;
						compProp.center.y /= countY;//(compProp.maxY - compProp.minY)/2.0f + compProp.minY; //(float)countY;
						compProp.center.z /= countZ;//(compProp.maxZ - compProp.minZ)/2.0f + compProp.minZ; //(float)countZ;
					}
					else
					{
						compProp.center.x = maxCenter.x;
						compProp.center.y = maxCenter.y;
						compProp.center.z = maxCenter.z;
						
						// create the PSF
						/*int minX = compProp.minX - 7;
						int maxX = compProp.maxX + 7 + 1;
						int minY = compProp.minY - 7;
						int maxY = compProp.maxY + 7 + 1;
						int minZ = compProp.minZ - 7;
						int maxZ = compProp.maxZ + 7 + 1;
						
						IOFunctions.println( minX + " " + maxX );
						IOFunctions.println( minY + " " + maxY );
						IOFunctions.println( minZ + " " + maxZ );
						
						if (maxX - minX >= psf.width || maxY - minY >= psf.height || maxZ - minZ >= psf.height)
							break;

						IOFunctions.println( compProp.minX + " " + compProp.maxX );
						IOFunctions.println( compProp.minY + " " + compProp.maxY );
						IOFunctions.println( compProp.minZ + " " + compProp.maxZ );

						for (int z = compProp.minZ - 7; z <= compProp.maxZ + 7; z++)
							for (int y = compProp.minY - 7; y <= compProp.maxY + 7; y++)
								for (int x = compProp.minX - 7; x <= compProp.maxX + 7; x++)
								{
									int xs = psf.width/2 + (x - Math.round((float)compProp.center.x));
									int ys = psf.height/2 + (y - Math.round((float)compProp.center.y));
									int zs = psf.depth/2 + (z - Math.round((float)compProp.center.z));
									
									try
									{
										psf.set(psf.get(xs, ys, zs) + img.get(x, y, z), xs, ys, zs);
										count.set(count.get(xs, ys, zs) + 1, xs, ys, zs);
									}
									catch (Exception e)
									{
										IOFunctions.println(xs + " " + ys + " " + zs);
										IOFunctions.println(x + " " + y + " " + z);
										System.exit(0);
									}
								}
						System.exit(0);*/	
					}
					
					//IOFunctions.println(compProp.label + ": " + compProp.center);

					//ImageFilter.addGaussianSphere(spheres, null, 1.0f, (float)compProp.center.x, (float)compProp.center.y, (float)compProp.center.z, 4.0f, 10, false);						
					i++;
				}
			}
		}	

		/*for (int i = 0; i < psf.data.length; i++)
			if (count.data[i] > 1)
				psf.data[i] /= (float)(count.data[i]);
		
		ImageArrayConverter.FloatArrayToStack(psf, "psf", 0, 0).show();
		
		
		int a = 1;
		do
		{
			try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		while(1 == a);*/
		
		//FloatArrayToStack(spheres, "segemented beads", 0, 0).show();
		
		return components;
	}
	
	public synchronized void addLabel(int label)
	{
		Iterator <int[]>i = distinctLabels.iterator();
		
		while (i.hasNext())
		{
			int[] labelGroup = i.next();
			
			for (int knownLabel : labelGroup)
				if (knownLabel == label)
					return;
		}
		
		// we have a new group of labels which contains one label
		distinctLabels.add(new int[]{label});
		
		// this label is in group "distinctLabels.size() - 1"
		labelGroups.put(label, distinctLabels.size() - 1);
	}
	
	// this method cannot be called twice at a time, it would mess up the order in distinctlabels
	public synchronized void addEqualLabels(int label1, int label2)
	{
		// get the label group for both labels
		int group1 = getLabelGroup(label1);
		int group2 = getLabelGroup(label2);
		
		// if they are not in the same group already we merge both groups
		if (group1 != group2)
		{
			int[] labelGroup1 = distinctLabels.get(group1);
			int[] labelGroup2 = distinctLabels.get(group2);				
				
			int[] newGroup = new int[labelGroup1.length + labelGroup2.length];
			
			for (int i = 0; i < labelGroup1.length; i++)
				newGroup[i] = labelGroup1[i];

			for (int i = 0; i < labelGroup2.length; i++)
				newGroup[i + labelGroup1.length] = labelGroup2[i];
			
			if (group2 > group1)
			{
				distinctLabels.remove(group2);
				distinctLabels.remove(group1);
			}
			else
			{
				distinctLabels.remove(group1);
				distinctLabels.remove(group2);					
			}
			
			distinctLabels.add(newGroup);	
			
			// labelGroups has to know all new positions of the labels (order in arraylist changed!) 
			Iterator <int[]>i = distinctLabels.iterator();
			
			int group = 0;
			while (i.hasNext())
			{
				int[] labelGroup = i.next();
				
				for (int label : labelGroup)
					labelGroups.put(label, group);
				
				group++;
			}								
		}
	}
	
	private int getLabelGroup(int label)
	{
		/*Iterator <int[]>i = distinctLabels.iterator();
		
		int count = 0;
		int result = -1;
		while (i.hasNext() && result == -1)
		{
			int[] labelGroup = i.next();
			
			for (int knownLabel : labelGroup)
				if (knownLabel == label)
					result = count;
			
			count++;
		}
		
		//IOFunctions.printErr("EqualLabel.getLabelGroup(): Label " +  label +  " not found!");
		//return -1;
		*/
		
		Integer group = labelGroups.get(label);
		
		if (group == null)			
		{				
			IOFunctions.printErr("EqualLabel.getLabelGroup(): Label " +  label +  " not found!");
			return -1;
		}
		else
		{
			return group; 
		}
	}		
}