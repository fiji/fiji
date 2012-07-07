package imagescience.utility;

/** Resource class for working with the <a href="http://www.vtk.org/" target="newbrowser">Visualization Toolkit</a> (VTK) in the current environment. If available, the VTK dynamic link libraries (DLLs) are loaded into the system upon first use of this class. All DLLs are loaded except the ones containing patented algorithms. See the VTK website (linked above) for details on how to obtain and install the DLLs and the associated <code>vtk.jar</code> file. */
public class VTKResource {
	
	private static boolean available;
	
	static {
		try {
			System.loadLibrary("vtkCommonJava");
			System.loadLibrary("vtkFilteringJava");
			System.loadLibrary("vtkGraphicsJava");
			System.loadLibrary("vtkHybridJava");
			System.loadLibrary("vtkImagingJava");
			System.loadLibrary("vtkIOJava");
			System.loadLibrary("vtkParallelJava");
			System.loadLibrary("vtkRenderingJava");
			available = true;
		} catch (Throwable e) {
			available = false;
		}
	}
	
	/** Default constructor. */
	public VTKResource() { }
	
	/** Checks whether VTK can be used in the current environment.
		
		@return {@code true} if the VTK Java DLLs could be successfully loaded (availability check) and the associated Java classes in {@code vtk.jar} can be found (accessibility check); {@code false} if this is not the case.
	*/
	public static boolean check() {
		
		boolean accessible = true;
		try { Class.forName("vtk.vtkVersion"); }
		catch (Throwable e) { accessible = false; }
		
		return (available && accessible);
	}
	
}
