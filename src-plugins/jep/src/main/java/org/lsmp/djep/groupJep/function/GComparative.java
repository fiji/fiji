/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.lsmp.djep.groupJep.function;

import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.lsmp.djep.groupJep.*;
import org.lsmp.djep.groupJep.interfaces.*;

/** 
 * Implements logical operations on a group.
 * Will always return Boolean results.
 * 
 * @author Rich Morris
 * Created on 06-Mar-2004
 */
public class GComparative extends PostfixMathCommand
{
	int id;
	GroupI group;
	
	/**
	 * Constructor.	For inequalities the group should implement OrderedSetI.
	 * 
	 * @param id should be Comparative.LT or GT, LE, GE, NE EQ
	 * @see org.nfunk.jep.function.Comparative
	 */	
	public GComparative(GroupI group,int id)
	{
		this.id = id;
		numberOfParameters = 2;
		this.group = group;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		if ((param1 instanceof Number) && (param2 instanceof Number))
		{
			Number num1 = (Number)param1;
			Number num2 = (Number)param2;
			boolean flag;
			if(group instanceof OrderedSetI)
			{
				int comp = ((OrderedSetI) group).compare(num1,num2);
				switch (id)
				{
					case Comparative.LT:
						flag = comp < 0;
						break;
					case Comparative.GT:
						flag = comp > 0;
						break;
					case Comparative.LE:
						flag = comp <= 0;
						break;
					case Comparative.GE:
						flag = comp >= 0;
						break;
					case Comparative.NE:
						flag = comp != 0;
						break;
					case Comparative.EQ:
						flag = comp == 0;
						break;
					default:
						throw new ParseException("Unknown relational operator");
				}
			}
			else
			{
				switch (id)
				{
				case Comparative.NE:
					flag = !group.equals(num1,num2);
					break;
				case Comparative.EQ:
					flag = group.equals(num1,num2);
					break;
				default:
					throw new ParseException("Unknown relational operator");
				}
			}
			inStack.push(flag ? Boolean.TRUE : Boolean.FALSE);//push the result on the inStack
		}
		else if ((param1 instanceof Boolean) && (param2 instanceof Boolean))
		{
			boolean num1 = ((Boolean)param1).booleanValue();
			boolean num2 = ((Boolean)param2).booleanValue();
			boolean flag;
			switch (id)
			{
			case Comparative.NE:
				flag = num1 != num2;
				break;
			case Comparative.EQ:
				flag = num1 == num2;
				break;
			default:
				throw new ParseException("Unknown relational operator");
			}
			inStack.push(flag ? Boolean.TRUE : Boolean.FALSE);//push the result on the inStack
		}
		else throw new ParseException("Invalid parameters for comparitive op");
			
	}
}
