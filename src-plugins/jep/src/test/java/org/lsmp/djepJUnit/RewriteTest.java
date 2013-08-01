package org.lsmp.djepJUnit;

import junit.framework.*;

import org.nfunk.jep.*;
import org.lsmp.djep.djep.*;
import org.lsmp.djep.rewrite.*;

/* @author rich
 * Created on 19-Nov-2003
 */

/**
 * @author Rich Morris
 * Created on 19-Nov-2003
 */
public class RewriteTest extends DJepTest {
	DJep j;
	public static final boolean SHOW_BAD=false;
	
	public RewriteTest(String name) {
		super(name);
	}

	public static void main(String args[]) {
		// Create an instance of this class and analyse the file

		TestSuite suite= new TestSuite(RewriteTest.class);
//		DJepTest jt = new DJepTest("DJepTest");
//		jt.setUp();
		suite.run(new TestResult());
	}	

	public static Test suite() {
		return new TestSuite(RewriteTest.class);
	}

	public void testRewrite() throws Exception
	{
		DJep j=new DJep();
		j.addStandardFunctions();
		j.addStandardConstants();
		j.setImplicitMul(true);    
		j.addComplex();
		j.setAllowUndeclared(true);    
		j.setAllowAssignment(true);    
		j.addStandardDiffRules();
		j.getPrintVisitor().setMaxLen(80);
        
		j.addVariable("x", 0);
		RewriteVisitor ev = new RewriteVisitor();
		RewriteRuleI expand = new ExpandBrackets(j);
		RewriteRuleI colectPower = new CollectPowers(j);
		RewriteRuleI rules[] = new RewriteRuleI[]{expand,colectPower};

		String expresions[] = new String[]{
			"x*x",
			"x*x^2",
			"x^2*x"		
		};
		for(int i=0;i<expresions.length;++i)
		{
			Node node = j.parse(expresions[i]);
			System.out.print("Eqn:\t");
			j.println(node);
			Node node2 = ev.rewrite(node,j,rules,true);
			System.out.print("Expand:\t");
			j.println(node2);
		}
	}

	public void testTaylor() throws Exception
	{
		DJep taylorParser=new DJep();
		taylorParser.addStandardFunctions();
		taylorParser.addStandardConstants();
		taylorParser.setAllowUndeclared(true);    
		taylorParser.setAllowAssignment(true);    
		taylorParser.setImplicitMul(true); 
		taylorParser.addComplex();
		taylorParser.addStandardDiffRules();
		taylorParser.getPrintVisitor().setMaxLen(80);
       
		taylorParser.addVariable("x", 0);
		RewriteVisitor ev = new RewriteVisitor();
		RewriteRuleI expand = new ExpandBrackets(taylorParser);
		RewriteRuleI colectPower = new CollectPowers(taylorParser);
		RewriteRuleI rules[] = new RewriteRuleI[]{expand,colectPower};
		
		Node node2 = taylorParser.parse("ln(1+x)");
		Node node3 = node2;
		for(int i=1;i<5;++i)
		{
			Node node4 = taylorParser.differentiate(node3,"x");
			System.out.println("Deriv "+i);
			taylorParser.println(node4);
			Node node5 = taylorParser.simplify(node4);
			System.out.println("Simp ");
			taylorParser.println(node5);
			Node node6 = ev.rewrite(node5,taylorParser,rules,true);
			System.out.println("Expand ");
			taylorParser.println(node6);

			node3 = node5;
		}
	}
	
	public void testMemory() throws Exception
	{
		DJep taylorParser=new DJep();
		taylorParser.addStandardFunctions();
		taylorParser.addStandardConstants();
		taylorParser.setAllowUndeclared(true);    
		taylorParser.setAllowAssignment(true);    
		taylorParser.setImplicitMul(true);    
		taylorParser.addComplex();
		taylorParser.addStandardDiffRules();
       
		taylorParser.addVariable("x", 0);
		/*
		try {
			Node node = taylorParser.parse("diff(diff(diff(diff(diff(diff(diff(diff(ln(x+1),x),x),x),x),x),x),x),x)");
			Node processed = taylorParser.preprocess(node);
			Node simp = taylorParser.simplify(processed); 
		}
		catch(OutOfMemoryError e) { System.out.println(e.getMessage()); e.printStackTrace(); }
		
//		System.out.println(taylorParser.toString(simp));
 		*/   
	}

	public void testBad() throws ParseException
	{
		if(SHOW_BAD)
		{
			simplifyTest("1&&(1||x)","1");
			simplifyTest("diff(sgn(x),x)","0");	// sgn not implemented
			simplifyTest("diff(re(x+i y),x)","1"); // not smart enought to work out re(i) = 1
			simplifyTest("diff(re(x+i y),y)","0");
			simplifyTest("diff(im(x+i y),x)","0");
			simplifyTest("diff(im(x+i y),y)","1");
			simplifyTest("(x/2)*3","x*1.5");
			simplifyTest("diff(pow(x,y),x)","y*(pow(x,y-1))");
			simplifyTest("diff(pow(x,y),y)","(ln(x)) (pow(x,y))");

			DJep j2 = new DJep();			
			j2.addStandardDiffRules();

		}
	}
}
