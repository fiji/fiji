package process3d;

import ij.text.TextWindow;
import ij.gui.GenericDialog;
import ij.WindowManager;

import ij.ImageStack;
import ij.ImagePlus;
import ij.IJ;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class Particle_Analyzer_3D implements PlugInFilter {

	private ImagePlus image;
	private ImagePlus result;
	private int[] classes;
	private int[] sizes;
	private int[] intensities;


	private int w,h,z;
	private int threshold = 100;
	private boolean showStatus = true;

	public Particle_Analyzer_3D() {}

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Particle Analyzer 3D");
		gd.addNumericField("Threshold [0..255]", threshold, 0);
		gd.addCheckbox("Show_result_table", true);
		gd.addCheckbox("Show_result_chart", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		threshold = (int)gd.getNextNumber();
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.z = image.getStackSize();
		this.showStatus = true;

		this.result = classify(image);
		calculateIntensities(image);
		calculateSizes();
 		sortResults();
		if (sizes.length > 3)
			keepNLargest(3);
		getResultAsByteImage().show();
		if(gd.getNextBoolean())
			showResultWindow();
		if(gd.getNextBoolean())
			showChart();
	}

	public ImagePlus getResultAsByteImage() {
		ImageStack stack = new ImageStack(w, h);
		for(int d = 0; d < z; d++) {
			int[] p = (int[])result.getStack().getPixels(d+1);
			byte[] b = new byte[w*h];
			for(int i = 0; i < b.length; i++) {
				if(p[i] == -1)
					b[i] = (byte)255;
				else
					b[i] = (byte)p[i];
			}
			stack.addSlice("", new ByteProcessor(w, h, b, null));
		}
		ImagePlus res = new ImagePlus("Classified", stack);
		res.setCalibration(image.getCalibration());
		return res;
	}

	public void keepNLargest(int n) {
		int[] sizes_tmp = new int[n];
		int[] intensities_tmp = new int[n];
		int[] classes_tmp = new int[n];
		System.arraycopy(sizes, 0, sizes_tmp, 0, n);
		System.arraycopy(intensities, 0, intensities_tmp, 0, n);
		System.arraycopy(classes, 0, classes_tmp, 0, n);
		for(int z = 0; z < result.getStackSize(); z++) {
			ImageProcessor ip = result.getStack().getProcessor(z+1);
			for(int i = 0; i < w*h; i++) {
				if(ip.get(i) >= n)
					ip.set(i, -1);
			}
		}
		sizes = sizes_tmp;
		intensities = intensities_tmp;
		classes = classes_tmp;
	}

	public void sortResults() {
		Cl[] cls = new Cl[classes.length];
		for(int i = 0; i < cls.length; i++) {
			cls[i] = new Cl(classes[i], sizes[i], intensities[i]);
		}
		Arrays.sort(cls);
		for(int z = 0; z < result.getStackSize(); z++) {
			ImageProcessor ip = result.getStack().getProcessor(z+1);
			for(int i = 0; i < w * h; i++) {
				if(ip.get(i) == -1)
					continue;
				for(int c = 0; c < cls.length; c++) {
					if(ip.get(i) == cls[c].cl) {
						ip.set(i, c);
						break;
					}
				}
			}
		}
		for(int c = 0; c < classes.length; c++) {
			Cl cl = cls[c];
			sizes[c] = cl.size;
			intensities[c] = cl.inten;
		}
	}

	private class Cl implements Comparable {
		int cl, size, inten;
		Cl(int cl, int size, int inten) {
			this.cl = cl; this.size = size; this.inten = inten;
		}

		public int compareTo(Object o) {
			// sort descending
			return ((Cl)o).size - this.size;
		}
	}

	public void showResultWindow() {
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < classes.length; i++) {
			buf.append(classes[i] + "\t" + sizes[i]);
			buf.append("\t" + intensities[i]);
			buf.append("\n");
		}
		String headings = "Class\tsize\tintensity";
		new TextWindow("Results", headings, buf.toString(), 500, 350);
	}

	public void showChart() {
		double[] x = new double[sizes.length];
		double[] y = new double[intensities.length];
		String[] labels = new String[classes.length];
		for(int i = 0; i < x.length; i++) {
			x[i] = (double)sizes[i];
			y[i] = (double)intensities[i]/(double)100;
			labels[i] = Integer.toString(classes[i]);
		}
		Plot_Dots pd = new Plot_Dots();
		pd.x = x;
		pd.y = y;
		pd.labels = labels;
		pd.xLabel = "Size";
		pd.yLabel = "Intensity";
		pd.create();
	}


	/**
	 * Constructor
	 */
	public Particle_Analyzer_3D(ImagePlus imp, int th, boolean showStatus) {
		this.image = imp;
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.z = image.getStackSize();
		this.showStatus = showStatus;
		this.threshold = th;
		this.result = classify(image);
	}

	/**
	 * Returns an array of ints, where each 
	 * int is one class label.
	 * @return classes
	 */
	public int[] getClasses(){
		return classes;
	}
	
	/**
	 * Returns an array which contains the number of 
	 * pixels for each class. The order in the array 
	 * is the same as in the array returned by getClasses().
	 * @return size of the classes
	 */
	public int[] sizes(){
		return sizes;
	}

	/**
	 * Returns the number of classes (excluding the 
	 * 'background' class)
	 * @return number of classes
	 */
	public int classesCount(){
		return classes.length;
	}

	/**
	 * Returns an image whose pixel values are the class
	 * labels.
	 * @return class image
	 */
	public ImagePlus classesAsImage(){
		return result;
	}

	/**
	 * Returns the part of the original picture which
	 * belongs to the specified class
	 */
	public ImagePlus imageForClass(int classlabel, String label){
		ImageStack stack = new ImageStack(w,h);
		for(int d=1;d<=z;d++){
			int[] result_pixels = (int[])result.getStack().getProcessor(d).
				getPixels();
			byte[] new_pixels = new byte[result_pixels.length];
			for(int i=0;i<result_pixels.length;i++){
				new_pixels[i] = result_pixels[i] == classlabel ? (byte)255 : (byte)0;
			}
			stack.addSlice("", new ByteProcessor(w,h,new_pixels,null));
		}
		if(label == null || label.trim().equals(""))
			label = "Class " + classlabel;
		ImagePlus tmp = new ImagePlus(label, stack);
		tmp.setCalibration(result.getCalibration());
		return tmp;
	}

	/**
	 * Returns the size of the specified class
	 */
	public int getSize(int classlabel){
		for(int i=0;i<classes.length;i++){
			if(classes[i] == classlabel)
				return sizes[i];
		}
		return -1;
	}

	/**
	 * Returns the label of the largest class
	 */
	public int getLargestClass(){
		int index = -1, maxSize = -1;
		for(int i=0;i<classes.length;i++){
			if(sizes[i] > maxSize){
				maxSize = sizes[i];
				index = i;
			}
		}
		return classes[index];
	}

	/**
	 * Returns the image of the largest class
	 */
	public ImagePlus imageOfLargestClass(){
		return imageForClass(getLargestClass(), "Largest object");
	}
	
	/**
	 * Creates a new ImagePlus which contains at each pixel location 
	 * a byte value which indicates to which class this location 
	 * belongs to. Background class has integer -1. The other classes start 
	 * with integer 0 and continue in ascending order (0, 1, 2, ...)
	 * 
	 * @param image ImagePlus binary image
	 * @return result classesAsImage
	 */
	private ImagePlus classify(ImagePlus image){
		if(showStatus)
			IJ.showStatus("classify...");
		MergedClasses mergedClasses = new MergedClasses();
		ImageStack resStack = new ImageStack(w,h);
		for(int d=1;d<=z;d++){
			byte[] pixels = 
				(byte[])image.getStack().getProcessor(d).getPixels();
			int[] classes = new int[w * h];
			int[] classesBefore = d > 1 ? 
				(int[])resStack.getProcessor(d-1).getPixels() : null;
			for(int i=0;i<h;i++){
				for(int j=0;j<w;j++){
					int index = i*w+j;
					byte current = pixels[index];
					int upper_c   = i > 0 ? classes[index-w] : -1;
					int left_c    = j > 0 ? classes[index-1] : -1;
					int before_c  = d > 1 ? classesBefore[index] : -1;
					classes[index] = classifyPixel(mergedClasses,
							current, upper_c, left_c, before_c);
				}
			}
			if(showStatus)
				IJ.showProgress(d,z);
			resStack.addSlice("",new ColorProcessor(w,h,classes));
		}
		correctMergedClasses(mergedClasses, resStack);
		ImagePlus tmp = new ImagePlus("Classified", resStack);
		tmp.setCalibration(image.getCalibration());
		return tmp;
	}
		
	private void correctMergedClasses(MergedClasses mergedClasses, ImageStack resStack){

		if(showStatus)
			IJ.showStatus("correct merged classes...");
		Map<Integer,Integer> map = mergedClasses.mapToRealClasses();
		for(int d=1;d<=z;d++){
			int[] res_pixels = (int[])resStack.getProcessor(d).getPixels();
			for(int i=0;i<res_pixels.length;i++){
				if(res_pixels[i] != -1){
					int realClass = map.get(res_pixels[i]);
					res_pixels[i] = realClass;
				}
			}
			if(showStatus)
				IJ.showProgress(d,z);
		}
		int n_classes = mergedClasses.classes.size();
		classes = new int[n_classes];
		for(int i=0;i<n_classes;i++) {
			classes[i] = i;
		}
	}

	public void calculateSizes(){
		if(showStatus)
			IJ.showStatus("calculate class sizes...");
		ImageStack resStack = result.getStack();
		sizes = new int[classes.length];
		for(int d=1;d<=z;d++){
			int[] classPixels = (int[])resStack
					.getProcessor(d).getPixels();
			for(int i=0;i<w*h;i++){
				if(classPixels[i] != -1)
					sizes[classPixels[i]]++;
			}
			if(showStatus)
				IJ.showProgress(d,z);
		}
	}

	public void calculateIntensities(ImagePlus intImp){
		if(showStatus)
			IJ.showStatus("calculate class intensities...");
		intensities = new int[classes.length];
		ImageStack resStack = result.getStack();
		ImageStack intStack = intImp.getStack();
		for(int d=1;d<=z;d++){
			int[] classPixels = (int[])resStack
					.getProcessor(d).getPixels();
			byte[] intPixels = (byte[])intStack
					.getProcessor(d).getPixels();
			for(int i=0;i<w*h;i++){
				if(classPixels[i] != -1)
					intensities[classPixels[i]]
						+= (int)(intPixels[i] & 0xff);
			}
			if(showStatus)
				IJ.showProgress(d,z);
		}
	}

	private int max(int[] array){
		int max = array[0];
		for(int i=1;i<array.length;i++)
			if(array[i] > max)
				max = array[i];
		return max;
	}
			
	private int classifyPixel(MergedClasses mergedClasses, byte cur, 
			int upper_c, int left_c, int before_c){
		if(((int)(cur & 0xff)) < threshold) // bg
			return -1;
		boolean connected = (upper_c != -1 || left_c != -1 || before_c != -1);
		int classl = -1;
		if(connected){
			classl = Math.max( Math.max(upper_c,left_c),before_c);
			if(upper_c != classl && upper_c != -1)
				mergedClasses.mergeIfNecessary(upper_c,classl);
			if(left_c != classl && left_c != -1)
				mergedClasses.mergeIfNecessary(left_c,classl);
			if(before_c != classl && before_c != -1)
				mergedClasses.mergeIfNecessary(before_c,classl);
		} else {
			classl = mergedClasses.addNewClass();
		}
		return classl;
	}

	private class MergedClasses{
		private List<Set<Integer>> classes = new ArrayList<Set<Integer>>();
		private int n_entries = 0;
		
		int addNewClass(){
			Set<Integer> newset = new HashSet<Integer>();
			newset.add(n_entries);
			classes.add(newset);
			n_entries++;
			return n_entries-1;
		}

		void mergeIfNecessary(int a, int b){
			Set<Integer> aSet = getClassWhichContains(a);
			Set<Integer> bSet = getClassWhichContains(b);
			if(aSet == null || bSet == null)
				IJ.error("Expected that both classes a and b already exist");
			if(aSet == bSet)
				return;
			aSet.addAll(bSet);
			classes.remove(bSet);
		}

		Set<Integer> getClassWhichContains(int n){
			int index = getClassIndexWhichContains(n);
			if(index != -1)
				return classes.get(index);
			return null;
		}

		int getClassIndexWhichContains(int n){
			int i = 0;
			for(Set<Integer> set : classes){
				if(set.contains(n))
					return i;
				i++;
			}
			return -1;
		}

		Map<Integer,Integer> mapToRealClasses(){
			Map<Integer,Integer> map = new HashMap<Integer,Integer>();
			for(int i=0;i<n_entries;i++){
				map.put(i,getClassIndexWhichContains(i));
			}
			return map;
		}

		void print(){
			for(int i=0;i<classes.size();i++){
				Set<Integer> set = classes.get(i);
				System.out.println(i + " --> [" + asString(set) + "]");
			}
		}

		String asString(Set<Integer> set){
			StringBuffer buf = new StringBuffer();
			for(Integer i : set)
				buf.append(i + "  ");
			return buf.toString();
		}
	}
}

