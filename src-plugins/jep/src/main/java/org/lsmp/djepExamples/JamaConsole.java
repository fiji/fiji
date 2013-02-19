/* @author rich
 * Created on 21-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;
import org.lsmp.djep.jama.JamaUtil;

/**
 * has support for Jama matrix operations
 * @author Rich Morris
 * Created on 21-Mar-2005
 * @see <a href="http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a>
 */
public class JamaConsole extends VectorConsole
{
	private static final long serialVersionUID = -4256036388099114905L;

	public static void main(String[] args)
	{
		Console c = new JamaConsole();
		c.run(args);
	}
	
	public void initialise()
	{
		super.initialise();
		JamaUtil.addStandardFunctions(j);
	}

	public void printHelp()
	{
		super.printHelp();
		println("inverse([[1,2],[3,4]])");
		println("rank([[1,2],[3,4]])");
		println("z = solve(x,y) solves x*z = y");
	}

	public void printIntroText()
	{
		super.printIntroText();
		println("Adds Jama matrix ops: inverse, solve, rank");
	}

}
