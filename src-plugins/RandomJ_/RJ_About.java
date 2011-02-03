import ij.IJ;
import ij.plugin.PlugIn;
import java.util.Calendar;

public class RJ_About implements PlugIn {

	public void run(String arg) {

		IJ.showMessage(
			RJ.name()+": About",
			RJ.name()+" is a plugin package for image randomization.\n"+
			"The version number of the present installation is "+RJ.version()+".\n"+
			"Copyright (C) 2003-"+Calendar.getInstance().get(Calendar.YEAR)+" by Erik Meijering.\n"
		);
	}

}
