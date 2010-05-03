package org.imagearchive.lsm.toolbox;

import ij.IJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MasterModel {

	//private ServiceMediator serviceMediator;

	//private Reader reader;

	private static MasterModel masterModel;

	public static final String VERSION = "4.0g";

	public static boolean debugMode = false;

	public static char micro = '\u00b5';

	public static String micrometer = micro + "m";

	public static byte NONE = 0;

	public static byte CHANNEL = 1;

	public static byte DEPTH = 2;

	public static byte TIME = 3;

	public String[] supportedBatchTypes = { "Tiff", "8-bit Tiff", "Jpeg",
			"Zip", "Raw" };

	public String[] macroFiles = { "magic_montage.txt" };

	public String[] macros = new String[macroFiles.length];


	public static MasterModel getMasterModel(){
		if (masterModel == null) masterModel = new MasterModel();
		return masterModel;
	}

	public MasterModel() {
		initializeModel();
		registerServices();
		readMacros();
	}

	public void initializeModel() {
		//serviceMediator = new ServiceMediator();
		//reader = new Reader();
	}

	public void readMacros() {
		for (int i = 0; i < macroFiles.length; i++) {
			InputStream in = getClass().getClassLoader().getResourceAsStream(
					"org/imagearchive/lsm/toolbox/macros/" + macroFiles[i]);
			try {
				if (in == null)
					throw new IOException();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in));
				StringBuffer macroBuffer = new StringBuffer();
				String line;
				while ((line = reader.readLine()) != null) {
					macroBuffer.append(line + "\n");
				}
				macros[i] = macroBuffer.toString();
				reader.close();
			} catch (IOException e) {
				macros[i] = null;
				IJ.error("Could not load internal macro.");
			}
		}
	}

	private void registerServices() {
	}

	/** *************************************************************************** */
	public String getVersion() {
		return VERSION;
	}

	public String getMacro(int i) {
		if (i >= 0 && i < macros.length)
			return macros[i];
		else
			return null;
	}

	public String getMagicMontaqe() {
		StringBuffer sb = new StringBuffer();
		String ext_macro = null;
		float ext_ver = 0.0f;
		float int_ver = 0.0f;
		String int_macro = new String();
		String toolsSetDir = IJ.getDirectory("macros") + File.separator
				+ "toolsets";
		BufferedReader input;
		try {
			File f = new File(toolsSetDir + File.separator + "magic_montage.txt");
			input = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = input.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}
			input.close();
			ext_macro = sb.toString();

		} catch (IOException e) {
			//probably no magic montage
		}
		if (ext_macro != null)
		try{
			ext_ver = Float.parseFloat(ext_macro.substring(ext_macro.indexOf("//--version--")+13,ext_macro.indexOf("\n")));
		}catch(NumberFormatException e){
		}
		int_macro = getMacro(0);
		if (int_macro != null)
		try{
			int_ver = Float.parseFloat(int_macro.substring(int_macro.indexOf("//--version--")+13,int_macro.indexOf("\n")));
		}catch(NumberFormatException e){
		}
		if (int_ver < ext_ver) return ext_macro;
		return int_macro;
	}
}
