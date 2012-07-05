
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Benjamin Schmid
 * @date 19. July 2006
 */
public class Fill_holes implements PlugIn {
	
	private static boolean debug = false;

    public static final String MACRO_CMD =
		"var leftClick=16, alt=9;\n" +
        "macro 'Fill hole Tool - C111O11ffC100T6c0aF' {\n" +
        " while (true) {\n" +
        "  getCursorLoc(x, y, z, flags);\n" +
        "  if (flags&leftClick==0) exit();\n" +
        "  call('Fill_holes.fillHoles', x,y,z,flags);\n" +
        "  exit();" + 
        " }\n" +
        "}\n" +
        "\n";
	
	public void run(String arg) {
		MacroInstaller installer = new MacroInstaller();
        installer.install(MACRO_CMD);
	}
	
	public synchronized static void fillHoles(String x, String y, String z, String flags){
		fillHoles(Integer.parseInt(x),
				Integer.parseInt(y),
				Integer.parseInt(z));
	}

	public synchronized static void fillHoles(int x, int y, int z){
		
		ImagePlus imp = IJ.getImage();
		Roi roi = imp.getRoi();
        if (roi==null || roi.getType()!=Roi.COMPOSITE) {
        	IJ.showMessage("Image with composite selection required");
        	return;
        }
        if(roi.contains(x,y)){
        	IJ.showMessage("There is no hole at the specified location");
        	return;
        }
		
        if(roi instanceof ShapeRoi){
        	
        	try{
	        	Roi[] rois = ((ShapeRoi)roi).getRois();
		        imp.killRoi();
		        ShapeRoi containingRois = null;	        
		        
		        // Build up the recursive tree structure
		        RoiNode root = new RoiNode(new ShapeRoi(
		        		new Roi(0,0,imp.getWidth(),imp.getHeight())),true);
		        for(int i=0;i<rois.length;i++){
		        	root.add(new RoiNode(new ShapeRoi(rois[i])));
		        }
		        
		        RoiNode clickedRoiNode = findNodeForPosition(root, x, y);		        
		        clickedRoiNode.remove();
		       
		        ShapeRoi newRoi = new ShapeRoi(new Roi(0,0,imp.getWidth(),imp.getHeight()));
		        root.createRoi(newRoi);
		        
		        // Update the image
		        imp.setRoi(newRoi);		
				imp.updateAndDraw();
        	} catch(Exception e){
        		e.printStackTrace();
        	}
        }
	}
	
	public static RoiNode findNodeForPosition(RoiNode root, int x, int y){
		RoiNode parent = root, clickedRoiNode = null;		        
		while(parent != null){
			// look for the only one child node of parent 
			// which contains (x,y).
			boolean found = false;
			for (int i=0;i<parent.size();i++){
				clickedRoiNode = parent.children().get(i);
				if(clickedRoiNode.roi.contains(x,y)){
					found = true; 
					break;
				}
			}
			if(!found){ 
				// not found, so the click was within this node, 
				// but not in one of this node's children
				clickedRoiNode = parent;
				break;
			}		        	
			parent = clickedRoiNode;
		}
		return clickedRoiNode;
	}
	
	private static void dbg(String s){
		if(debug)
			System.out.println(s);
	}
		
	static class RoiNode {
		private RoiNode parent;
		private List<RoiNode> children;
		private ShapeRoi roi;
		private boolean isRoot = false;
		private int level;
		
		public RoiNode(ShapeRoi roi, boolean isRoot){
			this(roi);
			this.isRoot = isRoot;
			if(isRoot) level=0;
		}
		
		public RoiNode(ShapeRoi roi){
			this.roi = roi;
			this.children = new ArrayList<RoiNode>();
		}
		
		public int getLevel(){
			return this.level;
		}
		
		public boolean containsChild(RoiNode child){
			return children.contains(child);			
		}
		
		public List<RoiNode> children(){
			return children;
		}
		
		public List<RoiNode> grandchildren(){
			List<RoiNode> grandchildren = new ArrayList<RoiNode>();
			for(int i=0;i<children.size();i++){
				RoiNode child = children.get(i);
				grandchildren.addAll(child.children);				
			}
			return grandchildren;
		}
		
		public int size(){
			return children.size();
		}
		
		public ShapeRoi getRoi(){
			return roi;
		}
		
		public RoiNode getParent(){
			return parent;
		}
		
		public void setParent(RoiNode parent){
			this.parent = parent;
		}
		
		public void updateLevels(){
			level = isRoot ? 0 : parent.level+1;
			for(int i=0;i<size();i++)
				children.get(i).updateLevels();
		}
		
		public void createRoi(ShapeRoi newRoi){
			if(level%2 == 0){
				newRoi.not(this.roi);
			} else {
				newRoi.or(this.roi);
			}
	        for(int i=0;i<size();i++){
	        	children.get(i).createRoi(newRoi);
	        }
		}
		
		public void add(RoiNode newNode){
			dbg("\n\n" + this + "." + "add(" + newNode + ")...");
			if(!this.contains(newNode)){
				dbg("  this node does not contain it, give it to the parents");
				// parent != null since the root node returns always true on
				// contains().
				parent.add(newNode); 
				return;
			}
			// If one of my children contains the new node, 
			// I give the responsibility to her and forget about it.
			for(int i=0;i<children.size();i++){
				RoiNode child = children.get(i);
				if(child.contains(newNode)){
					dbg("My child " + child + " contains " + newNode + ", give the responsibility to her");
					child.add(newNode);					
					return;
				}				
			}
			dbg("None of my children contains " + newNode + ", so I must be the father myself ;)");
			// If not, the new node is my own child. I have to check if some 
			// of my children are actually children of the new node
			newNode.setParent(this);
			dbg("But perhaps I can give some of my children to her...");
			for(int i=0;i<children.size();i++){
				RoiNode child = children.get(i);
				if(newNode.contains(child)){
					dbg(newNode + " contains " + child);
					newNode.children.add(child);
					child.setParent(newNode);
					this.children.remove(child);
					i--; // since one is removed!
				}
			}
			this.children.add(newNode);
			this.updateLevels();
		}
		
		/**
		 * Removes me and my children, therefore my grandchildren
		 * become the children of my parent
		 *
		 */
		public void remove(){
			if(isRoot) return;
			parent.children.addAll(grandchildren());
			parent.children.remove(this);
			parent.updateLevels();
		}
		
		public boolean contains(RoiNode r){
			if(this.isRoot){
				return true;
			}
			int x0 = r.roi.getPolygon().xpoints[0];
			int y0 = r.roi.getPolygon().ypoints[0];
			if(roi.contains(x0,y0)){
				return true;
			}
			return false;			
		}
		
		public String longString(){
			String s = "";
			for(int i=0;i<level;i++) s+= "  ";
			s += "\\--" + this + "  parent: " + parent;
			return s;
		}
	
		public String toString(){
			return "[" + roi.getBoundingRect().width + "; " 
			+ roi.getBoundingRect().height + "] (" + level + ")";
		}
		
		public void print(){
			System.out.print(this.longString() + "\n");
			for(int i=0;i<children.size();i++){
				children.get(i).print();
			}
		}
	}
}
