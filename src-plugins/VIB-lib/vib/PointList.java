package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import math3d.Point3d;


/**
 * @date 07.08.2006
 *
 * @author Benjamin Schmid
 */
public class PointList implements Iterable<BenesNamedPoint>{

	private List<BenesNamedPoint> points;
	private List<PointListListener> listeners;

	public PointList(){
		listeners = new ArrayList<PointListListener>();
		points = new ArrayList<BenesNamedPoint>();
	}

	public void add(BenesNamedPoint point){
		points.add(point);
		fireAdded(point);
	}

	public void add(String name, double x, double y, double z) {
		add(new BenesNamedPoint(name, x, y, z));
	}

	public void add(double x, double y, double z) {
		add(getDefaultNameForNext(), x, y, z);
	}

	private String getDefaultNameForNext() {
		int size = points.size();
		int point = 1;
		if(size != 0) {
			String lastp = get(points.size() - 1).name;
			try {
				point = Integer.parseInt(lastp.substring(
					5, lastp.length())) + 1;
			} catch(Exception e) {
				point = size;
			}
		}
		return "point" + point;
	}

	public void remove(BenesNamedPoint point){
		int i = indexOf(point);
		remove(i);
	}

	public void remove(int i) {
		if(i >= 0 && i < size()) {
			BenesNamedPoint p = points.get(i);
			points.remove(i);
			fireRemoved(p);
		}
	}

	public void clear() {
		while(size() > 0) {
			remove(0);
		}
	}

	public void rename(BenesNamedPoint point, String name){
		point.name = name;
		fireRenamed(point);
	}

	public void up(BenesNamedPoint point) {
		int size = points.size();
		int i = points.indexOf(point);
		points.remove(i);
		points.add((i - 1 + size) % size, point);
		fireReordered();
	}

	public void down(BenesNamedPoint point) {
		int i = points.indexOf(point);
		int size = points.size();
		points.remove(i);
		points.add((i + 1) % size, point);
		fireReordered();
	}

	public void highlight(BenesNamedPoint p) {
		fireHighlighted(p);
	}

	public void placePoint(BenesNamedPoint point,
				double x, double y, double z) {
		point.set(x, y, z);
		fireMoved(point);
	}

	public BenesNamedPoint get(int index){
		return points.get(index);
	}

	public int indexOf(BenesNamedPoint p) {
		return points.indexOf(p);
	}

	public int indexOfPointAt(double x, double y, double z, double tol) {
		Point3d p = new Point3d(x, y, z);
		tol *= tol;
		for(int i = 0; i < points.size(); i++) {
			BenesNamedPoint bnp = (BenesNamedPoint)points.get(i);
			if(p.distance2(bnp) < tol)
				return i;
		}
		return -1;
	}

	public BenesNamedPoint pointAt(double x, double y, double z, double tol) {
		int i = indexOfPointAt(x, y, z, tol);
		if(i == -1)
			return null;
		return get(i);
	}

	public BenesNamedPoint[] toArray(){
		return points.toArray(new BenesNamedPoint[]{});
	}

	public int size(){
		return points.size();
	}

	public BenesNamedPoint get(String name){
		for(BenesNamedPoint p : points){
			if(p.name.equals(name)){
				return p;
			}
		}
		return null;
	}

	public Iterator<BenesNamedPoint> iterator() {
		return points.iterator();
	}

	public PointList duplicate() {
		PointList copy = new PointList();
		Iterator<BenesNamedPoint> it = iterator();
		while(it.hasNext()) {
			BenesNamedPoint p = it.next();
			copy.add(new BenesNamedPoint(p.name, p.x, p.y, p.z));
		}
		return copy;
	}

	public static PointList fromMask(ImagePlus imp) {
		PointList res = new PointList();
		int w = imp.getWidth();
		int d = imp.getStackSize();
		Calibration cal = imp.getCalibration();
		double pw = cal.pixelWidth, ph = cal.pixelHeight;
		double pd = cal.pixelDepth;
		for(int z = 0; z < d; z++) {
			byte[] pixels = (byte[])imp.getStack().getPixels(z+1);
			for(int i = 0; i < pixels.length; i++) {
				if(pixels[i] != (byte)255)
					continue;
				res.add(new BenesNamedPoint("point" + i,
					(i % w) * pw, (i / w) * ph, z * pd));
			}
		}
		return res;
	}

	public static PointList load(ImagePlus imp){
		FileInfo info = imp.getOriginalFileInfo();
		if(info != null){
			PointList l = load(info.directory,
				info.fileName + ".points",true);
			return l;
		}
		return load(null, null, true);
	}

	public static PointList load(String dir, String file,
						boolean showDialog){

		String openPath = dir + File.separatorChar + file;
		if(showDialog) {
			OpenDialog od = new OpenDialog(
				"Open points annotation file", dir,file);

			if(od.getFileName()==null)
				return null;
			else {
				openPath = od.getDirectory()+od.getFileName();
			}
		}
		try {
			return load(new FileReader(openPath));
		} catch (FileNotFoundException e) {
			IJ.showMessage("Could not find file " + openPath);
		} catch (IOException e) {
			IJ.showMessage("Could not read file " + openPath);
		}
		return null;
	}

	public static PointList load(Reader reader) throws IOException {
		PointList list = new PointList();
		BufferedReader f = new BufferedReader(reader);
		String line;
		while ((line = f.readLine()) != null) {
			BenesNamedPoint p = BenesNamedPoint.
						fromLine(line);
			if(p != null)
				list.add(p);
		}
		f.close();
		return list;
	}

	public static PointList parseString(String fileContents) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(fileContents.getBytes());
		Reader reader = new InputStreamReader(inputStream);
		return load(reader);
	}

	public void save(String directory, String fileName ) {

		String suggestedSaveFilename = fileName+".points";
		SaveDialog sd = new SaveDialog(
					"Save points annotation file as...",
				       directory,
				       suggestedSaveFilename,
				       ".points");

		if(sd.getFileName() == null)
			return;

		String savePath = sd.getDirectory() + sd.getFileName();
		File file = new File(savePath);
		if ((file != null) && file.exists()) {
			if (!IJ.showMessageWithCancel(
				    "Save points annotation file", "The file "+
				    savePath+" already exists.\n"+
				    "Do you want to replace it?"))
				return;
		}
		IJ.showStatus("Saving point annotations to "+savePath);
		try {
			save(new PrintStream(savePath), true);
			IJ.showStatus("Saved point annotations.");
		} catch( IOException e ) {
			IJ.error("Error saving to: "+savePath+"\n"+e);
		}
	}

	public void save(PrintStream fos, boolean close) throws IOException {
		for(BenesNamedPoint p : points)
			if(p.set)
				fos.println(p.toYAML());
		if (close)
			fos.close();
		else
			fos.flush();
	}

	public static ArrayList<String> pointsInBothAsString(PointList points0,
		     PointList points1) {

		ArrayList<String> common = new ArrayList<String>();
		for(BenesNamedPoint point0 : points0){
			for(BenesNamedPoint point1 : points1){
				if(point0.name.equals(point1.name)){
					common.add(point0.name);
					break;
				}
			}
		}
		return common;
	}

	public static PointList pointsInBoth(
				PointList points0, PointList points1){

		PointList common = new PointList();
		for(BenesNamedPoint point0 : points0){
			for(BenesNamedPoint point1 : points1){
				if(point0.name.equals(point1.name)){
					common.add(point0);
					break;
				}
			}
		}
		return common;
	}

	public void print(){
		for(BenesNamedPoint p : points){
			System.out.println(p.toString());
		}
	}

	// listener stuff

	public void addPointListListener(PointListListener pll) {
		listeners.add(pll);
	}

	public void removePointListListener(PointListListener pll) {
		listeners.remove(pll);
	}

	private void fireAdded(BenesNamedPoint p) {
		for(PointListListener l : listeners)
			l.added(p);
	}

	private void fireRemoved(BenesNamedPoint p) {
		for(PointListListener l : listeners)
			l.removed(p);
	}

	private void fireRenamed(BenesNamedPoint p) {
		for(PointListListener l : listeners)
			l.renamed(p);
	}

	private void fireMoved(BenesNamedPoint p) {
		for(PointListListener l : listeners)
			l.moved(p);
	}

	private void fireHighlighted(BenesNamedPoint p) {
		for(PointListListener l : listeners)
			l.highlighted(p);
	}

	private void fireReordered() {
		for(PointListListener l : listeners)
			l.reordered();
	}


	public interface PointListListener {
		public void added(BenesNamedPoint p);
		public void removed(BenesNamedPoint p);
		public void renamed(BenesNamedPoint p);
		public void moved(BenesNamedPoint p);
		public void highlighted(BenesNamedPoint p);
		public void reordered();
	}
}
