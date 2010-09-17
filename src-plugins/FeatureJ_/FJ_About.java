import ij.IJ;
import ij.plugin.PlugIn;
import java.util.Calendar;

public class FJ_About implements PlugIn {

	public void run(String arg) {

		IJ.showMessage(
			FJ.name()+": About",
			FJ.name()+" is a plugin package for image feature extraction.\n"+
			"The version number of the present installation is "+FJ.version()+".\n"+
			"Copyright (C) 2002-"+Calendar.getInstance().get(Calendar.YEAR)+" by Erik Meijering.\n"
		);
	}

}
