/* @author rich
 * Created on 14-Apr-2004
 */
package org.lsmp.djep.rpe;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import java.util.*;
/**
 * A fast evaluation algorithm for equations over Doubles, does not work with vectors or matricies.
 * This is based around reverse polish notation
 * and is optimised for speed at every opportunity.
 * <p>
 * To use do
 * <pre>
 * JEP j = ...;
 * Node node = ...; 
 * RpEval rpe = new RpEval(j);
 * RpCommandList list = rpe.compile(node);
 * double val = rpe.evaluate(list);
 * System.out.println(val);
 * rpe.cleanUp();
 * </pre>
 * The compile methods converts the expression represented by node
 * into a string of commands. For example the expression "1+2*3" will
 * be converted into the sequence of commands
 * <pre>
 * Constant no 1 (pushes constant onto stack)
 * Constant no 2
 * Constant no 3
 * Multiply scalers (multiplies last two entries on stack)
 * Add scalers (adds last two entries on stack)
 * </pre>
 * The evaluate method executes these methods sequentially
 * using a stack 
 * and returns the last object on the stack. 
 * <p>
 * A few cautionary notes:
 * Its very unlikely to be thread safe. It only works over doubles
 * expressions with complex numbers or strings will cause problems.
 * It only works for expressions involving scalers.
 * <p>
 * <b>Implementation notes</b>
 * A lot of things have been done to make it as fast as possible:
 * <ul>
 * <li>Everything is final which maximises the possibility for in-lining.</li>
 * <li>All object creation happens during compile.</li>
 * <li>All calculations done using double values.</li>
 * <li>Each operator/function is hand coded. To extend functionality you will have to modify the source.</li>
 * </ul>
 *  
 * @author Rich Morris
 * Created on 14-Apr-2004
 */
public final class RpEval implements ParserVisitor {

	private OperatorSet opSet;
	private ScalerStore scalerStore = new ScalerStore();
	/** Contains the constant values **/
	double constVals[] = new double[0];

	/** Temporary holder for command list used during compilation */
	private RpCommandList curCommandList;

	public RpEval(JEP jep) {
		this.opSet = jep.getOperatorSet();
	}

	private RpEval() {}
	
	/** Index for each command */
	public static final short CONST = 0;
	public static final short VAR = 1;

	public static final short ADD = 2;
	public static final short SUB = 3;
	public static final short MUL = 4;
	
	public static final short DIV = 5;
	public static final short MOD = 6;
	public static final short POW = 7;

	public static final short AND = 8;
	public static final short OR  = 9;
	public static final short NOT = 10;

	public static final short LT = 11;
	public static final short LE = 12;
	public static final short GT = 13;
	public static final short GE = 14;
	public static final short NE = 15;
	public static final short EQ = 16;
	
	public static final short LIST = 17;
	public static final short DOT = 18;
	public static final short CROSS = 19;

	public static final short ASSIGN = 20;
	public static final short VLIST = 21;
	public static final short MLIST = 22;
	public static final short FUN = 23;
	public static final short UMINUS = 24;
	
	/** Standard functions **/
	
	private static final short SIN = 1;
	private static final short COS = 2;
	private static final short TAN = 3;
	private static final short ASIN = 4;
	private static final short ACOS = 5;
	private static final short ATAN = 6;
	private static final short SINH = 7;
	private static final short COSH = 8;
	private static final short TANH = 9;
	private static final short ASINH = 10;
	private static final short ACOSH = 11;
	private static final short ATANH = 12;
	
	private static final short ABS = 13;
	private static final short EXP = 14;
	private static final short LOG = 15;
	private static final short LN = 16;
	private static final short SQRT = 17;
	
	private static final short SEC = 18;
	private static final short COSEC = 19;
	private static final short COT = 20;
	
	// 2 argument functions
//	private static final short ANGLE = 21;
//	private static final short MODULUS = 22;


	/** Hashtable for function name lookup **/
	
	private static final Hashtable functionHash = new Hashtable();
	{
		functionHash.put("sin",new Short(SIN));
		functionHash.put("cos",new Short(COS));
		functionHash.put("tan",new Short(TAN));
		functionHash.put("asin",new Short(ASIN));
		functionHash.put("acos",new Short(ACOS));
		functionHash.put("atan",new Short(ATAN));
		functionHash.put("sinh",new Short(SINH));
		functionHash.put("cosh",new Short(COSH));
		functionHash.put("tanh",new Short(TANH));
		functionHash.put("asinh",new Short(ASINH));
		functionHash.put("acosh",new Short(ACOSH));
		functionHash.put("atanh",new Short(ATANH));

		functionHash.put("abs",new Short(ABS));
		functionHash.put("exp",new Short(EXP));
		functionHash.put("log",new Short(LOG));
		functionHash.put("ln",new Short(LN));
		functionHash.put("sqrt",new Short(SQRT));

		functionHash.put("sec",new Short(SEC));
		functionHash.put("cosec",new Short(COSEC));
		functionHash.put("cot",new Short(COT));
	}

	

	/**
	 * Base class for storage for each type of data.
	 * Each subclass should define
	 * <pre>
	 * private double stack[];
	 * private double vars[]= new double[0];
	 * </pre>
	 * and the stack is the current data used for calculations.
	 * Data for Variables is stored in vars and references to the Variables
	 * in varRefs. 
	 */
	private abstract static class ObjStore implements Observer {
		/** Contains references to Variables of this type */
		Hashtable varRefs = new Hashtable();
		/** The stack pointer */
		int sp=0;
		/** Maximum size of stack */
		int stackMax=0;
		final void incStack()	{sp++; if(sp > stackMax) stackMax = sp;	}
		final void decStack()	throws ParseException {--sp; if(sp <0 ) throw new ParseException("RPEval: stack error");}
		/** call this to reset pointers as first step in evaluation */
		final void reset() { sp = 0; }
		/** Add a reference to this variable. 
		 * @return the index of variable in table
		 */
		final int addVar(Variable var){
			Object index = varRefs.get(var);
			if(index==null)
			{
				int size = varRefs.size();
				expandVarArray(size+1);
				varRefs.put(var,new Integer(size));
				copyFromVar(var,size);
				var.addObserver(this);
				return size;
			}
			return ((Integer) index).intValue();
		}
		final public void update(Observable obs, Object arg1) 
		{
			Variable var = (Variable) obs;
			Object index = varRefs.get(var);
			copyFromVar(var,((Integer) index).intValue());
		}
		/** allocates space needed */
		abstract void alloc();
		
		final void cleanUp()
		{
			for(Enumeration e=varRefs.keys();e.hasMoreElements();)
			{
				Variable var = (Variable) e.nextElement();
				var.deleteObserver(this);
			}
			varRefs.clear();
		}
		/** Copy variable values into into private storage. 
		 * 
		 * @param var The variable
		 * @param i index of element in array
		 */
		abstract void copyFromVar(Variable var,int i);
		/** expand size of array used to hold variable values. */
		abstract void expandVarArray(int i);
		/** add two objects of same type */
		abstract void add();
		/** subtract two objects of same type */
		abstract void sub();
		/** multiply by a scaler either of left or right */
		abstract void mulS();
		/** assign a variable to stack value
		 * @param i index of variable */
		abstract void assign(int i);
		Variable getVariable(int ref)
		{
			for(Enumeration en=varRefs.keys();en.hasMoreElements();)
			{
				Variable var = (Variable) en.nextElement();
				Integer index = (Integer) varRefs.get(var);
				if(index.intValue()==ref) return var;
			}
			return null;
		}
	}

	private final class ScalerStore extends ObjStore {
		double stack[]=new double[0];
		double vars[]= new double[0];
		final void alloc() { 
			stack = new double[stackMax];
			}
		final void expandVarArray(int size)
		{
			double newvars[] = new double[size];
			System.arraycopy(vars,0,newvars,0,vars.length);
			vars = newvars;
		}
			
		final void copyFromVar(Variable var,int i){
			if(var.hasValidValue())
			{
				Double val = (Double) var.getValue();
				vars[i]=val.doubleValue();
			}
		}
		final void add(){
			double r = stack[--sp];
			stack[sp-1] += r;
		}
		final void sub(){
			double r = stack[--sp];
			stack[sp-1] -= r;
		}
		final void uminus(){
			double r = stack[--sp];
				stack[sp++] = -r;
		}
		final void mulS(){
			double r = stack[--sp];
			stack[sp-1] *= r;
		} 
		final void div(){
			double r = stack[--sp];
			stack[sp-1] /= r;
		} 
		final void mod(){
			double r = stack[--sp];
			stack[sp-1] %= r;
		} 
		final void pow(){
			double r = stack[--sp];
			double l = stack[--sp];
			stack[sp++] = Math.pow(l,r);
		} 
		final void powN(int n){
			double r = stack[--sp];
			switch(n){
				case 0: r = 1.0; break;
				case 1: break;
				case 2: r *= r; break;
				case 3: r *= r*r; break;
				case 4: r *= r*r*r; break;
				case 5: r *= r*r*r*r; break;
				default:
					r = Math.pow(r,n); break;
			}
			stack[sp++] = r;
		} 
		final void assign(int i) {
			vars[i] = stack[--sp]; ++sp;
		} 
		final void and(){
			double r = stack[--sp];
			double l = stack[--sp];
			if((l != 0.0) && (r != 0.0))
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void or(){
			double r = stack[--sp];
			double l = stack[--sp];
			if((l != 0.0) || (r != 0.0))
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void not(){
			double r = stack[--sp];
			if(r == 0.0)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void lt(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l < r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void gt(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l > r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void le(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l <= r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void ge(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l >= r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void eq(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l == r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
		final void neq(){
			double r = stack[--sp];
			double l = stack[--sp];
			if(l != r)
				stack[sp++] = 1.0;
			else
				stack[sp++] = 0.0;
		}
	}
	
	/**
	 * Compile the expressions to produce a set of commands in reverse Polish notation.
	 */
	public final RpCommandList compile(Node node) throws ParseException
	{
		curCommandList = new RpCommandList(this);
		node.jjtAccept(this,null);
		scalerStore.alloc();
		return curCommandList;
	}
	
	public final Object visit(ASTStart node, Object data) throws ParseException {
		throw new ParseException("RpeEval: Start node encountered");
	}
	public final Object visit(SimpleNode node, Object data) throws ParseException {
		throw new ParseException("RpeEval: Simple node encountered");
	}

	public final Object visit(ASTConstant node, Object data) throws ParseException {
		Object obj = node.getValue();
		double val;
		if(obj instanceof Double)
			val = ((Double) node.getValue()).doubleValue();
		else
			throw new ParseException("RpeEval: only constants of double type allowed");
		
		scalerStore.incStack();
		for(short i=0;i<constVals.length;++i)
		{
			if(val == constVals[i])
			{
				curCommandList.addCommand(CONST,i);
				return null;
			}
		}
		// create a new const
		double newConst[] = new double[constVals.length+1];
		System.arraycopy(constVals,0,newConst,0,constVals.length);
		newConst[constVals.length] = val;
		curCommandList.addCommand(CONST,(short) constVals.length);
		constVals = newConst;
		return null;
	}

	public final Object visit(ASTVarNode node, Object data) throws ParseException 
	{
		Variable var = node.getVar();
		// find appropriate table
		short vRef = (short) scalerStore.addVar(var);
		scalerStore.incStack();
		curCommandList.addCommand(VAR,vRef);
		return null;
	}

	public final Object visit(ASTFunNode node, Object data) throws ParseException 
	{
		int nChild = node.jjtGetNumChildren();

		if(node.getPFMC() instanceof SpecialEvaluationI )
		{				
		}
		else
			node.childrenAccept(this,null);

		if(node.isOperator())
		{
			Operator op = node.getOperator();

			if(op == opSet.getAdd())
			{
				curCommandList.addCommand(ADD);
				scalerStore.decStack();
				return null;
			}
			else if(op == opSet.getSubtract())
			{
				curCommandList.addCommand(SUB);
				scalerStore.decStack();
				return null;
			}
			else if(op == opSet.getUMinus())
			{
				curCommandList.addCommand(UMINUS);
				return null;
			}
			else if(op == opSet.getMultiply())
			{
				scalerStore.decStack();
				curCommandList.addCommand(MUL);
				return null;
			}
			else if(op == opSet.getAssign())
			{
				Node rightnode = node.jjtGetChild(1);
				rightnode.jjtAccept(this,null);
				Variable var = ((ASTVarNode)node.jjtGetChild(0)).getVar();
				short vRef = (short) scalerStore.addVar(var);
				scalerStore.decStack();
				curCommandList.addCommand(ASSIGN,vRef);
				return null;
			}
			else if(op == opSet.getEQ())
			{
				scalerStore.decStack();
				curCommandList.addCommand(EQ); return null;
			}
			else if(op == opSet.getNE())
			{
				scalerStore.decStack();
				curCommandList.addCommand(NE); return null;
			}
			else if(op == opSet.getLT())
			{
				scalerStore.decStack();
				curCommandList.addCommand(LT); return null;
			}
			else if(op == opSet.getGT())
			{
				scalerStore.decStack();
				curCommandList.addCommand(GT); return null;
			}
			else if(op == opSet.getLE())
			{
				scalerStore.decStack();
				curCommandList.addCommand(LE); return null;
			}
			else if(op == opSet.getGE())
			{
				scalerStore.decStack();
				curCommandList.addCommand(GE); return null;
			}
			else if(op == opSet.getAnd())
			{
				scalerStore.decStack();
				curCommandList.addCommand(AND); return null;
			}
			else if(op == opSet.getOr())
			{
				scalerStore.decStack();
				curCommandList.addCommand(OR); return null;
			}
			else if(op == opSet.getNot())
			{
				//scalerStore.decStack();
				curCommandList.addCommand(NOT); return null;
			}
			else if(op == opSet.getDivide())
			{
				scalerStore.decStack();
				curCommandList.addCommand(DIV); return null;
			}
			else if(op == opSet.getMod())
			{
				scalerStore.decStack();
				curCommandList.addCommand(MOD); return null;
			}
			else if(op == opSet.getPower())
			{
				scalerStore.decStack();
				curCommandList.addCommand(POW); return null;
			}
			throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ node.getName());
		}
		// other functions
		
		Short val = (Short) functionHash.get(node.getName());
		if(val == null)
			throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ node.getName());
		if(node.getPFMC().getNumberOfParameters() == 1 && nChild == 1)
		{
			//scalerStore.decStack();
			curCommandList.addCommand(FUN,val.shortValue()); 
			return null;
		}

		throw new ParseException("RpeEval: sorry can currently only support single argument functions");
	}

	/***************************** evaluation *****************************/
	
	/** Evaluate the expression.
	 * 
	 * @return the double value of the equation
	 */
	public final double evaluate(RpCommandList comList)
	{
		scalerStore.reset();
	
		// Now actually process the commands
		int num = comList.getNumCommands();
		for(short commandNum=0;commandNum<num;++commandNum)
		{
			RpCommand command = comList.commands[commandNum];
			short aux1 = command.aux1;
			switch(command.command)
			{
			case CONST:
				scalerStore.stack[scalerStore.sp++]=constVals[aux1]; break;
			case VAR:
				scalerStore.stack[scalerStore.sp++]=scalerStore.vars[aux1]; break;
				
			case ADD: scalerStore.add(); break;
			case SUB: scalerStore.sub(); break; 
			case MUL: scalerStore.mulS(); break;
			case DIV: scalerStore.div(); break;
			case MOD: scalerStore.mod(); break;
			case POW: scalerStore.pow(); break;

			case AND: scalerStore.and(); break;
			case OR:  scalerStore.or(); break;
			case NOT: scalerStore.not(); break;

			case LT: scalerStore.lt(); break;
			case LE: scalerStore.le(); break;
			case GT: scalerStore.gt(); break;
			case GE: scalerStore.ge(); break;
			case NE: scalerStore.neq(); break;
			case EQ: scalerStore.eq(); break;
			case ASSIGN: scalerStore.assign(aux1); break;
			case FUN: unitaryFunction(aux1); break;
			case UMINUS: scalerStore.uminus(); break;
			}
		}

		return scalerStore.stack[--scalerStore.sp];
	}

	
	private static final double LOG10 = Math.log(10.0);

	private final void unitaryFunction(short fun)
	{
		double r = scalerStore.stack[--scalerStore.sp];
		switch(fun) {
			case SIN: r = Math.sin(r); break;
			case COS: r = Math.cos(r); break;
			case TAN: r = Math.tan(r); break;

			case ASIN: r = Math.asin(r); break;
			case ACOS: r = Math.acos(r); break;
			case ATAN: r = Math.atan(r); break;

			case SINH: r = (Math.exp(r)-Math.exp(-r))/2; break;
			case COSH: r = (Math.exp(r)+Math.exp(-r))/2; break;
			case TANH: 
				{double ex = Math.exp(r*2);
				 r = (ex-1)/(ex+1); break;
				}

			case ASINH: r = Math.log(r+Math.sqrt(1+r*r)); break;
			case ACOSH: r = Math.log(r+Math.sqrt(r*r-1)); break;
			case ATANH: r = Math.log((1+r)/(1-r))/2.0; break;

			case ABS: r = Math.abs(r); break;
			case EXP: r = Math.exp(r); break;
			case LOG: r = Math.log(r) / LOG10; break;
			case LN:  r = Math.log(r); break;
			case SQRT: r = Math.sqrt(r); break;

			case SEC: r = 1.0/Math.cos(r); break;
			case COSEC:  r = 1.0/Math.sin(r); break;
			case COT: r = 1.0/Math.tan(r); break;
		}
		scalerStore.stack[scalerStore.sp++] = r;
	}
	
	/**
	 * Removes observers and other cleanup needed when evaluator no longer used.
	 */
	public void cleanUp()
	{
		scalerStore.cleanUp();
	}
	
	public Variable getVariable(int ref)
	{
		return scalerStore.getVariable(ref);
	}
	
	public String getFunction(short ref)
	{
			switch(ref) {
			case SIN: return "sin";
			case COS: return "cos";
			case TAN: return "tan";
			case ASIN: return "asin";
			case ACOS: return "acos";
			case ATAN: return "atan";
			case SINH: return "sinh";
			case COSH: return "cosh";
			case TANH: return "tanh";
			case ASINH: return "asinh";
			case ACOSH: return "acosh";
			case ATANH: return "atanh";
			case ABS: return "abs";
			case EXP: return "exp";
			case LOG: return "log";
			case LN: return "ln";
			case SQRT: return "sqrt";
			case SEC: return "sec";
			case COSEC: return "cosec";
			case COT: return "cot";
			}
			return null;
	}
}
