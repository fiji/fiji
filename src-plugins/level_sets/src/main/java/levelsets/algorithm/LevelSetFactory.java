package levelsets.algorithm;

import ij.IJ;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import levelsets.ij.ImageContainer;
import levelsets.ij.ImageProgressContainer;
import levelsets.ij.StateContainer;




/**
 * @author erwin
 *
 * This class encapsulates all known implementations for Level Sets and their parameters.
 * This makes it (hopefully) easier to implement future variants of the LevelSets without
 * having to rework the dialog.
 * This class returns a Level Set instance as requested in getImplementation (as a factory should)
 * Parameters for each implementation (which may differ) are all handled in this factory. 
 * A calling class can get the names of each implementation, the parameters used in each implementation, 
 * their names and the suggested defaults.
 * To add a Level Set implementation, the implementer has to do following:
 * 1) Each parameter ever used in any implementation must be listed in the enum Parameter
 * 2) Add the corresponding free text description for the parameter in constructor param_desc.put
 * 3) Add the parameters that the implementation uses in def_global
 * 4) Create a case in getImplementation to create an instance
 * 5) Right now the class only deals with Double parameters. Anything else should be created as 
 *    nested class and must be dealt with in dialog. 
 */
public class LevelSetFactory {

	public enum Parameter { CONVERGENCE, W_ADVECTION, W_PROPAGATION, W_CURVATURE, TOL_GRAYSCALE }
		
	protected EnumMap<Parameter,String> param_desc = new EnumMap<Parameter,String>(Parameter.class);
	protected EnumMap<Parameter,Object> param_val = new EnumMap<Parameter, Object>(Parameter.class);
	protected HashMap<String, LevelSetDef> def = new HashMap<String, LevelSetDef>();
	protected String impl_default; 
	
	protected LevelSetDef def_global;
	
	final int verbose = 0;
	
	
	public LevelSetFactory() {
		
		// The parameters in all implementations and their full text description for a dialog
		// Convergence is always used (in base class), implemented independently in dialog
		// and thus not listed here
		param_desc.put(Parameter.W_ADVECTION, "Advection");
		param_desc.put(Parameter.W_CURVATURE, "Curvature");
		param_desc.put(Parameter.W_PROPAGATION, "Propagation");
		param_desc.put(Parameter.TOL_GRAYSCALE, "Grayscale tolerance");
		
		// hack, until the dialog in LevelSets becomes more sophisticated to deal with implementation specific parameters
		def_global = new LevelSetDef(
			new Parameter [] { Parameter.CONVERGENCE, Parameter.W_ADVECTION, Parameter.W_CURVATURE, Parameter.TOL_GRAYSCALE, Parameter.W_PROPAGATION },
			new Double [] { 0.005, 2.2, 1.0, 30.0, 1.0 } 
		); 
		
		// default to show in the dialog
		impl_default = new String("Active Contours");
		
		// Parameters in each implementation and their defaults
		def.put( "Active Contours",	
				new LevelSetDef( 
						new Parameter [] { Parameter.CONVERGENCE, Parameter.W_ADVECTION, Parameter.W_CURVATURE, Parameter.TOL_GRAYSCALE },
						new Double [] { 0.005, 2.2, 1.0, 30.0 } 
						) 
				);
		def.put( "Geodesic Active Contours",	
				new LevelSetDef( 
						new Parameter [] { Parameter.CONVERGENCE, Parameter.W_ADVECTION, Parameter.W_CURVATURE, Parameter.W_PROPAGATION },
						new Double [] { 0.005, 2.2, 1.0, 1.0 } 
						) 
			);
		
	}
	
	
	public LevelSetImplementation getImplementation(String impl, ImageContainer image,
			ImageProgressContainer img_progress, StateContainer init_state) {
		
		reconcileParams(impl);
		if (verbose > 0) IJ.log("Instantiating " + impl + " with parameters:");
		
		if (impl.equals("Active Contours")) {
			
			double convergence = ((Double) param_val.get(Parameter.CONVERGENCE)).doubleValue();
			double advection = ((Double) param_val.get(Parameter.W_ADVECTION)).doubleValue();
			double curvature = ((Double) param_val.get(Parameter.W_CURVATURE)).doubleValue();
			double grey_tol = ((Double) param_val.get(Parameter.TOL_GRAYSCALE)).doubleValue();
			
			if (verbose > 0) {
				IJ.log(((String) param_desc.get(Parameter.CONVERGENCE)) + " = " + convergence );
				IJ.log(((String) param_desc.get(Parameter.W_ADVECTION)) + " = " + advection );
				IJ.log(((String) param_desc.get(Parameter.W_CURVATURE)) + " = " + curvature );
				IJ.log(((String) param_desc.get(Parameter.TOL_GRAYSCALE)) + " = " + grey_tol );
			}
			
			return new ActiveContours(image, img_progress, init_state, 
					convergence, advection, curvature, grey_tol);
			
		} else if (impl.equals("Geodesic Active Contours"))	{

			double convergence = ((Double) param_val.get(Parameter.CONVERGENCE)).doubleValue();
			double advection = ((Double) param_val.get(Parameter.W_ADVECTION)).doubleValue();
			double curvature = ((Double) param_val.get(Parameter.W_CURVATURE)).doubleValue();
			double propagation = ((Double) param_val.get(Parameter.W_PROPAGATION)).doubleValue();
			
			if (verbose > 0) {
				IJ.log(((String) param_desc.get(Parameter.CONVERGENCE)) + " = " + convergence );
				IJ.log(((String) param_desc.get(Parameter.W_ADVECTION)) + " = " + advection );
				IJ.log(((String) param_desc.get(Parameter.W_CURVATURE)) + " = " + curvature );
				IJ.log(((String) param_desc.get(Parameter.W_PROPAGATION)) + " = " + propagation );
			}

			return new GeodesicActiveContour(image, img_progress, init_state, 
					convergence, advection, curvature, propagation, 0d);

		} else {
			return null;
		}
	}

	/*
	 * Returns the free text implementation names known to the factory
	 * The default is always in [0] 
	 */
	
	public final String [] getImplementationNames() {

		String [] impl = new String[def.size()];
		int i = 1;
		
		// make sure default is always 0
		impl[0] = impl_default;		
		
		for ( Iterator<String> it = def.keySet().iterator(); it.hasNext(); ) {
			String it_impl = it.next();
			if ( it_impl.equals(impl[0])) {
				continue;
			}
			impl[i++] = it_impl;
		}

		return impl;
	}
	
	
	/*
	 * Resets all parameters to the default for implementation specified in arguments
	 * If any parameter doesn't exist in this implementation it's set to null.
	 * TODO: Right now the parameters are set to global defaults and the implementation
	 * specific parameters are ignored. 
	 */
	public void resetParameters(String impl) {
		
		// TODO: implementation specific parameters instead of global defaults
//		Parameter[] imp_param = def.get(impl).params_used;
//		Object [] imp_def = def.get(impl).params_defaults;
		Parameter[] imp_param = def_global.params_used;
		Object [] imp_def = def_global.params_defaults;
		
		for ( Parameter p : Parameter.values() ) {
			param_val.put(p, new Double(0)); // TODO: Must set to correct type 
		}
		
		for ( int i=0; i < imp_param.length; i++ ) {
			// put replaces the old value if necessary (from EnumMap man page)
			param_val.put(imp_param[i], imp_def[i]);
		}			
	}
	
	
	public EnumMap<Parameter, Object> getParameterDefaults(String impl) {
		
		EnumMap<Parameter, Object> values = new EnumMap<Parameter, Object>(Parameter.class);
		
		Parameter[] imp_param = def.get(impl).params_used;
		Object [] imp_def = def.get(impl).params_defaults;
		
		for ( int i=0; i < imp_param.length; i++ ) {
			values.put(imp_param[i], imp_def[i]);
		}
		
		return values;		
	}
	

	public final EnumMap<Parameter,String> getParameters() {
		return param_desc;
	}

	
	public final Object getParameterValue(Parameter key) {
		return param_val.get(key);
	}
	
	
	public void setParameterValue(Parameter key, Object value) {
		param_val.put(key, value);
	}
	
	/*
	 * Makes sure that all the parameters for this implementation exist and, if not, are set to default value. 
	 */
	protected void reconcileParams(String impl) {
		
		LevelSetDef imp_def = def.get(impl);
		
		for (int i = 0; i < imp_def.params_used.length; i++	) {
			if ( !param_val.containsKey(imp_def.params_used[i]) ) {
				param_val.put(imp_def.params_used[i], imp_def.params_defaults[i]);
			}
		}
	}
	
	
	protected class LevelSetDef {
		public Parameter [] params_used;
		public Object [] params_defaults;
		
		public LevelSetDef(Parameter [] params_used, Object [] params_defaults ) {
			this.params_used = params_used;
			this.params_defaults = params_defaults;
		}
	}
	
	
}
