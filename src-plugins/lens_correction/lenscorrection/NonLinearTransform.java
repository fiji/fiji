/**

Copyright (C) 2008 Verena Kaynig.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
**/

/* ****************************************************************  *
 * Representation of a non linear transform by explicit polynomial	 
 * kernel expansion.												
 * 																	
 * TODO:														
 * 	- make different kernels available
 * 	- inverse transform for visualization
 *  - improve image interpolation 				
 *  - apply and applyInPlace should use precalculated transform?
 *    (What about out of image range pixels?)
 *  																
 *  Author: Verena Kaynig						
 *  Kontakt: verena.kaynig@inf.ethz.ch	
 *  
 * ****************************************************************  */

package lenscorrection;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.io.BufferedWriter;
import java.lang.Math;

import Jama.Matrix;

import java.io.*;


public class NonLinearTransform implements mpicbg.trakem2.transform.CoordinateTransform{

		private double[][] beta = null;
		private double[] normMean = null;
		private double[] normVar = null;
		private double[][][] transField = null;
		private int dimension = 0;
		private int length = 0;
		private int width = 0;
		private int height = 0;
		private boolean precalculated = false;
	
		public NonLinearTransform(double[][] b, double[] nm, double[] nv, int d, int w, int h){
				beta = b;
				normMean = nm;
				normVar = nv;
				dimension = d;
				length = (dimension + 1)*(dimension + 2)/2;	
				width = w;
				height = h;
		}
		
		public NonLinearTransform(int d, int w, int h){
				dimension = d;
				length = (dimension + 1)*(dimension + 2)/2;	
		
				beta = new double[length][2];
				normMean = new double[length];
				normVar = new double[length];
		
				for (int i=0; i < length; i++){
						normMean[i] = 0;
						normVar[i] = 1;
				}
		
				width = w;
				height = h;
		}
	
		public NonLinearTransform(){};

		public NonLinearTransform(String filename){
				this.load(filename);
		}

		public NonLinearTransform(double[][] coeffMatrix, int w, int h){
				length = coeffMatrix.length;
				beta = new double[length][2];
				normMean = new double[length];
				normVar = new double[length];
				width = w;
				height = h;
				dimension = (int)(-1.5 + Math.sqrt(0.25 + 2*length));

				for(int i=0; i<length; i++){
						beta[i][0] = coeffMatrix[0][i];
						beta[i][1] = coeffMatrix[1][i];
						normMean[i] = coeffMatrix[2][i];
						normVar[i] = coeffMatrix[3][i];
				}
		}
		

		//implements mpicbg.trakem2
		public void init( String data ) throws NumberFormatException{
				final String[] fields = data.split( " " );
				int c = 0;

				dimension = Integer.parseInt(fields[c]); c++;
				length = Integer.parseInt(fields[c]); c++;

				beta = new double[length][2];
				normMean = new double[length];
				normVar = new double[length];

				if ( fields.length == 4 + 4*length )
						{
								for (int i=0; i < length; i++){
										beta[i][0] = Double.parseDouble(fields[c]); c++;
										beta[i][1] = Double.parseDouble(fields[c]); c++;
								}
								
								System.out.println("c: " + c); 

								for (int i=0; i < length; i++){
										normMean[i] = Double.parseDouble(fields[c]); c++;
								}
								
								System.out.println("c: " + c); 

								for (int i=0; i < length; i++){
										normVar[i] = Double.parseDouble(fields[c]); c++;
								}

								width = Integer.parseInt(fields[c]); c++;				
								height = Integer.parseInt(fields[c]); c++;
								System.out.println("c: " + c); 
								
						}
				else throw new NumberFormatException( "Inappropriate parameters for " + this.getClass().getCanonicalName() );
		}



		public String toXML(final String indent){
				return new StringBuffer(indent).append("<ict_transform class=\"").append(this.getClass().getCanonicalName()).append("\" data=\"").append(toDataString()).append("\"/>").toString();
		}

		public String toDataString(){
				String data = "";
				data += Integer.toString(dimension) + " ";
				data += Integer.toString(length) + " ";

				for (int i=0; i < length; i++){
						data += Double.toString(beta[i][0]) + " ";
						data += Double.toString(beta[i][1]) + " ";
				}

				for (int i=0; i < length; i++){
						data += Double.toString(normMean[i]) + " ";
				}

				for (int i=0; i < length; i++){
						data += Double.toString(normVar[i]) + " ";
				}
				data += Integer.toString(width) + " ";
				data += Integer.toString(height) + " ";

				return data;

		}

		public float[] apply( float[] location ){

				double[] position = {(double) location[0], (double) location[1]};
				double[] featureVector = kernelExpand(position);
				double[] newPosition = multiply(beta, featureVector);

				float[] newLocation = new float[2];
				newLocation[0] = (float) newPosition[0];
				newLocation[1] = (float) newPosition[1];

				return newLocation;
		}

		public void applyInPlace( float[] location ){
				double[] position = {(double) location[0], (double) location[1]};
				double[] featureVector = kernelExpand(position);
				double[] newPosition = multiply(beta, featureVector);

				location[0] = (float) newPosition[0];
				location[1] = (float) newPosition[1];
		}

		void precalculateTransfom(){
				transField = new double[width][height][2];
				//double minX = width, minY = height, maxX = 0, maxY = 0;
				
				for (int x=0; x<width; x++){
						for (int y=0; y<height; y++){
								double[] position = {x,y};
								double[] featureVector = kernelExpand(position);
								double[] newPosition = multiply(beta, featureVector);
				
								if ((newPosition[0] < 0) || (newPosition[0] >= width) ||
										(newPosition[1] < 0) || (newPosition[1] >= height))
										{
												transField[x][y][0] = -1;
												transField[x][y][1] = -1;
												continue;
										}
				
								transField[x][y][0] = newPosition[0];
								transField[x][y][1] = newPosition[1];

								//minX = Math.min(minX, x);
								//minY = Math.min(minY, y);
								//maxX = Math.max(maxX, x);
								//maxY = Math.max(maxY, y);
								
						}
				}

				precalculated = true;
		}

		public double[][] getCoefficients(){
				double[][] coeffMatrix = new double[4][length];

				for(int i=0; i<length; i++){
						coeffMatrix[0][i] = beta[i][0];
						coeffMatrix[1][i] = beta[i][1];
						coeffMatrix[2][i] = normMean[i];
						coeffMatrix[3][i] = normVar[i];

				}
				return coeffMatrix;
		}

		public void setBeta(double[][] b){
				beta = b;
				//FIXME: test if normMean and normVar are still valid for this beta
		}
	
		public void print(){
				System.out.println("beta:");
				for (int i=0; i < beta.length; i++){
						for (int j=0; j < beta[i].length; j++){
								System.out.print(beta[i][j]);
								System.out.print(" ");
						}
						System.out.println();
				}
		
				System.out.println("normMean:");
				for (int i=0; i < normMean.length; i++){
						System.out.print(normMean[i]);
						System.out.print(" ");
				}
		
				System.out.println("normVar:");
				for (int i=0; i < normVar.length; i++){
						System.out.print(normVar[i]);
						System.out.print(" ");
				}
		
				System.out.println("Image size:");
				System.out.println("width: " + width + " height: " + height);
		
				System.out.println();
		
		}
	
		public void save( final String filename )
		{
				try{
						BufferedWriter out = new BufferedWriter(
							new OutputStreamWriter(
							new FileOutputStream( filename) ) );
						try{	
								out.write("Kerneldimension");
								out.newLine();
								out.write(Integer.toString(dimension));
								out.newLine();
								out.newLine();
								out.write("number of rows");
								out.newLine();
								out.write(Integer.toString(length));
								out.newLine();
								out.newLine();
								out.write("Coefficients of the transform matrix:");
								out.newLine();
								for (int i=0; i < length; i++){
										String s = Double.toString(beta[i][0]);
										s += "    ";
										s += Double.toString(beta[i][1]);
										out.write(s);
										out.newLine();		
								}
								out.newLine();
								out.write("normMean:");
								out.newLine();
								for (int i=0; i < length; i++){
										out.write(Double.toString(normMean[i]));
										out.newLine();
								}
								out.newLine();
								out.write("normVar: ");
								out.newLine();
								for (int i=0; i < length; i++){
										out.write(Double.toString(normVar[i]));
										out.newLine();
								}
								out.newLine();
								out.write("image size: ");
								out.newLine();
								out.write(width + "    " + height);
								out.close();
						}
						catch(IOException e){System.out.println("IOException");}
				}
				catch(FileNotFoundException e){System.out.println("File not found!");}
		}
	
		public void load(String filename){
				try{
						BufferedReader in = new BufferedReader(new FileReader(filename));
						try{	
								String line = in.readLine(); //comment;
								dimension = Integer.parseInt(in.readLine()); 
								line = in.readLine(); //comment;
								line = in.readLine(); //comment;
								length = Integer.parseInt(in.readLine());
								line = in.readLine(); //comment;
								line = in.readLine(); //comment;
		
								beta = new double[length][2]; 
			
								for (int i=0; i < length; i++){
										line = in.readLine();
										int ind = line.indexOf(" ");
										beta[i][0] = Double.parseDouble(line.substring(0, ind));
										beta[i][1] = Double.parseDouble(line.substring(ind+4));
								}	
		
								line = in.readLine(); //comment;
								line = in.readLine(); //comment;
			    
								normMean = new double[length];
			
								for (int i=0; i < length; i++){
										normMean[i]=Double.parseDouble(in.readLine());
								}
			
								line = in.readLine(); //comment;
								line = in.readLine(); //comment;
			
								normVar = new double[length];
			
								for (int i=0; i < length; i++){
										normVar[i]=Double.parseDouble(in.readLine());
								}
								line = in.readLine(); //comment;
								line = in.readLine(); //comment;
								line = in.readLine();
								int ind = line.indexOf(" ");
								width = Integer.parseInt(line.substring(0, ind));
								height = Integer.parseInt(line.substring(ind+4));
								in.close();
			
								print();
						}
						catch(IOException e){System.out.println("IOException");}
				}
				catch(FileNotFoundException e){System.out.println("File not found!");}
		}
	
		public ImageProcessor[] transform(ImageProcessor ip){
				if (!precalculated)
						this.precalculateTransfom();

				ImageProcessor newIp = ip.createProcessor(ip.getWidth(), ip.getHeight());
				if (ip instanceof ColorProcessor) ip.max(0); 
				ImageProcessor maskIp = new ByteProcessor(ip.getWidth(),ip.getHeight());
		
				for (int x=0; x < width; x++){
						for (int y=0; y < height; y++){
								if (transField[x][y][0] == -1){
										continue;
								}
								newIp.set(x, y, (int) ip.getInterpolatedPixel((int)transField[x][y][0],(int)transField[x][y][1]));
								maskIp.set(x,y,255);
						}
				}
				return new ImageProcessor[]{newIp, maskIp};
		}
	
		private double[] multiply(double beta[][], double featureVector[]){
				double[] result = {0.0,0.0};
		
				if (beta.length != featureVector.length){
						IJ.showMessage("Dimension of TransformMatrix and featureVector do not match!");
						return new double[2];
				}
		
				for (int i=0; i<featureVector.length; i++){
						result[0] = result[0] + featureVector[i] * beta[i][0];
						result[1] = result[1] + featureVector[i] * beta[i][1];
				}
		
				return result;
		}

		public double[] kernelExpand(double position[]){
				double expanded[] = new double[length];
		
				int counter = 0;
				for (int i=1; i<=dimension; i++){
						for (double j=i; j>=0; j--){
								double val = Math.pow(position[0],j) * Math.pow(position[1],i-j);
								expanded[counter] = val;
								++counter;
						}
				}
		
				for (int i=0; i<length-1; i++){
						expanded[i] = expanded[i] - normMean[i];
						expanded[i] = expanded[i] / normVar[i];
				}

				expanded[length-1] = 100;
		
				return expanded;
		}

	
		public double[][] kernelExpandMatrixNormalize(double positions[][]){
				normMean = new double[length];
				normVar = new double[length];
		
				for (int i=0; i < length; i++){
						normMean[i] = 0;
						normVar[i] = 1;
				}
		
				double expanded[][] = new double[positions.length][length];
		
				for (int i=0; i < positions.length; i++){
						expanded[i] = kernelExpand(positions[i]);
				}
		
				for (int i=0; i < length; i++){
						double mean = 0;
						double var = 0;
						for (int j=0; j < expanded.length; j++){
								mean += expanded[j][i];
						}
			
						mean /= expanded.length;
			
						for (int j=0; j < expanded.length; j++){
								var += (expanded[j][i] - mean)*(expanded[j][i] - mean);
						}
						var /= (expanded.length -1);
						var = Math.sqrt(var);
			
						normMean[i] = mean;
						normVar[i] = var;
				}
		
				return kernelExpandMatrix(positions);
		
		}
	
		//this function uses the parameters already stored
		//in this object to normalize the positions given.
		public double[][] kernelExpandMatrix(double positions[][]){
	
		
				double expanded[][] = new double[positions.length][length];
		
				for (int i=0; i < positions.length; i++){
						expanded[i] = kernelExpand(positions[i]);
				}
		
				return expanded;
		
		}
	
		public void inverseTransform(double range[][]){
				Matrix expanded = new Matrix(kernelExpandMatrix(range));
				Matrix b = new Matrix(beta);	
		
				Matrix transformed = expanded.times(b);
				expanded = new Matrix(kernelExpandMatrixNormalize(transformed.getArray()));
		
				Matrix r = new Matrix(range);
				Matrix invBeta = expanded.transpose().times(expanded).inverse().times(expanded.transpose()).times(r);
				setBeta(invBeta.getArray());
		}
	
    //FIXME this takes way too much memory 
		public void visualize(){
		
				int density = Math.max(width,height)/32;
				int border = Math.max(width,height)/8;
				
				double[][] orig = new double[width *  height][2];
				double[][] trans = new double[height * width][2];
				double[][] gridOrigVert = new double[width*height][2];
				double[][] gridTransVert = new double[width*height][2];
				double[][] gridOrigHor = new double[width*height][2];
				double[][] gridTransHor = new double[width*height][2];	
		
				FloatProcessor magnitude = new FloatProcessor(width, height);
				FloatProcessor angle = new FloatProcessor(width, height);
				ColorProcessor quiver = new ColorProcessor(width, height);
				ByteProcessor empty = new ByteProcessor(width+2*border, height+2*border);
				quiver.setLineWidth(1);
				quiver.setColor(Color.green);
		
				GeneralPath quiverField = new GeneralPath();
		
				float minM = 1000, maxM = 0;
				float minArc = 5, maxArc = -6;
				int countVert = 0, countHor = 0, countHorWhole = 0;
				
				for (int i=0; i < width; i++){
						countHor = 0;
						for (int j=0; j < height; j++){
								double[] position = {(double) i,(double) j};
								double[] posExpanded = kernelExpand(position);
								double[] newPosition = multiply(beta, posExpanded);

								orig[i*j][0] = position[0];
								orig[i*j][1] = position[1];
				
								trans[i*j][0] = newPosition[0];
								trans[i*j][1] = newPosition[1];
				
								double m = (position[0] - newPosition[0]) * (position[0] - newPosition[0]);
								m += (position[1] - newPosition[1]) * (position[1] - newPosition[1]);
								m = Math.sqrt(m);
								magnitude.setf(i,j, (float) m);
								minM = Math.min(minM, (float) m); 
								maxM = Math.max(maxM, (float) m);
				
								double a = Math.atan2(position[0] - newPosition[0], position[1] - newPosition[1]);
								minArc = Math.min(minArc, (float) a);
								maxArc = Math.max(maxArc, (float) a);
								angle.setf(i,j, (float) a);
				
								if (i%density == 0 && j%density == 0)
										drawQuiverField(quiverField, position[0], position[1], newPosition[0], newPosition[1]);
								if (i%density == 0){
										gridOrigVert[countVert][0] = position[0] + border;
										gridOrigVert[countVert][1] = position[1] + border;
										gridTransVert[countVert][0] = newPosition[0] + border;
										gridTransVert[countVert][1] = newPosition[1] + border;
										countVert++;
								}
								if (j%density == 0){
										gridOrigHor[countHor*width+i][0] = position[0] + border;
										gridOrigHor[countHor*width+i][1] = position[1] + border;
										gridTransHor[countHor*width+i][0] = newPosition[0] + border;
										gridTransHor[countHor*width+i][1] = newPosition[1] + border;
										countHor++;
										countHorWhole++;
								}	
						}
				}
		
				magnitude.setMinAndMax(minM, maxM);
				angle.setMinAndMax(minArc, maxArc);
				//System.out.println(" " + minArc + " " + maxArc);
	
				ImagePlus magImg = new ImagePlus("Magnitude of Distortion Field", magnitude);
				magImg.show();
		
				//		ImagePlus angleImg = new ImagePlus("Angle of Distortion Field Vectors", angle);
				//		angleImg.show();
		
				ImagePlus quiverImg = new ImagePlus("Quiver Plot of Distortion Field", magnitude);
				quiverImg.show();
				quiverImg.getCanvas().setDisplayList(quiverField, Color.green, null );
				quiverImg.updateAndDraw();
		
				//		GeneralPath gridOrig = new GeneralPath();
				//		drawGrid(gridOrig, gridOrigVert, countVert, height);
				//		drawGrid(gridOrig, gridOrigHor, countHorWhole, width);
				//		ImagePlus gridImgOrig = new ImagePlus("Distortion Grid", empty);
				//		gridImgOrig.show();
				//		gridImgOrig.getCanvas().setDisplayList(gridOrig, Color.green, null );
				//		gridImgOrig.updateAndDraw();
		
				GeneralPath gridTrans = new GeneralPath();
				drawGrid(gridTrans, gridTransVert, countVert, height);
				drawGrid(gridTrans, gridTransHor, countHorWhole, width);
				ImagePlus gridImgTrans = new ImagePlus("Distortion Grid", empty);
				gridImgTrans.show();
				gridImgTrans.getCanvas().setDisplayList(gridTrans, Color.green, null );
				gridImgTrans.updateAndDraw();

				//new FileSaver(quiverImg.getCanvas().imp).saveAsTiff("QuiverCanvas.tif");
				new FileSaver(quiverImg).saveAsTiff("QuiverImPs.tif");
		
				System.out.println("FINISHED");
		}


		public void visualizeSmall(double lambda){
				int density = Math.max(width,height)/32;
				
				double[][] orig = new double[width *  height][2];
				double[][] trans = new double[height * width][2];
		
				FloatProcessor magnitude = new FloatProcessor(width, height);
				ColorProcessor quiver = new ColorProcessor(width, height);
				quiver.setLineWidth(1);
				quiver.setColor(Color.green);
		
				GeneralPath quiverField = new GeneralPath();
		
				float minM = 1000, maxM = 0;
				float minArc = 5, maxArc = -6;
				int countVert = 0, countHor = 0, countHorWhole = 0;
				
				for (int i=0; i < width; i++){
						countHor = 0;
						for (int j=0; j < height; j++){
								double[] position = {(double) i,(double) j};
								double[] posExpanded = kernelExpand(position);
								double[] newPosition = multiply(beta, posExpanded);

								orig[i*j][0] = position[0];
								orig[i*j][1] = position[1];
				
								trans[i*j][0] = newPosition[0];
								trans[i*j][1] = newPosition[1];
				
								double m = (position[0] - newPosition[0]) * (position[0] - newPosition[0]);
								m += (position[1] - newPosition[1]) * (position[1] - newPosition[1]);
								m = Math.sqrt(m);
								magnitude.setf(i,j, (float) m);
								minM = Math.min(minM, (float) m); 
								maxM = Math.max(maxM, (float) m);
				
								if (i%density == 0 && j%density == 0)
										drawQuiverField(quiverField, position[0], position[1], newPosition[0], newPosition[1]);
						}
				}
		
				magnitude.setMinAndMax(minM, maxM);
				ImagePlus quiverImg = new ImagePlus("Quiver Plot for lambda = "+lambda, magnitude);
				quiverImg.show();
				quiverImg.getCanvas().setDisplayList(quiverField, Color.green, null );
				quiverImg.updateAndDraw();
		
				System.out.println("FINISHED");
		}

	
		public static void drawGrid(GeneralPath g, double[][] points, int count, int s){
				for (int i=0; i < count - 1; i++){
						if ((i+1)%s != 0){
								g.moveTo((float)points[i][0], (float)points[i][1]);
								g.lineTo((float)points[i+1][0], (float)points[i+1][1]);
						}
				}
		}
	
		public static void drawQuiverField(GeneralPath qf, double x1, double y1, double x2, double y2)
		{
				qf.moveTo((float)x1, (float)y1);
				qf.lineTo((float)x2, (float)y2);
		}
	
		public int getWidth(){
				return width;
		}
	
		public int getHeight(){
				return height;
		}
}
