//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
//==============================================================================

package edf;

import javax.swing.JTextArea;

public class LogSingleton {

	private static LogSingleton instance;

	public static int LENGTH_TASK_1 = 15;
	public static int LENGTH_TASK_2 = 65;
	public static int LENGTH_TASK_3 = 15;
	public static int LENGTH_TASK_4 = 5;

	private JTextArea log;
	private double startTime;
	private int progessLength;
	private String mem;


	/**
	 *
	 */
	private LogSingleton() {
		log = new JTextArea();
	}


	/**
	 *
	 */
	public static LogSingleton getInstance() {
		if(instance == null){
			instance = new LogSingleton();
		}
		return instance;
	}

	/**
	 *
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}


	/**
	 *
	 */
	public JTextArea getJTextArea() {
		return log;
	}


	/**
	 *
	 */
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}


	/**
	 *
	 */
	public void setProgessLength(int current) {
		this.progessLength = current;
	}



	/**
	 */
	public int getProgessLength() {
		return this.progessLength;
	}

	/**
	 *
	 */
	public String getElapsedTime() {
		double t = System.currentTimeMillis()-startTime;
		if (t > 3000)
			return new java.text.DecimalFormat(" 000.00 s ").format(t/1000);
		else
			return new java.text.DecimalFormat(" 0000 ms ").format(t);
	}

	/**
	 *
	 */
	public String getMemString(){
		return this.mem;
	}


	/**
	 *
	 */
	public void start(String msg) {
		long freeMem = (Runtime.getRuntime().freeMemory())/1024;
		java.text.DecimalFormat dfm = new java.text.DecimalFormat(" 000000kB ");
		mem = dfm.format(freeMem);
		log.append(mem + "\t" + getElapsedTime() + "\t" + msg);
		log.setCaretPosition(log.getDocument().getLength());
	}


	/**
	 *
	 */
	public void acknowledge() {
		log.append(": OK\n");
		log.setCaretPosition(log.getDocument().getLength());
	}


	/**
	 *
	 */
	public void append(String msg) {
		log.append(msg + "\n");
		log.setCaretPosition(log.getDocument().getLength());
	}

	/**
	 *
	 */
	public void clear(){
		log.setText("");
	}

}
