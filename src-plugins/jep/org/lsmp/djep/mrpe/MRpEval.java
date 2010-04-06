/* @author rich
 * Created on 14-Apr-2004
 */
package org.lsmp.djep.mrpe;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.vectorJep.*;
import org.lsmp.djep.vectorJep.values.*;
import org.lsmp.djep.xjep.*;
import java.util.*;
/**
 * A fast evaluation algorithm for equations using Vectors and Matrix over the Doubles.
 * This is based around reverse polish notation (hence the name M Rp Eval)
 * and is optimised for speed at every opportunity.
 * <p>
 * To use do
 * <pre>
 * MatrixJep j = ...;
 * Node node = ...; 
 * MRpEval rpe = new MRpEval(j);
 * MRpCommandList list = rpe.compile(node);
 * MRpRes rpRes = rpe.evaluate(list);
 * System.out.println(rpRes.toString());
 * MatrixValueI mat = rpRes.toVecMat();
 * rpe.cleanUp();
 * </pre>
 * 
 * <p>
 * The real use of this class is when an equation (or set of equations)
 * need to be repeatedly evaluated with different values for the variables.
 * MRpEval use an internal store for variable values different from those
 * used in the main Jep classes. Changes in the Jep variable values, 
 * say by calling {@link org.nfunk.jep.JEP#setVarValue JEP.setVarValue},
 * are reflected
 * in changes in MRpEval variables, (the converse does not hold).
 * A more efficient way is to use <code>int ref=getVarRef(var)</code>
 * to return an index number of the variable and then calling
 * <code>setVarVal(ref,value)</code> to set its value.
 * For example
 * <pre>
 * MRpCommandList list = rpe.compile(node);
 * int ref = rpe.getVarRef(j.getVar("x"));
 * for(double x=-1.;x<1.001;x+=0.1) {
 *      rpe.setVarVal(ref,x);
 *      rpe.evaluate(list);
 * }
 * </pre>
 * 
 * <p>
 * Combining mrpe with differentation requires special techniques
 * to cope with that fact that internal equations are used
 * <p>
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
 * using a stack (actually a set of stacks)
 * and returns the last object on the stack. 
 * <p>
 * A few cautionary notes: the values returned by evaluate
 * are references to internal variables, their values will change
 * at the next call to compile or evaluate.
 * Its very unlikely to be thread safe. It only works over doubles;
 * expressions with complex numbers or strings will cause problems.
 * It is tuned to work best for expressions involving scalers and 2, 3 and 4 dimensional vectors and matricies,
 * larger vectors and matrices will be noticeably slower.
 * The cleanUp function should be used when you no longer need
 * the evaluator, this stops the evaluator listening to Variable
 * through the java.util.Observer interface.
 * <p>
 * <b>Implementation notes</b>
 * A lot of things have been done to make it as fast as possible:
 * <ul>
 * <li>Everything is final which maximises the possibility for in-lining.</li>
 * <li>All object creation happens during compile.</li>
 * <li>All calculations done using double values.</li>
 * <li>Vectors and Matrices are instances of VecObj and MatObj optimised for speed.
 * For instance a 2 by 2 matrix is an instance of Mat22Obj whose elements
 * are represented by the fields a,b,c,d. This eliminates bound checking on arrays.
 * </li>
 * <li>Each possible vector and matrix operation has been hand coded, and there are
 * a lot of methods (27 just for matrix multiplication!).</li>
 * <li>The values of variables are kept on local arrays for fast access. 
 * These values are kept in sync with the main Jep Variables by using
 * the java.util.Observer interface.</li> 
 * </ul>
 *  
 * <p>
 * For each type of vector or matrix (i.e. 2D vecs, 3D vecs, 4D vecs, 2 by 2 matrices ... 4 by 4 matrices.
 * there is a corresponding class V2Obj, M22Obj etc.
 * which stores the values and another class V2Store, M22Store etc.
 * Each Store class contains a stack, a heap and a array of variable values.
 * During evaluation objects are pushed and popped from the stack
 * when a new object is needed it is taken from the heap.
 * The operation is illustrated by the add method for 2 by 2 matrices.
 * <pre>
 * private final class M22Store
 * {
 *  ....
 *  final void add(){
 *   M22Obj r = stack[--sp]; // pop from stack
 *   M22Obj l = stack[--sp]; // pop from stack
 *	 M22Obj res = heap[hp++]; // result is next entry in heap
 *	 res.a = l.a+r.a;	// add each componant
 *	 res.b = l.b+r.b;
 *	 res.c = l.c+r.c;
 *	 res.d = l.d+r.d;
 *	 stack[sp++]=res;	// push result onto stack
 *  }
 * }
 * </pre>
 *
 * @author Rich Morris
 * Created on 14-Apr-2004
 */
public final class MRpEval implements ParserVisitor {

	private MatrixOperatorSet opSet;

	public MRpEval(MatrixJep mjep) {
		this.opSet = (MatrixOperatorSet) mjep.getOperatorSet();
	}

	private MRpEval() {}
	
	/** compile an expression of the type var = node. */
	public final MRpCommandList compile(MatrixVariableI var,Node node) throws ParseException
	{
		MRpCommandList list = compile(node);
		ObjStore store = getStoreByDim(var.getDimensions());
		short vRef = (short) store.addVar(var);
		store.decStack();
		list.addCommand(ASSIGN,getDimType(var.getDimensions()),vRef);
		return list;
	}
	
	/**
	 * Compile the expressions to produce a set of commands in reverse Polish notation.
	 */
	public final MRpCommandList compile(Node node) throws ParseException
	{
		curCommandList = new MRpCommandList();

		node.jjtAccept(this,null);

		scalerStore.alloc();
		v2Store.alloc();
		v3Store.alloc();
		v4Store.alloc();
		vnStore.alloc();
		m22Store.alloc();
		m23Store.alloc();
		m24Store.alloc();
		m32Store.alloc();
		m33Store.alloc();
		m34Store.alloc();
		m42Store.alloc();
		m43Store.alloc();
		m44Store.alloc();
		mnnStore.alloc();

		Dimensions dims = ((MatrixNodeI) node).getDim();
		curCommandList.setFinalType(getDimType(dims));
//		returnObj = Tensor.getInstance(dims);
//		if(dims.is2D())
//			returnMat = (Matrix) returnObj;
		return curCommandList;
	}

	/** Index for each command */
	static final short CONST = 0;
	static final short VAR = 1;

	static final short ADD = 2;
	static final short SUB = 3;
	static final short MUL = 4;
	
	static final short DIV = 5;
	static final short MOD = 6;
	static final short POW = 7;

	static final short AND = 8;
	static final short OR  = 9;
	static final short NOT = 10;

	static final short LT = 11;
	static final short LE = 12;
	static final short GT = 13;
	static final short GE = 14;
	static final short NE = 15;
	static final short EQ = 16;
	
	static final short LIST = 17;
	static final short DOT = 18;
	static final short CROSS = 19;

	static final short ASSIGN = 20;
	static final short VLIST = 21;
	static final short MLIST = 22;
	static final short FUN = 23;
	static final short UMINUS = 24;
	
	/** Constant type scalers - used in the aux field of RpCommand */
	private static final short SCALER = 0; // Scalers
	private static final short V2 = 2; // 2D vect
	private static final short V3 = 3;
	private static final short V4 = 4;
	private static final short Vn = 5; // n D vec
	private static final short M22 = 6; // 2 by 2 mat
	private static final short M23 = 7; // 2 by 3 mat
	private static final short M24 = 8;
	private static final short M32 = 9;
	private static final short M33 = 10;
	private static final short M34 = 11;
	private static final short M42 = 12;
	private static final short M43 = 13;
	private static final short M44 = 14;
	private static final short Mnn = 15; // other mats
	private static final short Dtens = 16; // tensors
	
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
	/** Contains the constant values **/
	private double constVals[] = new double[0];
	/**
	 * Finds the reference number used for this variable.
	 * @param var
	 * @return an index used to refer to the variable
	 * @throws ParseException
	 */
	public int getVarRef(Variable var) throws ParseException
	{
		Dimensions dims = ((MatrixVariableI)var).getDimensions();
		ObjStore store = getStoreByDim(dims);
		int ref = store.addVar((MatrixVariableI) var);
		return ref;
	}
	/**
	 * Finds the reference number used for this variable.
	 * @param var
	 * @return an index used to refer to the variable
	 * @throws ParseException
	 */
	public int getVarRef(MatrixVariableI var) throws ParseException
	{
		Dimensions dims = var.getDimensions();
		ObjStore store = getStoreByDim(dims);
		int ref = store.addVar(var);
		return ref;
	}

	/**
	 * Sets value of rpe variable.
	 * 
	 * @param ref the reference number for the variable 
	 * (found using {@link #getVarRef(org.lsmp.djep.matrixJep.MatrixVariableI)})
	 * @param val
	 * @throws ParseException
	 */
	public final void setVarValue(int ref,MatrixValueI val)
		throws ParseException
	{
		ObjStore store = getStoreByDim(val.getDim());
		store.setVarValue(ref,val);
	}
	/**
	 * Sets value of rpe variable. 
	 * Only applies to scaler (double variables).
	 * 
	 * @param ref the reference number for the variable
	 * (found using {@link #getVarRef(org.lsmp.djep.matrixJep.MatrixVariableI)})
	 * @param val the value
	 */
	public final void setVarValue(int ref,double val)
	{
		scalerStore.setVarValue(ref,val);
	}
	
	private final static class ScalerObj extends MRpRes {
		double a;
		private ScalerObj(double val) {a =val; }
		public final Dimensions getDims() { return Dimensions.ONE; }
		public final void copyToVecMat(MatrixValueI res)  throws ParseException {
			if(! res.getDim().is0D()) throw new ParseException("CopyToVecMat: dimension of argument "+res.getDim()+" is not equal to dimension of object "+getDims());
			res.setEle(0,new Double(a));
		}
		public final String toString() { return String.valueOf(a); }
		public Object toArray() { return new double[]{a}; }
	}
	private ScalerObj scalerRes = new ScalerObj(0.0);
	
	private abstract static class VecObj extends MRpRes {
		public final void copyToVecMat(MatrixValueI res)  throws ParseException {
			if(! getDims().equals(res.getDim())) throw new ParseException("CopyToVecMat: dimension of argument "+res.getDim()+" is not equal to dimension of object "+getDims());
			copyToVec((MVector) res);
		}
		public abstract void copyToVec(MVector res);
		abstract double[] toArrayVec();
		public Object toArray() { return toArrayVec();	}
		/**
		 * Sets the value of th vector frm an array.
		 */
//		public abstract void fromArray(double array[]);
	}
	
	private abstract static class MatObj extends MRpRes  {
		public final void copyToVecMat(MatrixValueI res)  throws ParseException {
			if(! getDims().equals(res.getDim())) throw new ParseException("CopyToVecMat: dimension of argument "+res.getDim()+" is not equal to dimension of object "+getDims());
			copyToMat((Matrix) res);
		}
		public abstract void copyToMat(Matrix res);
		abstract double[][] toArrayMat();
		public Object toArray() { return toArrayMat();	}
	}
	/**
	 * Base class for storage for each type of data.
	 * Each subclass should define
	 * <pre>
	 * private Obj stack[];
	 * private Obj heap[];
	 * private Obj vars[]= new Obj[0];
	 * </pre>
	 * where Obj is an Object of the specific type, eg V2Obj.
	 * Memory for the data is allocated from the heap
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
		/** The heap pointer */ 
		int hp=0;
		final void incStack()	{sp++; if(sp > stackMax) stackMax = sp;	}
		final void incHeap()	{hp++;}
		final void decStack()	throws ParseException {--sp; if(sp <0 ) throw new ParseException("RPEval: stack error");}
		/** call this to reset pointers as first step in evaluation */
		final void reset() { sp = 0; hp = 0; }
		/** Add a reference to this variable. 
		 * @return the index of variable in table
		 */
		final int addVar(MatrixVariableI var){
			Object index = varRefs.get(var);
			if(index==null)
			{
				int size = varRefs.size();
				expandVarArray(var);
				varRefs.put(var,new Integer(size));
				copyFromVar(var,size);
				((Variable) var).addObserver(this);
				return size;
			}
			return ((Integer) index).intValue();
		}
		/** Callback function for Observable Variables.
		 * Called whenever the value of a variable is changed
		 * so the private list of variables is kept in sync.
		 */
		final public void update(Observable obs, Object arg1) 
		{
			MatrixVariableI var = (MatrixVariableI) obs;
			Object index = varRefs.get(var);
			copyFromVar(var,((Integer) index).intValue());
		}
		abstract public void setVarValue(int ref,MatrixValueI val);
		/** allocates space needed */
		abstract void alloc();
		/** removed store from list of listeners. */
		final void cleanUp()
		{
			for(Enumeration e=varRefs.keys();e.hasMoreElements();)
			{
				Variable var = (Variable) e.nextElement();
				var.deleteObserver(this);
			}
			varRefs.clear();
		}
		/** Copy variable values into into private storage */
		abstract void copyFromVar(MatrixVariableI var,int i);
		/** Copy values from private storage into JEP variables */
//		abstract void copyToVar(MatrixVariableI var,int i);
		/** expand size of array used to hold variable values. */
		abstract void expandVarArray(MatrixVariableI var);
		/** add two objects of same type */
		abstract void add();
		/** subtract two objects of same type */
		abstract void sub();
		/** subtract two objects of same type */
		abstract void uminus();
		/** multiply by a scaler either of left or right */
		abstract void mulS();
		/** convert a set of scaler values into object of this type */
		abstract void makeList();
		/** assign a variable to stack value
		 * @param i index of variable */
		abstract void assign(int i);
	}

	private final class ScalerStore extends ObjStore {
		double stack[]=new double[0];
		double vars[]= new double[0];
		final void alloc() { 
			stack = new double[stackMax];
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			double newvars[] = new double[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			vars = newvars;
		}
		final void copyFromVar(MatrixVariableI var,int i)
		{
			if(var.hasValidValue())
			{
				Scaler val = (Scaler) var.getMValue();
				vars[i]=val.doubleValue();
			}
		}
		public void setVarValue(int ref, double val) {
			vars[ref] = val;
		}
		public final void setVarValue(int ref,MatrixValueI val)
		{
				vars[ref]=((Scaler) val).doubleValue();
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
		final void divS(){
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
		final void makeList() {
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
	ScalerStore scalerStore = new ScalerStore();

	/** Base class for vector type storage */
	private abstract class VecStore extends ObjStore {
		abstract void copyVar(int i,MVector val);
		final void copyFromVar(MatrixVariableI var,int i)
		{
			if(var.hasValidValue())
			{
				MVector val = (MVector) ((MatrixVariable) var).getMValue();
				copyVar(i,val);
			}
		}
		
		public final void setVarValue(int ref, MatrixValueI val) {
			copyVar(ref,(MVector) val);
		}
	}

	private static final class V2Obj extends VecObj {
		double a,b;

		private static Dimensions dims = Dimensions.TWO;
		public Dimensions getDims() { return dims;	}
		public String toString() { return "["+a+","+b+"]"; }
		public void fromVec(MVector val){
			a = ((Double) val.getEle(0)).doubleValue();
			b = ((Double) val.getEle(1)).doubleValue();
		}
		public void copyToVec(MVector val){
			val.setEle(0,new Double(a));
			val.setEle(1,new Double(b));
		}
		public double[] toArrayVec() { return new double[]{a,b}; }
	}
	private final class V2Store extends VecStore {
		V2Obj stack[];
		V2Obj heap[];
		V2Obj vars[]= new V2Obj[0];
		final void alloc() { 
			heap = new V2Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new V2Obj();
			stack = new V2Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new V2Obj();
		}

		final void expandVarArray(MatrixVariableI var)
		{ 
			V2Obj newvars[] = new V2Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new V2Obj();
			vars = newvars;
		}
		final void copyVar(int i,MVector vec) { vars[i].fromVec(vec); }
		final void add(){
			V2Obj r = stack[--sp];
			V2Obj l = stack[--sp];
			V2Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			stack[sp++]=res;	
		}
		final void sub(){
			V2Obj r = stack[--sp];
			V2Obj l = stack[--sp];
			V2Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			stack[sp++]=res;	
		}
		final void uminus(){
			V2Obj r = stack[--sp];
			V2Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			V2Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			V2Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			stack[sp++]=res;
		} 
		final void divS()
		{
			V2Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			V2Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			stack[sp++]=res;
		} 
		final void mulV2() { // treat as complex mult
			V2Obj r = stack[--sp];
			V2Obj l = stack[--sp];
			V2Obj res = heap[hp++];
			res.a = l.a*r.a-l.a*r.b;
			res.b = l.a*r.b+l.b*r.a;
			stack[sp++]=res;	
		}
		final void makeList() {
			V2Obj res = heap[hp++];
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i)
		{
			V2Obj r = stack[--sp];  ++sp;
			V2Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
		} 
		final void eq(){
			V2Obj r = stack[--sp];
			V2Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b)
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private V2Store v2Store = new V2Store();

	private static final class V3Obj extends VecObj {
		double a,b,c;

		private static Dimensions dims = Dimensions.THREE;
		public Dimensions getDims() { return dims;	}
		public String toString() { return "["+a+","+b+","+c+"]"; }
		public void fromVec(MVector val){
			a = ((Double) val.getEle(0)).doubleValue();
			b = ((Double) val.getEle(1)).doubleValue();
			c = ((Double) val.getEle(2)).doubleValue();
		}
		public void copyToVec(MVector val){
			val.setEle(0,new Double(a));
			val.setEle(1,new Double(b));
			val.setEle(2,new Double(c));
		}
		public double[] toArrayVec() { return new double[]{a,b,c}; }
	}
	private final class V3Store extends VecStore {
		V3Obj stack[];
		V3Obj heap[];
		V3Obj vars[]= new V3Obj[0];
		final void alloc() { 
			heap = new V3Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new V3Obj();
			stack = new V3Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new V3Obj();
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			V3Obj newvars[] = new V3Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new V3Obj();
			vars = newvars;
		}
		final void copyVar(int i,MVector vec) { vars[i].fromVec(vec); }
		final void add(){
			V3Obj r = stack[--sp];
			V3Obj l = stack[--sp];
			V3Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;
			stack[sp++]=res;	
		}
		final void sub(){
			V3Obj r = stack[--sp];
			V3Obj l = stack[--sp];
			V3Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;
			stack[sp++]=res;	
		}
		final void uminus(){
			V3Obj r = stack[--sp];
			V3Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			V3Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			V3Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			stack[sp++]=res;
		} 
		final void divS()
		{
			V3Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			V3Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			V3Obj res = heap[hp++];
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			V3Obj r = stack[--sp];  ++sp;
			V3Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
		} 
		final void eq(){
			V3Obj r = stack[--sp];
			V3Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private V3Store v3Store = new V3Store();

	private static final class V4Obj extends VecObj {
		double a,b,c,d;

		private static Dimensions dims = Dimensions.valueOf(4);
		public Dimensions getDims() { return dims;	}
		public String toString() { return "["+a+","+b+","+c+","+d+"]"; }
		public void fromVec(MVector val){
			a = ((Double) val.getEle(0)).doubleValue();
			b = ((Double) val.getEle(1)).doubleValue();
			c = ((Double) val.getEle(2)).doubleValue();
			d = ((Double) val.getEle(3)).doubleValue();
		}
		public void copyToVec(MVector val){
			val.setEle(0,new Double(a));
			val.setEle(1,new Double(b));
			val.setEle(2,new Double(c));
			val.setEle(3,new Double(d));
		}
		public double[] toArrayVec() { return new double[]{a,b,c,d}; }
	}
	private final class V4Store extends VecStore {
		V4Obj stack[];
		V4Obj heap[];
		V4Obj vars[]= new V4Obj[0];
		final void alloc() { 
			heap = new V4Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new V4Obj();
			stack = new V4Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new V4Obj();
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			V4Obj newvars[] = new V4Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new V4Obj();
			vars = newvars;
		}
		final void copyVar(int i,MVector vec) { vars[i].fromVec(vec); }
		final void add(){
			V4Obj r = stack[--sp];
			V4Obj l = stack[--sp];
			V4Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;
			res.d = l.d+r.d;
			stack[sp++]=res;	
		}
		final void sub(){
			V4Obj r = stack[--sp];
			V4Obj l = stack[--sp];
			V4Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;
			res.d = l.d-r.d;
			stack[sp++]=res;	
		}
		final void uminus(){
			V4Obj r = stack[--sp];
			V4Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			V4Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			V4Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			stack[sp++]=res;
		} 
		final void divS()
		{
			V4Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			V4Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			V4Obj res = heap[hp++];
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			V4Obj r = stack[--sp];  ++sp;
			V4Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
		} 
		final void eq(){
			V4Obj r = stack[--sp];
			V4Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private V4Store v4Store = new V4Store();

	private static final class VnObj extends VecObj {
		double data[];
	
		VnObj(int len) { data = new double[len]; }
		VnObj(double vec[]) { data = vec; }
		
		public Dimensions getDims() { return Dimensions.valueOf(data.length);	}
		public String toString() { 
			StringBuffer sb = new StringBuffer("[");
			for(int i=0;i<data.length;++i){
				if(i>0) sb.append(",");
				sb.append(data[i]);
			}
			sb.append("]");
			return sb.toString();
		}
		public void fromVec(MVector val){
			for(int i=0;i<data.length;++i)
				data[i] = ((Double) val.getEle(i)).doubleValue();
		}
		public void copyToVec(MVector val){
			for(int i=0;i<data.length;++i)
				val.setEle(i,new Double(data[i]));
		}
		double[] toArrayVec() { return data; }
		public Object toArray() { 
			double res[] = new double[data.length];
			System.arraycopy(data,0,res,0,data.length);
			return res;
		}
	}
	private final class VnStore extends VecStore {
		VnObj stack[];
		VnObj vars[]= new VnObj[0];
		final void alloc() { 
			stack = new VnObj[stackMax];
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			VnObj newvars[] = new VnObj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new VnObj(var.getDimensions().getFirstDim());
			vars = newvars;
		}
		final void copyVar(int i,MVector vec) { vars[i].fromVec(vec); }
		final void add(){
			VnObj r = stack[--sp];
			VnObj l = stack[--sp];
			VnObj res = new VnObj(l.data.length);
			for(int i=0;i<l.data.length;++i)
				res.data[i]=l.data[i]+r.data[i];
			stack[sp++]=res;	
		}
		final void sub(){
			VnObj r = stack[--sp];
			VnObj l = stack[--sp];
			VnObj res = new VnObj(l.data.length);
			for(int i=0;i<l.data.length;++i)
				res.data[i]=l.data[i]-r.data[i];
			stack[sp++]=res;	
		}
		final void uminus(){
			VnObj r = stack[--sp];
			VnObj res = new VnObj(r.data.length);
			for(int i=0;i<r.data.length;++i)
				res.data[i]=-r.data[i];
			stack[sp++]=res;	
		}
		final void mulS()
		{
			VnObj r = stack[--sp];
			double l = scalerStore.stack[--scalerStore.sp];
			VnObj res = new VnObj(r.data.length);
			for(int i=0;i<r.data.length;++i)
				res.data[i]=l * r.data[i];
			stack[sp++]=res;
		} 
		final void divS()
		{
			VnObj l = stack[--sp];
			double r = scalerStore.stack[--scalerStore.sp];
			VnObj res = new VnObj(l.data.length);
			for(int i=0;i<l.data.length;++i)
				res.data[i]=l.data[i]/r;
			stack[sp++]=res;
		} 
		final void makeList(int num) {
			VnObj res = new VnObj(num);
			for(int i=num-1;i>=0;--i)
				res.data[i] = scalerStore.stack[--scalerStore.sp]; 
			stack[sp++]=res;
		} 
		final void makeList() {
			throw new UnsupportedOperationException("VnObj: makeList cannot be called with no arguments");
		} 
		final void assign(int j) {
			VnObj r = stack[sp-1];
			VnObj res = vars[j];
			for(int i=0;i<r.data.length;++i)
				res.data[i]=r.data[i];
		} 
		final void eq(){
			VnObj r = stack[--sp];
			VnObj l = stack[--sp];
			for(int i=0;i<r.data.length;++i){
				if(l.data[i]!=r.data[i]){
					scalerStore.stack[scalerStore.sp++] = 0.0;
					return;
				}
			}
			scalerStore.stack[scalerStore.sp++] = 1.0;
		}
	}
	private VnStore vnStore = new VnStore();
	
	private static abstract class MatStore extends ObjStore {
		abstract void copyVar(int i,Matrix val);
		final void copyFromVar(MatrixVariableI var,int i)
		{
			if(var.hasValidValue())
			{
				Matrix val = (Matrix) ((MatrixVariable) var).getMValue();
				copyVar(i,val);
			}
		}
		
/*		final void copyVars(){
			int i=0;
			for(Enumeration e=varRefs.elements();e.hasMoreElements();)
			{
				MatrixVariable var = (MatrixVariable) e.nextElement();
				Matrix val = (Matrix) var.getMValue();
				if(var.hasValidValue())
					copyVar(i,val);
				++i;
			}
		}
*/
		final public void setVarValue(int ref, MatrixValueI val) {
			copyVar(ref,(Matrix) val);
		}
	}
	private static final class M22Obj extends MatObj {
		double a,b, c,d;

		static Dimensions dims = Dimensions.valueOf(2,2);
		public Dimensions getDims() { return dims;	}
		public String toString() { 
			return "[["+a+","+b+"],"+ 
					"["+c+","+d+"]]"; 
		}
		public void fromMat(Matrix val){
			a = ((Double) val.getEle(0,0)).doubleValue();
			b = ((Double) val.getEle(0,1)).doubleValue();
			c = ((Double) val.getEle(1,0)).doubleValue();
			d = ((Double) val.getEle(1,1)).doubleValue();
		}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(1,0,new Double(c));
			val.setEle(1,1,new Double(d));
		}
		public double[][] toArrayMat() { return new double[][]{{a,b},{c,d}}; }
	}
	private final class M22Store extends MatStore {
		M22Obj stack[];
		M22Obj heap[];
		M22Obj vars[]= new M22Obj[0];
		final void alloc() { 
			heap = new M22Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M22Obj();
			stack = new M22Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M22Obj();
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M22Obj newvars[] = new M22Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M22Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M22Obj r = stack[--sp];
			M22Obj l = stack[--sp];
			M22Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;
			res.d = l.d+r.d;
			stack[sp++]=res;	
		}
		final void sub(){
			M22Obj r = stack[--sp];
			M22Obj l = stack[--sp];
			M22Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;
			res.d = l.d-r.d;
			stack[sp++]=res;	
		}
		final void uminus(){
			M22Obj r = stack[--sp];
			M22Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M22Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M22Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			stack[sp++]=res;
		}
		final void divS()
		{
			M22Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M22Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			stack[sp++]=res;
		} 
		final void assign(int i)
		{
			M22Obj r = stack[--sp];  ++sp;
			M22Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
		} 
		final void makeList() {
			M22Obj res = heap[hp++];
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void eq(){
			M22Obj r = stack[--sp];
			M22Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M22Store m22Store = new M22Store();

	private static final class M23Obj extends MatObj {
		double a,b,c, d,e,f;

		static Dimensions dims = Dimensions.valueOf(2,3);
		public Dimensions getDims() { return dims;	}
		public String toString() { 
			return "[["+a+","+b+","+c+"],"+ 
					"["+d+","+e+","+f+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();
				c = ((Double) val.getEle(0,2)).doubleValue();

				d = ((Double) val.getEle(1,0)).doubleValue();
				e = ((Double) val.getEle(1,1)).doubleValue();
				f = ((Double) val.getEle(1,2)).doubleValue();
		}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(0,2,new Double(c));
			val.setEle(1,0,new Double(d));
			val.setEle(1,1,new Double(e));
			val.setEle(1,2,new Double(f));
		}
		public double[][] toArrayMat() { return new double[][]{{a,b,c},{d,e,f}}; }
	}
	private final class M23Store extends MatStore {
		M23Obj stack[];
		M23Obj heap[];
		M23Obj vars[]= new M23Obj[0];
		final void alloc() { 
			heap = new M23Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M23Obj();
			stack = new M23Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M23Obj();
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M23Obj newvars[] = new M23Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M23Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M23Obj r = stack[--sp];
			M23Obj l = stack[--sp];
			M23Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;

			res.d = l.d+r.d;
			res.e = l.e+r.e;
			res.f = l.f+r.f;
			stack[sp++]=res;	
		}
		final void sub(){
			M23Obj r = stack[--sp];
			M23Obj l = stack[--sp];
			M23Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;

			res.d = l.d-r.d;
			res.e = l.e-r.e;
			res.f = l.f-r.f;
			stack[sp++]=res;	
		}
		final void uminus(){
			M23Obj r = stack[--sp];
			M23Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M23Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M23Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			stack[sp++]=res;
		}
		final void divS()
		{
			M23Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M23Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M23Obj res = heap[hp++];
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M23Obj r = stack[sp-1]; 
			M23Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
		} 
		final void eq(){
			M23Obj r = stack[--sp];
			M23Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M23Store m23Store = new M23Store();

	private static final class M24Obj extends MatObj {
		double a,b,c,d, e,f,g,h;

		static Dimensions dims = Dimensions.valueOf(2,4);
		public Dimensions getDims() { return dims;	}
		public String toString() { 
			return "[["+a+","+b+","+c+","+d+"],"+ 
					"["+e+","+f+","+g+","+h+"]]";
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();
				c = ((Double) val.getEle(0,2)).doubleValue();
				d = ((Double) val.getEle(0,3)).doubleValue();

				e = ((Double) val.getEle(1,0)).doubleValue();
				f = ((Double) val.getEle(1,1)).doubleValue();
				g = ((Double) val.getEle(1,2)).doubleValue();
				h = ((Double) val.getEle(1,3)).doubleValue();
		}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(0,2,new Double(c));
			val.setEle(0,3,new Double(d));
			val.setEle(1,0,new Double(e));
			val.setEle(1,1,new Double(f));
			val.setEle(1,2,new Double(g));
			val.setEle(1,3,new Double(h));
		}
		public double[][] toArrayMat() { return new double[][]{{a,b,c,d},{e,f,g,h}}; }
	}
	private final class M24Store extends MatStore {
		M24Obj stack[];
		M24Obj heap[];
		M24Obj vars[]= new M24Obj[0];
		final void alloc() { 
			heap = new M24Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M24Obj();
			stack = new M24Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M24Obj();
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M24Obj newvars[] = new M24Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M24Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M24Obj r = stack[--sp];
			M24Obj l = stack[--sp];
			M24Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;

			res.d = l.d+r.d;
			res.e = l.e+r.e;
			res.f = l.f+r.f;

			res.g = l.g+r.g;
			res.h = l.h+r.h;
			stack[sp++]=res;	
		}
		final void sub(){
			M24Obj r = stack[--sp];
			M24Obj l = stack[--sp];
			M24Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;

			res.d = l.d-r.d;
			res.e = l.e-r.e;
			res.f = l.f-r.f;

			res.g = l.g-r.g;
			res.h = l.h-r.h;
			stack[sp++]=res;	
		}
		final void uminus(){
			M24Obj r = stack[--sp];
			M24Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			res.g = -r.g;
			res.h = -r.h;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M24Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M24Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			res.g = l * r.g;
			res.h = l * r.h;
			stack[sp++]=res;
		}
		final void divS()
		{
			M24Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M24Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			res.g = l.g/r;
			res.h = l.h/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M24Obj res = heap[hp++];
			res.h = scalerStore.stack[--scalerStore.sp]; 
			res.g = scalerStore.stack[--scalerStore.sp]; 
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M24Obj r = stack[sp-1]; 
			M24Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
			res.g = r.g;
			res.h = r.h;
		} 
		final void eq(){
			M24Obj r = stack[--sp];
			M24Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f && l.g == r.g && l.h == r.h ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M24Store m24Store = new M24Store();

	private static final class M32Obj extends MatObj {
		double a,b, c,d, e,f;

		static Dimensions dims = Dimensions.valueOf(3,2);
		public Dimensions getDims() { return dims;	}
		public String toString() { 
			return "[["+a+","+b+"],"+ 
					"["+c+","+d+"],"+ 
					"["+e+","+f+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();

				c = ((Double) val.getEle(1,0)).doubleValue();
				d = ((Double) val.getEle(1,1)).doubleValue();

				e = ((Double) val.getEle(2,0)).doubleValue();
				f = ((Double) val.getEle(2,1)).doubleValue();
		}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(1,0,new Double(c));
			val.setEle(1,1,new Double(d));
			val.setEle(2,0,new Double(e));
			val.setEle(2,1,new Double(f));
		}
		public double[][] toArrayMat() { return new double[][]{{a,b},{c,d},{e,f}}; }
	}
	private final class M32Store extends MatStore {
		M32Obj stack[];
		M32Obj heap[];
		M32Obj vars[]= new M32Obj[0];
		final void alloc() { 
			heap = new M32Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M32Obj();
			stack = new M32Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M32Obj();
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M32Obj newvars[] = new M32Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M32Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M32Obj r = stack[--sp];
			M32Obj l = stack[--sp];
			M32Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;

			res.d = l.d+r.d;
			res.e = l.e+r.e;
			res.f = l.f+r.f;
			stack[sp++]=res;	
		}
		final void sub(){
			M32Obj r = stack[--sp];
			M32Obj l = stack[--sp];
			M32Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;
			res.d = l.d-r.d;
			res.e = l.e-r.e;
			res.f = l.f-r.f;
			stack[sp++]=res;	
		}
		final void uminus(){
			M32Obj r = stack[--sp];
			M32Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M32Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M32Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			stack[sp++]=res;
		}
		final void divS()
		{
			M32Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M32Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M32Obj res = heap[hp++];
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M32Obj r = stack[sp-1]; 
			M32Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
		} 
		final void eq(){
			M32Obj r = stack[--sp];
			M32Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M32Store m32Store = new M32Store();

	private static final class M33Obj extends MatObj {
		double a,b,c, d,e,f, g,h,i;
		static Dimensions dims = Dimensions.valueOf(3,3);
		public Dimensions getDims() { return dims;	}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(0,2,new Double(c));
			val.setEle(1,0,new Double(d));
			val.setEle(1,1,new Double(e));
			val.setEle(1,2,new Double(f));
			val.setEle(2,0,new Double(g));
			val.setEle(2,1,new Double(h));
			val.setEle(2,2,new Double(i));
		}
		public String toString() { 
			return "[["+a+","+b+","+c+"],"+ 
					"["+d+","+e+","+f+"],"+ 
					"["+g+","+h+","+i+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();
				c = ((Double) val.getEle(0,2)).doubleValue();

				d = ((Double) val.getEle(1,0)).doubleValue();
				e = ((Double) val.getEle(1,1)).doubleValue();
				f = ((Double) val.getEle(1,2)).doubleValue();

				g = ((Double) val.getEle(2,0)).doubleValue();
				h = ((Double) val.getEle(2,1)).doubleValue();
				i = ((Double) val.getEle(2,2)).doubleValue();
		}
		public double[][] toArrayMat() { return new double[][]{{a,b,c},{d,e,f},{g,h,i}}; }
	}
	private final class M33Store extends MatStore {
		M33Obj stack[];
		M33Obj heap[];
		M33Obj vars[]= new M33Obj[0];
		final void alloc() { 
			heap = new M33Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M33Obj();
			stack = new M33Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M33Obj();
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M33Obj newvars[] = new M33Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M33Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M33Obj r = stack[--sp];
			M33Obj l = stack[--sp];
			M33Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;

			res.d = l.d+r.d;
			res.e = l.e+r.e;
			res.f = l.f+r.f;

			res.g = l.g+r.g;
			res.h = l.h+r.h;
			res.i = l.i+r.i;
			stack[sp++]=res;	
		}
		final void sub(){
			M33Obj r = stack[--sp];
			M33Obj l = stack[--sp];
			M33Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;

			res.d = l.d-r.d;
			res.e = l.e-r.e;
			res.f = l.f-r.f;

			res.g = l.g-r.g;
			res.h = l.h-r.h;
			res.i = l.i-r.i;
			stack[sp++]=res;	
		}
		final void uminus(){
			M33Obj r = stack[--sp];
			M33Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			res.g = -r.g;
			res.h = -r.h;
			res.i = -r.i;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M33Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M33Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			res.g = l * r.g;
			res.h = l * r.h;
			res.i = l * r.i;
			stack[sp++]=res;
		} 		
		final void divS()
		{
			M33Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M33Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			res.g = l.g/r;
			res.h = l.h/r;
			res.i = l.i/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M33Obj res = heap[hp++];
			res.i = scalerStore.stack[--scalerStore.sp]; 
			res.h = scalerStore.stack[--scalerStore.sp]; 
			res.g = scalerStore.stack[--scalerStore.sp]; 
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M33Obj r = stack[sp-1]; 
			M33Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
			res.g = r.g;
			res.h = r.h;
			res.i = r.i;
		} 
		final void eq(){
			M33Obj r = stack[--sp];
			M33Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f && l.g == r.g && l.h == r.h 
			  && l.i == r.i ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M33Store m33Store = new M33Store();

	private static final class M34Obj extends MatObj {
		double a,b,c,d, e,f,g,h, i,j,k,l;
		private static Dimensions dims = Dimensions.valueOf(3,4);
		public Dimensions getDims() { return dims;	}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(0,2,new Double(c));
			val.setEle(0,3,new Double(d));
			val.setEle(1,0,new Double(e));
			val.setEle(1,1,new Double(f));
			val.setEle(1,2,new Double(g));
			val.setEle(1,3,new Double(h));
			val.setEle(2,0,new Double(i));
			val.setEle(2,1,new Double(j));
			val.setEle(2,2,new Double(k));
			val.setEle(2,3,new Double(l));
		}
		public String toString() { 
			return "[["+a+","+b+","+c+","+d+"],"+ 
					"["+e+","+f+","+g+","+h+"],"+ 
					"["+i+","+j+","+k+","+l+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();
				c = ((Double) val.getEle(0,2)).doubleValue();
				d = ((Double) val.getEle(0,3)).doubleValue();

				e = ((Double) val.getEle(1,0)).doubleValue();
				f = ((Double) val.getEle(1,1)).doubleValue();
				g = ((Double) val.getEle(1,2)).doubleValue();
				h = ((Double) val.getEle(1,3)).doubleValue();

				i = ((Double) val.getEle(2,0)).doubleValue();
				j = ((Double) val.getEle(2,1)).doubleValue();
				k = ((Double) val.getEle(2,2)).doubleValue();
				l = ((Double) val.getEle(2,3)).doubleValue();
		}
		public double[][] toArrayMat() { return new double[][]{{a,b,c,d},{e,f,g,h},{i,j,k,l}}; }
	}
	private final class M34Store extends MatStore {
		M34Obj stack[];
		M34Obj heap[];
		M34Obj vars[]= new M34Obj[0];
		final void alloc() { 
			heap = new M34Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M34Obj();
			stack = new M34Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M34Obj();
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M34Obj newvars[] = new M34Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M34Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M34Obj r = stack[--sp];
			M34Obj l = stack[--sp];
			M34Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;
			res.d = l.d+r.d;

			res.e = l.e+r.e;
			res.f = l.f+r.f;
			res.g = l.g+r.g;
			res.h = l.h+r.h;

			res.i = l.i+r.i;
			res.j = l.j+r.j;
			res.k = l.k+r.k;
			res.l = l.l+r.l;
			stack[sp++]=res;	
		}
		final void sub(){
			M34Obj r = stack[--sp];
			M34Obj l = stack[--sp];
			M34Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;
			res.d = l.d-r.d;

			res.e = l.e-r.e;
			res.f = l.f-r.f;
			res.g = l.g-r.g;
			res.h = l.h-r.h;

			res.i = l.i-r.i;
			res.j = l.j-r.j;
			res.k = l.k-r.k;
			res.l = l.l-r.l;
			stack[sp++]=res;	
		}
		final void uminus(){
			M34Obj r = stack[--sp];
			M34Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			res.g = -r.g;
			res.h = -r.h;
			res.i = -r.i;
			res.j = -r.j;
			res.k = -r.k;
			res.l = -r.l;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M34Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M34Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			res.g = l * r.g;
			res.h = l * r.h;
			res.i = l * r.i;
			res.j = l * r.j;
			res.k = l * r.k;
			res.l = l * r.l;
			stack[sp++]=res;
		} 
		final void divS()
		{
			M34Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M34Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			res.g = l.g/r;
			res.h = l.h/r;
			res.i = l.i/r;
			res.j = l.j/r;
			res.k = l.k/r;
			res.l = l.l/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M34Obj res = heap[hp++];
			res.l = scalerStore.stack[--scalerStore.sp]; 
			res.k = scalerStore.stack[--scalerStore.sp]; 
			res.j = scalerStore.stack[--scalerStore.sp]; 
			res.i = scalerStore.stack[--scalerStore.sp]; 
			res.h = scalerStore.stack[--scalerStore.sp]; 
			res.g = scalerStore.stack[--scalerStore.sp]; 
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M34Obj r = stack[sp-1]; 
			M34Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
			res.g = r.g;
			res.h = r.h;
			res.i = r.i;
			res.j = r.j;
			res.k = r.k;
			res.l = r.l;
		} 
		final void eq(){
			M34Obj r = stack[--sp];
			M34Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f && l.g == r.g && l.h == r.h 
			  && l.i == r.i && l.j == r.j && l.k == r.k && l.l == r.l ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M34Store m34Store = new M34Store();

	private static final class M42Obj extends MatObj {
		double a,b, c,d, e,f, g,h;

		private static Dimensions dims = Dimensions.valueOf(4,2);
		public Dimensions getDims() { return dims;	}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(1,0,new Double(c));
			val.setEle(1,1,new Double(d));
			val.setEle(2,0,new Double(e));
			val.setEle(2,1,new Double(f));
			val.setEle(3,0,new Double(g));
			val.setEle(3,1,new Double(h));
		}
		public String toString() { 
			return "[["+a+","+b+"],"+ 
					"["+c+","+d+"],"+ 
					"["+e+","+f+"],"+ 
					"["+g+","+h+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();

				c = ((Double) val.getEle(1,0)).doubleValue();
				d = ((Double) val.getEle(1,1)).doubleValue();

				e = ((Double) val.getEle(2,0)).doubleValue();
				f = ((Double) val.getEle(2,1)).doubleValue();

				g = ((Double) val.getEle(3,0)).doubleValue();
				h = ((Double) val.getEle(3,1)).doubleValue();
		}
		public double[][] toArrayMat() { return new double[][]{{a,b},{c,d},{e,f},{g,h}}; }
	}
	private final class M42Store extends MatStore {
		M42Obj stack[];
		M42Obj heap[];
		M42Obj vars[]= new M42Obj[0];
		final void alloc() { 
			heap = new M42Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M42Obj();
			stack = new M42Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M42Obj();
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M42Obj newvars[] = new M42Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M42Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M42Obj r = stack[--sp];
			M42Obj l = stack[--sp];
			M42Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;

			res.c = l.c+r.c;
			res.d = l.d+r.d;

			res.e = l.e+r.e;
			res.f = l.f+r.f;

			res.g = l.g+r.g;
			res.h = l.h+r.h;
			stack[sp++]=res;	
		}
		final void sub(){
			M42Obj r = stack[--sp];
			M42Obj l = stack[--sp];
			M42Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;

			res.d = l.d-r.d;
			res.e = l.e-r.e;
			res.f = l.f-r.f;

			res.g = l.g-r.g;
			res.h = l.h-r.h;
			stack[sp++]=res;	
		}
		final void uminus(){
			M42Obj r = stack[--sp];
			M42Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			res.g = -r.g;
			res.h = -r.h;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M42Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M42Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			res.g = l * r.g;
			res.h = l * r.h;
			stack[sp++]=res;
		}
		final void divS()
		{
			M42Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M42Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			res.g = l.g/r;
			res.h = l.h/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M42Obj res = heap[hp++];
			res.h = scalerStore.stack[--scalerStore.sp]; 
			res.g = scalerStore.stack[--scalerStore.sp]; 
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M42Obj r = stack[sp-1]; 
			M42Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
			res.g = r.g;
			res.h = r.h;
		} 
		final void eq(){
			M42Obj r = stack[--sp];
			M42Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f && l.g == r.g && l.h == r.h )
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M42Store m42Store = new M42Store();

	private static final class M43Obj extends MatObj {
		double a,b,c, d,e,f, g,h,i, j,k,l;

		private static Dimensions dims = Dimensions.valueOf(4,3);
		public Dimensions getDims() { return dims;	}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(0,2,new Double(c));
			val.setEle(1,0,new Double(d));
			val.setEle(1,1,new Double(e));
			val.setEle(1,2,new Double(f));
			val.setEle(2,0,new Double(g));
			val.setEle(2,1,new Double(h));
			val.setEle(2,2,new Double(i));
			val.setEle(3,0,new Double(j));
			val.setEle(3,1,new Double(k));
			val.setEle(3,2,new Double(l));
		}
		public String toString() { 
			return "[["+a+","+b+","+c+"],"+ 
					"["+d+","+e+","+f+"],"+ 
					"["+g+","+h+","+i+"],"+ 
					"["+j+","+k+","+l+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();
				c = ((Double) val.getEle(0,2)).doubleValue();

				d = ((Double) val.getEle(1,0)).doubleValue();
				e = ((Double) val.getEle(1,1)).doubleValue();
				f = ((Double) val.getEle(1,2)).doubleValue();

				g = ((Double) val.getEle(2,0)).doubleValue();
				h = ((Double) val.getEle(2,1)).doubleValue();
				i = ((Double) val.getEle(2,2)).doubleValue();

				j = ((Double) val.getEle(3,0)).doubleValue();
				k = ((Double) val.getEle(3,1)).doubleValue();
				l = ((Double) val.getEle(3,2)).doubleValue();
		}
		public double[][] toArrayMat() { return new double[][]{{a,b,c},{d,e,f},{g,h,i},{j,k,l}}; }
	}
	private final class M43Store extends MatStore {
		M43Obj stack[];
		M43Obj heap[];
		M43Obj vars[]= new M43Obj[0];
		final void alloc() { 
			heap = new M43Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M43Obj();
			stack = new M43Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M43Obj();
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M43Obj newvars[] = new M43Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M43Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M43Obj r = stack[--sp];
			M43Obj l = stack[--sp];
			M43Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;

			res.d = l.d+r.d;
			res.e = l.e+r.e;
			res.f = l.f+r.f;

			res.g = l.g+r.g;
			res.h = l.h+r.h;
			res.i = l.i+r.i;

			res.j = l.j+r.j;
			res.k = l.k+r.k;
			res.l = l.l+r.l;
			stack[sp++]=res;	
		}
		final void sub(){
			M43Obj r = stack[--sp];
			M43Obj l = stack[--sp];
			M43Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;

			res.d = l.d-r.d;
			res.e = l.e-r.e;
			res.f = l.f-r.f;

			res.g = l.g-r.g;
			res.h = l.h-r.h;
			res.i = l.i-r.i;

			res.j = l.j-r.j;
			res.k = l.k-r.k;
			res.l = l.l-r.l;
			stack[sp++]=res;	
		}
		final void uminus(){
			M43Obj r = stack[--sp];
			M43Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			res.g = -r.g;
			res.h = -r.h;
			res.i = -r.i;
			res.j = -r.j;
			res.k = -r.k;
			res.l = -r.l;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M43Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M43Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			res.g = l * r.g;
			res.h = l * r.h;
			res.i = l * r.i;
			res.j = l * r.j;
			res.k = l * r.k;
			res.l = l * r.l;
			stack[sp++]=res;
		}
		final void divS()
		{
			M43Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M43Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			res.g = l.g/r;
			res.h = l.h/r;
			res.i = l.i/r;
			res.j = l.j/r;
			res.k = l.k/r;
			res.l = l.l/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M43Obj res = heap[hp++];
			res.l = scalerStore.stack[--scalerStore.sp]; 
			res.k = scalerStore.stack[--scalerStore.sp]; 
			res.j = scalerStore.stack[--scalerStore.sp]; 
			res.i = scalerStore.stack[--scalerStore.sp]; 
			res.h = scalerStore.stack[--scalerStore.sp]; 
			res.g = scalerStore.stack[--scalerStore.sp]; 
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M43Obj r = stack[sp-1]; 
			M43Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
			res.g = r.g;
			res.h = r.h;
			res.i = r.i;
			res.j = r.j;
			res.k = r.k;
			res.l = r.l;
		} 
		final void eq(){
			M43Obj r = stack[--sp];
			M43Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f && l.g == r.g && l.h == r.h 
			  && l.i == r.i && l.j == r.j && l.k == r.k && l.l == r.l ) 
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M43Store m43Store = new M43Store();

	private static final class M44Obj extends MatObj {
		double a,b,c,d, e,f,g,h, i,j,k,l, m,n,o,p;

		private static Dimensions dims = Dimensions.valueOf(4,4);
		public Dimensions getDims() { return dims;	}
		public void copyToMat(Matrix val){
			val.setEle(0,0,new Double(a));
			val.setEle(0,1,new Double(b));
			val.setEle(0,2,new Double(c));
			val.setEle(0,3,new Double(d));
			val.setEle(1,0,new Double(e));
			val.setEle(1,1,new Double(f));
			val.setEle(1,2,new Double(g));
			val.setEle(1,3,new Double(h));
			val.setEle(2,0,new Double(i));
			val.setEle(2,1,new Double(j));
			val.setEle(2,2,new Double(k));
			val.setEle(2,3,new Double(l));
			val.setEle(3,0,new Double(m));
			val.setEle(3,1,new Double(n));
			val.setEle(3,2,new Double(o));
			val.setEle(3,3,new Double(p));
		}
		public String toString() { 
			return "[["+a+","+b+","+c+","+d+"],"+ 
					"["+e+","+f+","+g+","+h+"],"+ 
					"["+i+","+j+","+k+","+l+"],"+ 
					"["+m+","+n+","+o+","+p+"]]"; 
		}
		final void fromMat(Matrix val){
				a = ((Double) val.getEle(0,0)).doubleValue();
				b = ((Double) val.getEle(0,1)).doubleValue();
				c = ((Double) val.getEle(0,2)).doubleValue();
				d = ((Double) val.getEle(0,3)).doubleValue();

				e = ((Double) val.getEle(1,0)).doubleValue();
				f = ((Double) val.getEle(1,1)).doubleValue();
				g = ((Double) val.getEle(1,2)).doubleValue();
				h = ((Double) val.getEle(1,3)).doubleValue();

				i = ((Double) val.getEle(2,0)).doubleValue();
				j = ((Double) val.getEle(2,1)).doubleValue();
				k = ((Double) val.getEle(2,2)).doubleValue();
				l = ((Double) val.getEle(2,3)).doubleValue();

				m = ((Double) val.getEle(3,0)).doubleValue();
				n = ((Double) val.getEle(3,1)).doubleValue();
				o = ((Double) val.getEle(3,2)).doubleValue();
				p = ((Double) val.getEle(3,3)).doubleValue();
		}
		public double[][] toArrayMat() { return new double[][]{{a,b,c,d},{e,f,g,h},{i,j,k,l},{m,n,o,p}}; }
	}
	private final class M44Store extends MatStore {
		M44Obj stack[];
		M44Obj heap[];
		M44Obj vars[]= new M44Obj[0];
		final void alloc() { 
			heap = new M44Obj[hp]; 
			for(int i=0;i<hp;++i) heap[i]=new M44Obj();
			stack = new M44Obj[stackMax];
			for(int i=0;i<stackMax;++i) stack[i]=new M44Obj();
			M44Obj newvars[] = new M44Obj[varRefs.size()];
			System.arraycopy(vars,0,newvars,0,vars.length);
			for(int i=vars.length;i<varRefs.size();++i) newvars[i]=new M44Obj();
			vars = newvars;
			}
		final void expandVarArray(MatrixVariableI var)
		{ 
			M44Obj newvars[] = new M44Obj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			newvars[vars.length]=new M44Obj();
			vars = newvars;
		}
		final void copyVar(int i,Matrix val){vars[i].fromMat(val);}
		final void add(){
			M44Obj r = stack[--sp];
			M44Obj l = stack[--sp];
			M44Obj res = heap[hp++];
			res.a = l.a+r.a;
			res.b = l.b+r.b;
			res.c = l.c+r.c;
			res.d = l.d+r.d;

			res.e = l.e+r.e;
			res.f = l.f+r.f;
			res.g = l.g+r.g;
			res.h = l.h+r.h;

			res.i = l.i+r.i;
			res.j = l.j+r.j;
			res.k = l.k+r.k;
			res.l = l.l+r.l;

			res.m = l.m+r.m;
			res.n = l.n+r.n;
			res.o = l.o+r.o;
			res.p = l.p+r.p;
			stack[sp++]=res;	
		}
		final void sub(){
			M44Obj r = stack[--sp];
			M44Obj l = stack[--sp];
			M44Obj res = heap[hp++];
			res.a = l.a-r.a;
			res.b = l.b-r.b;
			res.c = l.c-r.c;
			res.d = l.d-r.d;

			res.e = l.e-r.e;
			res.f = l.f-r.f;
			res.g = l.g-r.g;
			res.h = l.h-r.h;

			res.i = l.i-r.i;
			res.j = l.j-r.j;
			res.k = l.k-r.k;
			res.l = l.l-r.l;

			res.m = l.m-r.m;
			res.n = l.n-r.n;
			res.o = l.o-r.o;
			res.p = l.p-r.p;
			stack[sp++]=res;	
		}
		final void uminus(){
			M44Obj r = stack[--sp];
			M44Obj res = heap[hp++];
			res.a = -r.a;
			res.b = -r.b;
			res.c = -r.c;
			res.d = -r.d;
			res.e = -r.e;
			res.f = -r.f;
			res.g = -r.g;
			res.h = -r.h;
			res.i = -r.i;
			res.j = -r.j;
			res.k = -r.k;
			res.l = -r.l;
			res.m = -r.m;
			res.n = -r.n;
			res.o = -r.o;
			res.p = -r.p;
			stack[sp++]=res;	
		}
		final void mulS()
		{
			M44Obj r = stack[--sp]; 
			double l = scalerStore.stack[--scalerStore.sp];
			M44Obj res = heap[hp++]; 
			res.a = l * r.a;
			res.b = l * r.b;
			res.c = l * r.c;
			res.d = l * r.d;
			res.e = l * r.e;
			res.f = l * r.f;
			res.g = l * r.g;
			res.h = l * r.h;
			res.i = l * r.i;
			res.j = l * r.j;
			res.k = l * r.k;
			res.l = l * r.l;
			res.m = l * r.m;
			res.n = l * r.n;
			res.o = l * r.o;
			res.p = l * r.p;
			stack[sp++]=res;
		} 
		final void divS()
		{
			M44Obj l = stack[--sp]; 
			double r = scalerStore.stack[--scalerStore.sp];
			M44Obj res = heap[hp++]; 
			res.a = l.a/r;
			res.b = l.b/r;
			res.c = l.c/r;
			res.d = l.d/r;
			res.e = l.e/r;
			res.f = l.f/r;
			res.g = l.g/r;
			res.h = l.h/r;
			res.i = l.i/r;
			res.j = l.j/r;
			res.k = l.k/r;
			res.l = l.l/r;
			res.m = l.m/r;
			res.n = l.n/r;
			res.o = l.o/r;
			res.p = l.p/r;
			stack[sp++]=res;
		} 
		final void makeList() {
			M44Obj res = heap[hp++];
			res.p = scalerStore.stack[--scalerStore.sp]; 
			res.o = scalerStore.stack[--scalerStore.sp]; 
			res.n = scalerStore.stack[--scalerStore.sp]; 
			res.m = scalerStore.stack[--scalerStore.sp]; 
			res.l = scalerStore.stack[--scalerStore.sp]; 
			res.k = scalerStore.stack[--scalerStore.sp]; 
			res.j = scalerStore.stack[--scalerStore.sp]; 
			res.i = scalerStore.stack[--scalerStore.sp]; 
			res.h = scalerStore.stack[--scalerStore.sp]; 
			res.g = scalerStore.stack[--scalerStore.sp]; 
			res.f = scalerStore.stack[--scalerStore.sp]; 
			res.e = scalerStore.stack[--scalerStore.sp]; 
			res.d = scalerStore.stack[--scalerStore.sp]; 
			res.c = scalerStore.stack[--scalerStore.sp]; 
			res.b = scalerStore.stack[--scalerStore.sp]; 
			res.a = scalerStore.stack[--scalerStore.sp];
			stack[sp++]=res;
		} 
		final void assign(int i) {
			M44Obj r = stack[sp-1]; 
			M44Obj res = vars[i]; 
			res.a = r.a;
			res.b = r.b;
			res.c = r.c;
			res.d = r.d;
			res.e = r.e;
			res.f = r.f;
			res.g = r.g;
			res.h = r.h;
			res.i = r.i;
			res.j = r.j;
			res.k = r.k;
			res.l = r.l;
			res.m = r.m;
			res.n = r.n;
			res.o = r.o;
			res.p = r.p;
		} 
		final void eq(){
			M44Obj r = stack[--sp];
			M44Obj l = stack[--sp];
			if(l.a == r.a && l.b == r.b && l.c == r.c && l.d == r.d 
			  && l.e == r.e && l.f == r.f && l.g == r.g && l.h == r.h 
			  && l.i == r.i && l.j == r.j && l.k == r.k && l.l == r.l 
			  && l.m == r.m && l.n == r.n && l.o == r.o && l.p == r.p )
				scalerStore.stack[scalerStore.sp++] = 1.0;
			else
				scalerStore.stack[scalerStore.sp++] = 0.0;
		}
	}
	private M44Store m44Store = new M44Store();

	final static Dimensions dimTypeToDimension(int dt) {
		switch(dt)
		{
			case SCALER:  return Dimensions.ONE;
			case V2:  return Dimensions.TWO;
			case V3:  return Dimensions.THREE;
			case V4:  return Dimensions.valueOf(4);
			case M22: return Dimensions.valueOf(2,2);
			case M23: return Dimensions.valueOf(2,3);
			case M24: return Dimensions.valueOf(2,4);
			case M32: return Dimensions.valueOf(3,2);
			case M33: return Dimensions.valueOf(3,3);
			case M34: return Dimensions.valueOf(3,4);
			case M42: return Dimensions.valueOf(4,2);
			case M43: return Dimensions.valueOf(4,3);
			case M44: return Dimensions.valueOf(4,4);
		}
		return null;
	}
	/** Gets the type of dimension **/
	private static final short getDimType(Dimensions dims)
	{
		if(dims.is0D())
			return SCALER;
		else if(dims.is1D())
		{
			switch(dims.getFirstDim())
			{
			case 2: return V2;
			case 3: return V3;
			case 4: return V4;
			default: return Vn;
			}
		}
		else if(dims.is2D())
		{
			switch(dims.getFirstDim())
			{
			case 2:
				switch(dims.getLastDim())
				{
					case 2:	return M22;
					case 3:	return M23;
					case 4:	return M24;
				}
				break;
			case 3:
				switch(dims.getLastDim())
				{
					case 2:	return M32;
					case 3:	return M33;
					case 4:	return M34;
				}
				break;
			case 4:
				switch(dims.getLastDim())
				{
					case 2:	return M42;
					case 3:	return M43;
					case 4:	return M44;
				}
				break;
			}
			return Mnn;
		}
		return Dtens;
	}
	
	private final ObjStore getStoreByDim(Dimensions dims) throws ParseException
	{
		switch(getDimType(dims))
		{
			case SCALER:  return scalerStore;
			case V2:  return v2Store;
			case V3:  return v3Store;
			case V4:  return v4Store;
			case Vn:  return vnStore;
			case M22: return m22Store;
			case M23: return m23Store;
			case M24: return m24Store;
			case M32: return m32Store;
			case M33: return m33Store;
			case M34: return m34Store;
			case M42: return m42Store;
			case M43: return m43Store;
			case M44: return m44Store;
			case Mnn: return mnnStore;
			default:
			throw new ParseException("Sorry, can only handle scaler, 2, 3 and 4 dimensional vectors and matrices");
		}
	}

	private static final class MnnObj extends MatObj {
		double data[][];
		int rows,cols;
		MnnObj(int row,int col) { data = new double[row][col]; rows=row; cols=col;}
		MnnObj(double mat[][]) { data = mat; rows=mat.length; cols=mat[0].length;}
			
		public Dimensions getDims() { return Dimensions.valueOf(rows,cols);}
		public String toString() { 
			StringBuffer sb = new StringBuffer("[");
			for(int i=0;i<rows;++i){
				if(i>0) sb.append(',');
				sb.append('[');
				for(int j=0;j<cols;++j){
					if(j>0) sb.append(',');
					sb.append(data[i][j]);
				}
				sb.append(']');
			}
			sb.append(']');
			return sb.toString();
		}
		public void fromMat(Matrix val){
			for(int i=0;i<rows;++i)
				for(int j=0;j<cols;++j)
					data[i][j] = ((Double) val.getEle(i,j)).doubleValue();
		}
		public void copyToMat(Matrix val){
			for(int i=0;i<rows;++i)
				for(int j=0;j<cols;++j)
					val.setEle(i,j,new Double(data[i][j]));
		}
		double[][] toArrayMat() { return data; }
		public Object toArray() { 
			double res[][] = new double[rows][cols];
			for(int i=0;i<rows;++i)
				System.arraycopy(data[i],0,res[i],0,cols);
			return res;
		}
	}
	private final class MnnStore extends MatStore {
		MnnObj stack[];
		MnnObj vars[]= new MnnObj[0];
		final void alloc() { 
			stack = new MnnObj[stackMax];
		}
		final void expandVarArray(MatrixVariableI var)
		{ 
			MnnObj newvars[] = new MnnObj[vars.length+1];
			System.arraycopy(vars,0,newvars,0,vars.length);
			Dimensions dims = var.getDimensions();
			newvars[vars.length]=new MnnObj(dims.getFirstDim(),dims.getLastDim());
			vars = newvars;
		}
		final void copyVar(int i,Matrix mat) { vars[i].fromMat(mat); }
		final void add(){
			MnnObj r = stack[--sp];
			MnnObj l = stack[--sp];
			MnnObj res = new MnnObj(l.rows,l.cols);
			for(int i=0;i<l.rows;++i)
				for(int j=0;j<l.cols;++j)
					res.data[i][j]=l.data[i][j]+r.data[i][j];
			stack[sp++]=res;	
		}
		final void sub(){
			MnnObj r = stack[--sp];
			MnnObj l = stack[--sp];
			MnnObj res = new MnnObj(l.rows,l.cols);
			for(int i=0;i<l.rows;++i)
				for(int j=0;j<l.cols;++j)
					res.data[i][j]=l.data[i][j]-r.data[i][j];
			stack[sp++]=res;	
		}
		final void uminus(){
			MnnObj r = stack[--sp];
			MnnObj res = new MnnObj(r.rows,r.cols);
			for(int i=0;i<r.rows;++i)
				for(int j=0;j<r.cols;++j)
					res.data[i][j]=-r.data[i][j];
			stack[sp++]=res;	
		}
		final void mulS()
		{
			MnnObj r = stack[--sp];
			double l = scalerStore.stack[--scalerStore.sp];
			MnnObj res = new MnnObj(r.rows,r.cols);
			for(int i=0;i<r.rows;++i)
				for(int j=0;j<r.cols;++j)
					res.data[i][j]=l* r.data[i][j];
			stack[sp++]=res;
		} 
		final void divS()
		{
			MnnObj l = stack[--sp];
			double r = scalerStore.stack[--scalerStore.sp];
			MnnObj res = new MnnObj(l.rows,l.cols);
			for(int i=0;i<l.rows;++i)
				for(int j=0;j<l.cols;++j)
					res.data[i][j]=l.data[i][j]/r;
			stack[sp++]=res;
		} 
		final void makeList(int rows,int cols) {
			MnnObj res = new MnnObj(rows,cols);
			for(int i=rows-1;i>=0;--i)
				for(int j=cols-1;j>=0;--j)
					res.data[i][j]= scalerStore.stack[--scalerStore.sp]; 
			stack[sp++]=res;
		} 
		final void makeList() {
			throw new UnsupportedOperationException("VnObj: makeList cannot be called with no arguments");
		} 
		final void assign(int k) {
			MnnObj r = stack[sp-1];
			MnnObj res = vars[k];
			for(int i=0;i<r.rows;++i)
				for(int j=0;j<r.cols;++j)
					res.data[i][j]=r.data[i][j];
		} 
		final void eq(){
			MnnObj r = stack[--sp];
			MnnObj l = stack[--sp];
			for(int i=0;i<r.rows;++i)
				for(int j=0;j<r.cols;++j)
					if(l.data[i][j]!=r.data[i][j]){
						scalerStore.stack[scalerStore.sp++] = 0.0;
						return;
				}
			scalerStore.stack[scalerStore.sp++] = 1.0;
		}
	}
	private MnnStore mnnStore = new MnnStore();
	
	private final void incByDim(Dimensions dims) throws ParseException {
		getStoreByDim(dims).incStack();
	}
	private final void decByDim(Dimensions dims) throws ParseException {
		getStoreByDim(dims).decStack();
	}
	private final void incheapByDim(Dimensions dims) throws ParseException	{
		getStoreByDim(dims).incHeap();
	}
		

	

	/** Temporary holder for command list used during compilation */
	private MRpCommandList curCommandList;
	
	
	public final Object visit(ASTStart node, Object data) throws ParseException {
		throw new ParseException("RpeEval: Start node encountered");
	}
	public final Object visit(SimpleNode node, Object data) throws ParseException {
		throw new ParseException("RpeEval: Simple node encountered");
	}

	final void addConstant(Object obj) throws ParseException {
		double val;
		if(obj instanceof Number)
			val = ((Number) obj).doubleValue();
		else
			throw new ParseException("RpeEval: only constants of double type allowed");
		
		scalerStore.incStack();
		for(short i=0;i<constVals.length;++i)
		{
			if(val == constVals[i])
			{
				curCommandList.addCommand(CONST,i);
				return;
			}
		}
		// create a new const
		double newConst[] = new double[constVals.length+1];
		System.arraycopy(constVals,0,newConst,0,constVals.length);
		newConst[constVals.length] = val;
		curCommandList.addCommand(CONST,(short) constVals.length);
		constVals = newConst;
	}

	public final Object visit(ASTConstant node, Object data) throws ParseException {
		addConstant(node.getValue());
		return null;
	}

	public final Object visit(ASTVarNode node, Object data) throws ParseException {
		MatrixVariableI var = (MatrixVariableI) node.getVar();
		if(var.isConstant()) {
			addConstant(var.getMValue());
			return null;
		}
		Dimensions dims = var.getDimensions();
		// find appropriate table
		ObjStore store = getStoreByDim(dims);
		short vRef = (short) store.addVar(var);
		store.incStack();
		curCommandList.addCommand(VAR,getDimType(dims),vRef);
		return null;
	}

	public final Object visit(ASTFunNode node, Object data) throws ParseException 
	{
		ASTMFunNode mnode = (ASTMFunNode) node;
		MatrixNodeI leftnode=null;
		MatrixNodeI rightnode=null;
		int nChild = mnode.jjtGetNumChildren();
		Dimensions dims = mnode.getDim();
		Dimensions ldims = null,rdims=null;

		if(mnode.getPFMC() instanceof MatrixSpecialEvaluationI )
		{				
		}
		else if(mnode.getPFMC() instanceof SpecialEvaluationI )
		{
		}
		else
			node.childrenAccept(this,null);

		if(nChild>=1)
		{
			leftnode = (MatrixNodeI) node.jjtGetChild(0);
			ldims = leftnode.getDim();
		}
		if(nChild>=2)
		{
			rightnode = (MatrixNodeI) node.jjtGetChild(1);
			rdims = rightnode.getDim();
		}

		if(mnode.isOperator())
		{
			XOperator op = (XOperator) mnode.getOperator();
			if(op.isBinary())
				if(nChild!=2)
					throw new ParseException("RpeEval: binary operator must have two children, but it has "+nChild);
			if(op.isUnary())
				if(nChild!=1)
					throw new ParseException("RpeEval: unary operator must have one child, but it has "+nChild);

			if(op == opSet.getAdd())
			{
				if(!dims.equals(ldims) || !dims.equals(rdims))
					throw new ParseException("RpeEval: dims for add must be equal");
				curCommandList.addCommand(ADD,getDimType(dims));
				decByDim(dims);
				incheapByDim(dims);
				return null;
			}
			else if(op == opSet.getSubtract())
			{
				if(!dims.equals(ldims) || !dims.equals(rdims))
					throw new ParseException("RpeEval: dims for add must be equal");
				curCommandList.addCommand(SUB,getDimType(dims));
				decByDim(dims);
				incheapByDim(dims);
				return null;
			}
			else if(op == opSet.getUMinus())
			{
				curCommandList.addCommand(UMINUS,getDimType(dims));
				incheapByDim(dims);
				return null;
			}
			else if(op == opSet.getMultiply())
			{
				decByDim(rdims);
				decByDim(ldims);
				incByDim(dims);
				incheapByDim(dims);
				curCommandList.addCommand(MUL,getDimType(ldims),getDimType(rdims));
				return null;
			}
			else if(op == opSet.getMList())
			{
				incByDim(dims);
				incheapByDim(dims);
				for(int j=0;j<dims.numEles();++j) scalerStore.decStack();
				int d = getDimType(dims);
				if(d == Vn) 
					curCommandList.addCommand(VLIST,(short) dims.getFirstDim());
				else if(d == Mnn) 
					curCommandList.addCommand(MLIST,(short) dims.getFirstDim(),(short) dims.getLastDim());
				else 
					curCommandList.addCommand(LIST,getDimType(dims)); 
				return null;
			}
			else if(op == opSet.getDot())
			{
				scalerStore.incStack();
				decByDim(rdims);
				decByDim(ldims);
				curCommandList.addCommand(DOT,getDimType(ldims)); return null;
			}
			else if(op == opSet.getCross())
			{
				if(ldims.equals(Dimensions.THREE) && rdims.equals(Dimensions.THREE))
				{
					v3Store.decStack();
					v3Store.incHeap();
					curCommandList.addCommand(CROSS,V3); return null;
				}
				else if(ldims.equals(Dimensions.TWO) && rdims.equals(Dimensions.TWO))
				{
					scalerStore.incStack();
					decByDim(ldims);
					decByDim(rdims);
					curCommandList.addCommand(CROSS,V2); return null;
				}
				else throw new ParseException("Bad dimensions for cross product "+ldims+" "+rdims);
			}
			else if(op == opSet.getAssign())
			{
				rightnode.jjtAccept(this,null);
				MatrixVariableI var = (MatrixVariableI) ((ASTMVarNode)node.jjtGetChild(0)).getVar();
				ObjStore store = getStoreByDim(dims);
				short vRef = (short) store.addVar(var);
				store.decStack();
				curCommandList.addCommand(ASSIGN,getDimType(dims),vRef);
				return null;
			}
			else if(op == opSet.getEQ())
			{
				if(!ldims.equals(rdims))throw new ParseException("Dimensions of operands for equals operator must be the same");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(EQ,getDimType(ldims)); return null;
			}
			else if(op == opSet.getNE())
			{
				if(!ldims.equals(rdims))throw new ParseException("Dimensions of operands for not-equals operator must be the same");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(NE,getDimType(ldims)); return null;
			}
			else if(op == opSet.getLT())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for < operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(LT,SCALER); return null;
			}
			else if(op == opSet.getGT())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for > operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(GT,SCALER); return null;
			}
			else if(op == opSet.getLE())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for <= operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(LE,SCALER); return null;
			}
			else if(op == opSet.getGE())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for >= operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(GE,SCALER); return null;
			}
			else if(op == opSet.getAnd())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for && operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(AND,SCALER); return null;
			}
			else if(op == opSet.getOr())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for || operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(OR,SCALER); return null;
			}
			else if(op == opSet.getNot())
			{
				if(!ldims.is0D())throw new ParseException("Dimension of operand for not operator must be one");
				scalerStore.incStack();
				decByDim(rdims);
				curCommandList.addCommand(NOT,SCALER); return null;
			}
			else if(op == opSet.getDivide())
			{
				if(!rdims.is0D())throw new ParseException("RHS operands of / operator must be a Scaler");
				decByDim(rdims);
				decByDim(ldims);
				incByDim(dims);
				incheapByDim(dims);
				curCommandList.addCommand(DIV,getDimType(ldims),getDimType(rdims));
				return null;
			}
			else if(op == opSet.getMod())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for || operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(MOD,SCALER); return null;
			}
			else if(op == opSet.getPower())
			{
				if(!ldims.is0D() || !rdims.is0D())throw new ParseException("Dimensions of operands for || operator must both be one");
				scalerStore.incStack();
				decByDim(ldims);
				decByDim(rdims);
				curCommandList.addCommand(POW,SCALER); return null;
			}
			throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ mnode.getName());
		}
		// other functions
		Short val = (Short) functionHash.get(mnode.getName());
		if(val == null)
			throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ mnode.getName());
		if(mnode.getPFMC().getNumberOfParameters() == 1 && nChild == 1)
		{
			scalerStore.incStack();
			decByDim(ldims);
			curCommandList.addCommand(FUN,val.shortValue()); 
			return null;
		}
		
		throw new ParseException("RpeEval: Sorry unsupported operator/function: "+ mnode.getName());
	}

	/***************************** evaluation *****************************/
	/** Evaluate the expression.
	 * 
	 * @return the value after evaluation
	 */
	public final MRpRes evaluate(MRpCommandList comList)
	{
		scalerStore.reset();
		v2Store.reset();
		v3Store.reset();
		v4Store.reset();
		vnStore.reset();
		m22Store.reset();
		m23Store.reset();
		m24Store.reset();
		m32Store.reset();
		m33Store.reset();
		m34Store.reset();
		m42Store.reset();
		m43Store.reset();
		m44Store.reset();
		mnnStore.reset();
	
		// Now actually process the commands
		int num = comList.getNumCommands();
		for(short commandNum=0;commandNum<num;++commandNum)
		{
			MRpCommandList.MRpCommand command = comList.commands[commandNum];
			short aux1 = command.aux1;
			short aux2 = command.aux2;
			switch(command.command)
			{
			case CONST:
				scalerStore.stack[scalerStore.sp++]=constVals[aux1]; break;
			case VAR:
				switch(aux1) {
				case SCALER:
					scalerStore.stack[scalerStore.sp++]=scalerStore.vars[aux2]; break;
				case V2:
					v2Store.stack[v2Store.sp++]=v2Store.vars[aux2]; break;
				case V3:
					v3Store.stack[v3Store.sp++]=v3Store.vars[aux2]; break;
				case V4:
					v4Store.stack[v4Store.sp++]=v4Store.vars[aux2]; break;
				case Vn:
					vnStore.stack[vnStore.sp++]=vnStore.vars[aux2]; break;

				case M22:
					m22Store.stack[m22Store.sp++]=m22Store.vars[aux2]; break;
				case M23:
					m23Store.stack[m23Store.sp++]=m23Store.vars[aux2]; break;
				case M24:
					m24Store.stack[m24Store.sp++]=m24Store.vars[aux2]; break;
	
				case M32:
					m32Store.stack[m32Store.sp++]=m32Store.vars[aux2]; break;
				case M33:
					m33Store.stack[m33Store.sp++]=m33Store.vars[aux2]; break;
				case M34:
					m34Store.stack[m34Store.sp++]=m34Store.vars[aux2]; break;

				case M42:
					m42Store.stack[m42Store.sp++]=m42Store.vars[aux2]; break;
				case M43:
					m43Store.stack[m43Store.sp++]=m43Store.vars[aux2]; break;
				case M44:
					m44Store.stack[m44Store.sp++]=m44Store.vars[aux2]; break;

				case Mnn:
					mnnStore.stack[mnnStore.sp++]=mnnStore.vars[aux2]; break;
				}
				break;
				
			case ADD:
				switch(aux1)
				{
				case SCALER: scalerStore.add(); break;
				case V2: v2Store.add(); break; 
				case V3: v3Store.add(); break; 
				case V4: v4Store.add(); break; 
				case Vn: vnStore.add(); break; 
	
				case M22: m22Store.add(); break; 
				case M23: m23Store.add(); break; 
				case M24: m24Store.add(); break; 
				case M32: m32Store.add(); break; 
				case M33: m33Store.add(); break; 
				case M34: m34Store.add(); break; 
				case M42: m42Store.add(); break; 
				case M43: m43Store.add(); break; 
				case M44: m44Store.add(); break; 
				case Mnn: mnnStore.add(); break; 
				}
				break;
			case SUB:
				switch(aux1)
				{
				case SCALER: scalerStore.sub(); break; 
				case V2: v2Store.sub(); break; 
				case V3: v3Store.sub(); break; 
				case V4: v4Store.sub(); break; 
				case Vn: vnStore.sub(); break; 
	
				case M22: m22Store.sub(); break; 
				case M23: m23Store.sub(); break; 
				case M24: m24Store.sub(); break; 
				case M32: m32Store.sub(); break; 
				case M33: m33Store.sub(); break; 
				case M34: m34Store.sub(); break; 
				case M42: m42Store.sub(); break; 
				case M43: m43Store.sub(); break; 
				case M44: m44Store.sub(); break; 
				case Mnn: mnnStore.sub(); break; 
				}
				break;
				
			case MUL:
				switch(aux1)
				{
				case SCALER:
					switch(aux2)
					{
					case SCALER: scalerStore.mulS(); break;
					case V2: v2Store.mulS(); break;
					case V3: v3Store.mulS(); break;
					case V4: v4Store.mulS(); break;
					case Vn: vnStore.mulS(); break;
		
					case M22: m22Store.mulS(); break;
					case M23: m23Store.mulS(); break;
					case M24: m24Store.mulS(); break;
	
					case M32: m32Store.mulS(); break;
					case M33: m33Store.mulS(); break;
					case M34: m34Store.mulS(); break;
	
					case M42: m42Store.mulS(); break;
					case M43: m43Store.mulS(); break;
					case M44: m44Store.mulS(); break;
					case Mnn: mnnStore.mulS(); break;
	
					}
					break;
					
				case V2:
					switch(aux2)
					{
					case SCALER: v2Store.mulS(); break;
					case V2: v2Store.mulV2(); break;
					case M22: mulV2M22(); break; 
					case M23: mulV2M23(); break; 
					case M24: mulV2M24(); break;
					case Mnn: mulVnMnn(
						v2Store.stack[--v2Store.sp],
						mnnStore.stack[--mnnStore.sp]); break; 
					}
					break;
	
				case V3:
					switch(aux2)
					{
					case SCALER: v3Store.mulS(); break;
					case M32: mulV3M32(); break; 
					case M33: mulV3M33(); break; 
					case M34: mulV3M34(); break; 
					case Mnn: mulVnMnn(
						v3Store.stack[--v3Store.sp],
						mnnStore.stack[--mnnStore.sp]); break; 
					}
					break;
					
				case V4:
					switch(aux2)
					{
					case SCALER: v4Store.mulS(); break;
					case M42: mulV4M42(); break; 
					case M43: mulV4M43(); break; 
					case M44: mulV4M44(); break; 
					case Mnn: mulVnMnn(
						v4Store.stack[--v4Store.sp],
						mnnStore.stack[--mnnStore.sp]); break; 
					}
					break;
				
				case Vn:
					switch(aux2)
					{
					case SCALER: mnnStore.mulS(); break;
					case Mnn: mulVnMnn(
						vnStore.stack[--vnStore.sp],
						mnnStore.stack[--mnnStore.sp]); break; 
					}
					break;
				case M22:
					switch(aux2)
					{
					case SCALER: m22Store.mulS(); break;
					case V2: mulM22V2(); break;
					case M22:mulM22M22(); break; 
					case M23:mulM22M23(); break; 
					case M24:mulM22M24(); break;
					case Mnn: mulMnnMnn(
						m22Store.stack[--m22Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M23:
					switch(aux2)
					{
					case SCALER: m23Store.mulS(); break;
					case V3: mulM23V3(); break;
					case M32:mulM23M32(); break; 
					case M33:mulM23M33(); break; 
					case M34:mulM23M34(); break; 
					case Mnn: mulMnnMnn(
						m23Store.stack[--m23Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M24:
					switch(aux2)
					{
					case SCALER: m24Store.mulS(); break;
					case V4: mulM24V4(); break;
					case M42:mulM24M42(); break; 
					case M43:mulM24M43(); break; 
					case M44:mulM24M44(); break; 
					case Mnn: mulMnnMnn(
						m24Store.stack[--m24Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M32:
					switch(aux2)
					{
					case SCALER: m32Store.mulS(); break;
					case V2: mulM32V2(); break;
					case M22:mulM32M22(); break; 
					case M23:mulM32M23(); break; 
					case M24:mulM32M24(); break; 
					case Mnn: mulMnnMnn(
						m32Store.stack[--m32Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M33:
					switch(aux2)
					{
					case SCALER: m33Store.mulS(); break;
					case V3: mulM33V3(); break;
					case M32:mulM33M32(); break; 
					case M33:mulM33M33(); break; 
					case M34:mulM33M34(); break; 
					case Mnn: mulMnnMnn(
						m33Store.stack[--m33Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M34:
					switch(aux2)
					{
					case SCALER: m34Store.mulS(); break;
					case V4: mulM34V4(); break;
					case M42:mulM34M42(); break; 
					case M43:mulM34M43(); break; 
					case M44:mulM34M44(); break; 
					case Mnn: mulMnnMnn(
						m34Store.stack[--m34Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M42:
					switch(aux2)
					{
					case SCALER: m42Store.mulS(); break;
					case V2: mulM42V2(); break;
					case M22:mulM42M22(); break; 
					case M23:mulM42M23(); break; 
					case M24:mulM42M24(); break; 
					case Mnn: mulMnnMnn(
						m42Store.stack[--m42Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
	
				case M43:
					switch(aux2)
					{
					case SCALER: m43Store.mulS(); break;
					case V3: mulM43V3(); break;
					case M32:mulM43M32(); break; 
					case M33:mulM43M33(); break; 
					case M34:mulM43M34(); break; 
					case Mnn: mulMnnMnn(
						m43Store.stack[--m43Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;
		
				case M44:
					switch(aux2)
					{
					case SCALER: m44Store.mulS(); break;
					case V4: mulM44V4(); break;
					case M42:mulM44M42(); break; 
					case M43:mulM44M43(); break; 
					case M44:mulM44M44(); break; 
					case Mnn: mulMnnMnn(
						m44Store.stack[--m44Store.sp],
						mnnStore.stack[--mnnStore.sp]); break;
					}
					break;

				case Mnn:
					switch(aux2)
					{
					case SCALER: mnnStore.mulS(); break;
					case V2:
						mulMnnVn(
							mnnStore.stack[--mnnStore.sp], 
							v2Store.stack[--v2Store.sp]); break;
					case V3:
						mulMnnVn(
							mnnStore.stack[--mnnStore.sp], 
							v3Store.stack[--v3Store.sp]); break;
					case V4:
						mulMnnVn(
							mnnStore.stack[--mnnStore.sp], 
							v4Store.stack[--v4Store.sp]); break;
					case Vn:
						mulMnnVn(
							mnnStore.stack[--mnnStore.sp], 
							vnStore.stack[--vnStore.sp]); break;
							
					case M22: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m22Store.stack[--m22Store.sp]); break;
					case M23: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m23Store.stack[--m23Store.sp]); break;
					case M24: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m24Store.stack[--m24Store.sp]); break;
					case M32: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m32Store.stack[--m32Store.sp]); break;
					case M33: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m33Store.stack[--m33Store.sp]); break;
					case M34: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m34Store.stack[--m34Store.sp]); break;
					case M42: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m42Store.stack[--m42Store.sp]); break;
					case M43: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m43Store.stack[--m43Store.sp]); break;
					case M44: mulMnnMnn(
						mnnStore.stack[--mnnStore.sp],
						m44Store.stack[--m44Store.sp]); break;
					case Mnn:
						MnnObj r = mnnStore.stack[--mnnStore.sp];
						MnnObj l = mnnStore.stack[--mnnStore.sp];
						mulMnnMnn(l,r); break;
					}
					break;
				}
				break;
/* TODO	DIV MOD POW
			case DIV = 29;
			case MOD = 30;
			case POW = 31;
*/
			case DIV: 
				switch(aux1)
				{
				case SCALER: scalerStore.divS(); break;
				case V2: v2Store.divS(); break;
				case V3: v3Store.divS(); break;
				case V4: v4Store.divS(); break;
				case Vn: vnStore.divS(); break;
	
				case M22: m22Store.divS(); break;
				case M23: m23Store.divS(); break;
				case M24: m24Store.divS(); break;

				case M32: m32Store.divS(); break;
				case M33: m33Store.divS(); break;
				case M34: m34Store.divS(); break;

				case M42: m42Store.divS(); break;
				case M43: m43Store.divS(); break;
				case M44: m44Store.divS(); break;
				case Mnn: mnnStore.divS(); break;

				}
				break;
			case MOD: scalerStore.mod(); break;
			case POW: scalerStore.pow(); break;

			case AND: scalerStore.and(); break;
			case OR: scalerStore.or(); break;
			case NOT: scalerStore.not(); break;

			case LT: scalerStore.lt(); break;
			case LE: scalerStore.le(); break;
			case GT: scalerStore.gt(); break;
			case GE: scalerStore.ge(); break;
			case NE:
				switch(aux1)
				{
				case SCALER: scalerStore.neq(); break;
				case V2: v2Store.eq(); scalerStore.not(); break; 
				case V3: v3Store.eq(); scalerStore.not(); break; 
				case V4: v4Store.eq(); scalerStore.not(); break; 
				case M22: m22Store.eq(); scalerStore.not(); break; 
				case M23: m23Store.eq(); scalerStore.not(); break; 
				case M24: m24Store.eq(); scalerStore.not(); break; 
				case M32: m32Store.eq(); scalerStore.not(); break; 
				case M33: m33Store.eq(); scalerStore.not(); break; 
				case M34: m34Store.eq(); scalerStore.not(); break; 
				case M42: m42Store.eq(); scalerStore.not(); break; 
				case M43: m43Store.eq(); scalerStore.not(); break; 
				case M44: m44Store.eq(); scalerStore.not(); break; 
				}
				break;
			case EQ:
				switch(aux1)
				{
				case SCALER: scalerStore.eq(); break;
				case V2: v2Store.eq(); break; 
				case V3: v3Store.eq(); break; 
				case V4: v4Store.eq(); break; 
				case M22: m22Store.eq(); break; 
				case M23: m23Store.eq(); break; 
				case M24: m24Store.eq(); break; 
				case M32: m32Store.eq(); break; 
				case M33: m33Store.eq(); break; 
				case M34: m34Store.eq(); break; 
				case M42: m42Store.eq(); break; 
				case M43: m43Store.eq(); break; 
				case M44: m44Store.eq(); break; 
				}
				break;
	
			case ASSIGN:
				switch(aux1)
				{
				case SCALER: scalerStore.assign(aux2); break;
				case V2: v2Store.assign(aux2); break; 
				case V3: v3Store.assign(aux2); break; 
				case V4: v4Store.assign(aux2); break; 
				case Vn: vnStore.assign(aux2); break; 
				case M22: m22Store.assign(aux2); break; 
				case M23: m23Store.assign(aux2); break; 
				case M24: m24Store.assign(aux2); break; 
				case M32: m32Store.assign(aux2); break; 
				case M33: m33Store.assign(aux2); break; 
				case M34: m34Store.assign(aux2); break; 
				case M42: m42Store.assign(aux2); break; 
				case M43: m43Store.assign(aux2); break; 
				case M44: m44Store.assign(aux2); break; 
				case Mnn: mnnStore.assign(aux2); break; 
				}
				break;

			case LIST:
			{
				switch(aux1)
				{
				case SCALER:{
					}break;
				case V2: v2Store.makeList(); break; 
				case V3: v3Store.makeList(); break; 
				case V4: v4Store.makeList(); break; 
	
				case M22: m22Store.makeList(); break; 
				case M23: m23Store.makeList(); break; 
				case M24: m24Store.makeList(); break; 
				case M32: m32Store.makeList(); break; 
				case M33: m33Store.makeList(); break; 
				case M34: m34Store.makeList(); break; 
				case M42: m42Store.makeList(); break; 
				case M43: m43Store.makeList(); break; 
				case M44: m44Store.makeList(); break; 
	
				}
				break;
				
			}
			case VLIST:
				vnStore.makeList(aux1); break;
			case MLIST:
				mnnStore.makeList(aux1,aux2); break;
				
			case DOT:
				switch(aux1)
				{
				case V2: dotV2(); break;
				case V3: dotV3(); break;
				case V4: dotV4(); break;
				case Vn: dotVn(); break;
				}
				break;
			case CROSS:
				switch(aux1)
				{
				case V2: crossV2(); break;
				case V3: crossV3(); break;
				}
				break;
			case FUN:
				unitaryFunction(aux1); break;
			case UMINUS:
				switch(aux1)
				{
				case SCALER: scalerStore.uminus(); break; 
				case V2: v2Store.uminus(); break; 
				case V3: v3Store.uminus(); break; 
				case V4: v4Store.uminus(); break; 
				case Vn: vnStore.uminus(); break; 

				case M22: m22Store.uminus(); break; 
				case M23: m23Store.uminus(); break; 
				case M24: m24Store.uminus(); break; 
				case M32: m32Store.uminus(); break; 
				case M33: m33Store.uminus(); break; 
				case M34: m34Store.uminus(); break; 
				case M42: m42Store.uminus(); break; 
				case M43: m43Store.uminus(); break; 
				case M44: m44Store.uminus(); break; 
				case Mnn: mnnStore.uminus(); break; 
				}
				break;
			}
		}

		switch(comList.getFinalType())
		{
			case SCALER: 
//				return new ScalerObj(scalerStore.stack[--scalerStore.sp]);
//				return new double[]{popScaler()};
				scalerRes.a = scalerStore.stack[--scalerStore.sp];
				return scalerRes;
			case V2:
				return v2Store.stack[--v2Store.sp];
			case V3:
				return v3Store.stack[--v3Store.sp];
			case V4: 
				return v4Store.stack[--v4Store.sp];
			case Vn: 
				return vnStore.stack[--vnStore.sp];
			case M22:
				return m22Store.stack[--m22Store.sp];
			case M23:
				return m23Store.stack[--m23Store.sp];
			case M24:
				return m24Store.stack[--m24Store.sp];
			case M32:
				return m32Store.stack[--m32Store.sp];
			case M33:
				return m33Store.stack[--m33Store.sp];
			case M34:
				return m34Store.stack[--m34Store.sp];
			case M42:
				return m42Store.stack[--m42Store.sp];
			case M43:
				return m43Store.stack[--m43Store.sp];
			case M44:
				return m44Store.stack[--m44Store.sp];
			case Mnn:
				return mnnStore.stack[--mnnStore.sp];
			default:
		}
		return null;
	}

	private final void dotV2(){
		V2Obj r = v2Store.stack[--v2Store.sp]; 
		V2Obj l = v2Store.stack[--v2Store.sp]; 
		scalerStore.stack[scalerStore.sp++]
			= l.a * r.a + l.b * r.b;
	}

	private final void crossV2(){
		V2Obj r = v2Store.stack[--v2Store.sp]; 
		V2Obj l = v2Store.stack[--v2Store.sp]; 
		scalerStore.stack[scalerStore.sp++]
			= l.a * r.b - l.b * r.a;
	}

	private final void dotV3(){
		V3Obj r = v3Store.stack[--v3Store.sp];
		V3Obj l = v3Store.stack[--v3Store.sp];
		scalerStore.stack[scalerStore.sp++]
			= l.a * r.a + l.b * r.b + l.c * r.c;
	}
	
	private final void crossV3(){
		V3Obj r = v3Store.stack[--v3Store.sp];
		V3Obj l = v3Store.stack[--v3Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.b*r.c - l.c*r.b;
		res.b = l.c*r.a - l.a*r.c;
		res.c = l.a*r.b - l.b*r.a;
		v3Store.stack[v3Store.sp++]=res;	
	}

	private final void dotV4(){
		V4Obj r = v4Store.stack[--v4Store.sp]; 
		V4Obj l = v4Store.stack[--v4Store.sp]; 
		scalerStore.stack[scalerStore.sp++]
			= l.a * r.a + l.b * r.b + l.c * r.c + l.d * r.d;
	}
	
	private final void dotVn(){
		VnObj r = vnStore.stack[--vnStore.sp];
		VnObj l = vnStore.stack[--vnStore.sp];
		double res = l.data[0] * r.data[0];
		for(int i=1;i<l.data.length;++i) res += l.data[i]*r.data[i];
		scalerStore.stack[scalerStore.sp++]=res;
	}

	private final void mulM22V2(){
		V2Obj r = v2Store.stack[--v2Store.sp];
		M22Obj l = m22Store.stack[--m22Store.sp];
		V2Obj res = v2Store.heap[v2Store.hp++];
		res.a = l.a*r.a+l.b*r.b;
		res.b = l.c*r.a+l.d*r.b;
		v2Store.stack[v2Store.sp++]=res;	
	}

	private final void mulV2M22(){
		M22Obj r = m22Store.stack[--m22Store.sp];
		V2Obj l = v2Store.stack[--v2Store.sp];
		V2Obj res = v2Store.heap[v2Store.hp++];
		res.a = l.a*r.a+l.b*r.c;
		res.b = l.a*r.b+l.b*r.d;
		v2Store.stack[v2Store.sp++]=res;	
	}

	private final void mulM32V2(){
		V2Obj r = v2Store.stack[--v2Store.sp];
		M32Obj l = m32Store.stack[--m32Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.a*r.a+l.b*r.b;
		res.b = l.c*r.a+l.d*r.b;
		res.c = l.e*r.a+l.f*r.b;
		v3Store.stack[v3Store.sp++]=res;	
	}

	private final void mulV2M23(){
		M23Obj r = m23Store.stack[--m23Store.sp];
		V2Obj l = v2Store.stack[--v2Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.a*r.a+l.b*r.d;
		res.b = l.a*r.b+l.b*r.e;
		res.c = l.a*r.c+l.b*r.f;
		v3Store.stack[v3Store.sp++]=res;	
	}

	private final void mulM42V2(){
		V2Obj r = v2Store.stack[--v2Store.sp];
		M42Obj l = m42Store.stack[--m42Store.sp];
		V4Obj res = v4Store.heap[v4Store.hp++];
		res.a = l.a*r.a + l.b*r.b;
		res.b = l.c*r.a + l.d*r.b;
		res.c = l.e*r.a + l.f*r.b;
		res.d = l.g*r.a + l.h*r.b;
		v4Store.stack[v4Store.sp++]=res;	
	}
		
	private final void mulV2M24(){
		M24Obj r = m24Store.stack[--m24Store.sp];
		V2Obj l = v2Store.stack[--v2Store.sp];
		V4Obj res = v4Store.heap[v4Store.hp++];
		res.a = l.a*r.a+l.b*r.e;
		res.b = l.a*r.b+l.b*r.f;
		res.c = l.a*r.c+l.b*r.g;
		res.d = l.a*r.d+l.b*r.h;
	
		v4Store.stack[v4Store.sp++]=res;	
	}
	
	// V3 
	private final void mulM23V3(){
		V3Obj r = v3Store.stack[--v3Store.sp];
		M23Obj l = m23Store.stack[--m23Store.sp];
		V2Obj res = v2Store.heap[v2Store.hp++];
		res.a = l.a*r.a+l.b*r.b+l.c*r.c;
		res.b = l.d*r.a+l.e*r.b+l.f*r.c;
		v2Store.stack[v2Store.sp++]=res;	
	}
		
	private final void mulV3M32(){
		M32Obj r = m32Store.stack[--m32Store.sp];
		V3Obj l = v3Store.stack[--v3Store.sp];
		V2Obj res = v2Store.heap[v2Store.hp++];
		res.a = l.a*r.a+l.b*r.c+l.c*r.e;
		res.b = l.a*r.b+l.b*r.d+l.c*r.f;
		v2Store.stack[v2Store.sp++]=res;	
	}
	
	private final void mulM33V3(){
		V3Obj r = v3Store.stack[--v3Store.sp];
		M33Obj l = m33Store.stack[--m33Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.a*r.a+l.b*r.b+l.c*r.c;
		res.b = l.d*r.a+l.e*r.b+l.f*r.c;
		res.c = l.g*r.a+l.h*r.b+l.i*r.c;
		v3Store.stack[v3Store.sp++]=res;	
	}
	
	private final void mulV3M33(){
		M33Obj r = m33Store.stack[--m33Store.sp];
		V3Obj l = v3Store.stack[--v3Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.a*r.a+l.b*r.d+l.c*r.g;
		res.b = l.a*r.b+l.b*r.e+l.c*r.h;
		res.c = l.a*r.c+l.b*r.f+l.c*r.i;
		v3Store.stack[v3Store.sp++]=res;	
	}
	
	private final void mulM43V3(){
		V3Obj r = v3Store.stack[--v3Store.sp];
		M43Obj l = m43Store.stack[--m43Store.sp];
		V4Obj res = v4Store.heap[v4Store.hp++];
		res.a = l.a*r.a + l.b*r.b + l.c*r.c;
		res.b = l.d*r.a + l.e*r.b + l.f*r.c;
		res.c = l.g*r.a + l.h*r.b + l.i*r.c;
		res.d = l.j*r.a + l.k*r.b + l.l*r.c;
		v4Store.stack[v4Store.sp++]=res;	
	}
			
	private final void mulV3M34(){
		M34Obj r = m34Store.stack[--m34Store.sp];
		V3Obj l = v3Store.stack[--v3Store.sp];
		V4Obj res = v4Store.heap[v4Store.hp++];
		res.a = l.a*r.a+l.b*r.e+l.c*r.i;
		res.b = l.a*r.b+l.b*r.f+l.c*r.j;
		res.c = l.a*r.c+l.b*r.g+l.c*r.k;
		res.d = l.a*r.d+l.b*r.h+l.c*r.l;
		
		v4Store.stack[v4Store.sp++]=res;	
	}

	private final void mulM24V4(){
		V4Obj r = v4Store.stack[--v4Store.sp];
		M24Obj l = m24Store.stack[--m24Store.sp];
		V2Obj res = v2Store.heap[v2Store.hp++];
		res.a = l.a*r.a + l.b*r.b + l.c*r.c + l.d*r.d;
		res.b = l.e*r.a + l.f*r.b + l.g*r.c + l.h*r.d;
		v2Store.stack[v2Store.sp++]=res;	
	}
		
	private final void mulV4M42(){
		M42Obj r = m42Store.stack[--m42Store.sp];
		V4Obj l = v4Store.stack[--v4Store.sp];
		V2Obj res = v2Store.heap[v2Store.hp++];
		res.a = l.a*r.a + l.b*r.c + l.c*r.e + l.d*r.g;
		res.b = l.a*r.b + l.b*r.d + l.c*r.f + l.d*r.h;
	
		v2Store.stack[v2Store.sp++]=res;	
	}

	private final void mulM34V4(){
		V4Obj r = v4Store.stack[--v4Store.sp];
		M34Obj l = m34Store.stack[--m34Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.a*r.a + l.b*r.b + l.c*r.c + l.d*r.d;
		res.b = l.e*r.a + l.f*r.b + l.g*r.c + l.h*r.d;
		res.c = l.i*r.a + l.j*r.b + l.k*r.c + l.l*r.d;
		v3Store.stack[v3Store.sp++]=res;	
	}
			
	private final void mulV4M43(){
		M43Obj r = m43Store.stack[--m43Store.sp];
		V4Obj l = v4Store.stack[--v4Store.sp];
		V3Obj res = v3Store.heap[v3Store.hp++];
		res.a = l.a*r.a + l.b*r.d + l.c*r.g + l.d*r.j;
		res.b = l.a*r.b + l.b*r.e + l.c*r.h + l.d*r.k;
		res.c = l.a*r.c + l.b*r.f + l.c*r.i + l.d*r.l;
		
		v3Store.stack[v3Store.sp++]=res;	
	}

	private final void mulM44V4(){
		V4Obj r = v4Store.stack[--v4Store.sp];
		M44Obj l = m44Store.stack[--m44Store.sp];
		V4Obj res = v4Store.heap[v4Store.hp++];
		res.a = l.a*r.a + l.b*r.b + l.c*r.c + l.d*r.d;
		res.b = l.e*r.a + l.f*r.b + l.g*r.c + l.h*r.d;
		res.c = l.i*r.a + l.j*r.b + l.k*r.c + l.l*r.d;
		res.d = l.m*r.a + l.n*r.b + l.o*r.c + l.p*r.d;
		v4Store.stack[v4Store.sp++]=res;	
	}
	
	private final void mulV4M44(){
		M44Obj r = m44Store.stack[--m44Store.sp];
		V4Obj l = v4Store.stack[--v4Store.sp];
		V4Obj res = v4Store.heap[v4Store.hp++];
		res.a = l.a*r.a+l.b*r.e+l.c*r.i+l.d*r.m;
		res.b = l.a*r.b+l.b*r.f+l.c*r.j+l.d*r.n;
		res.c = l.a*r.c+l.b*r.g+l.c*r.k+l.d*r.o;
		res.d = l.a*r.d+l.b*r.h+l.c*r.l+l.d*r.p;

		v4Store.stack[v4Store.sp++]=res;	
	}


	// M22 * 
	
	private final void mulM22M22(){
		M22Obj r = m22Store.stack[--m22Store.sp]; 
		M22Obj l = m22Store.stack[--m22Store.sp];
		M22Obj res = m22Store.heap[m22Store.hp++];
		res.a = l.a * r.a + l.b * r.c;
		res.b = l.a * r.b + l.b * r.d;
	
		res.c = l.c * r.a + l.d * r.c;
		res.d = l.c * r.b + l.d * r.d;
	
		m22Store.stack[m22Store.sp++]=res;
	}
	
	private final void mulM22M23(){
		M23Obj r = m23Store.stack[--m23Store.sp]; 
		M22Obj l = m22Store.stack[--m22Store.sp];
		M23Obj res = m23Store.heap[m23Store.hp++];
		res.a = l.a * r.a + l.b * r.d;
		res.b = l.a * r.b + l.b * r.e;
		res.c = l.a * r.c + l.b * r.f;
	
		res.d = l.c * r.a + l.d * r.d;
		res.e = l.c * r.b + l.d * r.e;
		res.f = l.c * r.c + l.d * r.f;
	
		m23Store.stack[m23Store.sp++]=res;
	}
	
	private final void mulM22M24(){
		M24Obj r = m24Store.stack[--m24Store.sp]; 
		M22Obj l = m22Store.stack[--m22Store.sp];
		M24Obj res = m24Store.heap[m24Store.hp++];
		res.a = l.a * r.a + l.b * r.e;
		res.b = l.a * r.b + l.b * r.f;
		res.c = l.a * r.c + l.b * r.g;
		res.d = l.a * r.d + l.b * r.h;
	
		res.e = l.c * r.a + l.d * r.e;
		res.f = l.c * r.b + l.d * r.f;
		res.g = l.c * r.c + l.d * r.g;
		res.h = l.c * r.d + l.d * r.h;
	
		m24Store.stack[m24Store.sp++]=res;
	}
	
	// M23 *
		
	private final void mulM23M32(){
		M32Obj r = m32Store.stack[--m32Store.sp]; 
		M23Obj l = m23Store.stack[--m23Store.sp];
		M22Obj res = m22Store.heap[m22Store.hp++];
		res.a = l.a * r.a + l.b * r.c + l.c * r.e;
		res.b = l.a * r.b + l.b * r.d + l.c * r.f;
	
		res.c = l.d * r.a + l.e * r.c + l.f * r.e;
		res.d = l.d * r.b + l.e * r.d + l.f * r.f;
	
		m22Store.stack[m22Store.sp++]=res;
	}
	
	private final void mulM23M33(){
		M33Obj r = m33Store.stack[--m33Store.sp]; 
		M23Obj l = m23Store.stack[--m23Store.sp];
		M23Obj res = m23Store.heap[m23Store.hp++];
		res.a = l.a * r.a + l.b * r.d + l.c * r.g;
		res.b = l.a * r.b + l.b * r.e + l.c * r.h;
		res.c = l.a * r.c + l.b * r.f + l.c * r.i;
	
		res.d = l.d * r.a + l.e * r.d + l.f * r.g;
		res.e = l.d * r.b + l.e * r.e + l.f * r.h;
		res.f = l.d * r.c + l.e * r.f + l.f * r.i;
	
		m23Store.stack[m23Store.sp++]=res;
	}
	
	private final void mulM23M34(){
		M34Obj r = m34Store.stack[--m34Store.sp]; 
		M23Obj l = m23Store.stack[--m23Store.sp];
		M24Obj res = m24Store.heap[m24Store.hp++];
		res.a = l.a * r.a + l.b * r.e + l.c * r.i;
		res.b = l.a * r.b + l.b * r.f + l.c * r.j;
		res.c = l.a * r.c + l.b * r.g + l.c * r.k;
		res.d = l.a * r.d + l.b * r.h + l.c * r.l;
	
		res.e = l.d * r.a + l.e * r.e + l.f * r.i;
		res.f = l.d * r.b + l.e * r.f + l.f * r.j;
		res.g = l.d * r.c + l.e * r.g + l.f * r.k;
		res.h = l.d * r.d + l.e * r.h + l.f * r.l;
	
		m24Store.stack[m24Store.sp++]=res;
	}
	
	// M24 *
	
	private final void mulM24M42(){
		M42Obj r = m42Store.stack[--m42Store.sp]; 
		M24Obj l = m24Store.stack[--m24Store.sp];
		M22Obj res = m22Store.heap[m22Store.hp++];
		res.a = l.a * r.a + l.b * r.c + l.c * r.e + l.d * r.g;
		res.b = l.a * r.b + l.b * r.d + l.c * r.f + l.d * r.h;
	
		res.c = l.e * r.a + l.f * r.c + l.g * r.e + l.h * r.g;
		res.d = l.e * r.b + l.f * r.d + l.g * r.f + l.h * r.h;
	
		m22Store.stack[m22Store.sp++]=res;
	}
	
	private final void mulM24M43(){
		M43Obj r = m43Store.stack[--m43Store.sp]; 
		M24Obj l = m24Store.stack[--m24Store.sp];
		M23Obj res = m23Store.heap[m23Store.hp++];
		res.a = l.a * r.a + l.b * r.d + l.c * r.g + l.d * r.j;
		res.b = l.a * r.b + l.b * r.e + l.c * r.h + l.d * r.k;
		res.c = l.a * r.c + l.b * r.f + l.c * r.i + l.d * r.l;
	
		res.d = l.e * r.a + l.f * r.d + l.g * r.g + l.h * r.j;
		res.e = l.e * r.b + l.f * r.e + l.g * r.h + l.h * r.k;
		res.f = l.e * r.c + l.f * r.f + l.g * r.i + l.h * r.l;
	
		m23Store.stack[m23Store.sp++]=res;
	}
	
	private final void mulM24M44(){
		M44Obj r = m44Store.stack[--m44Store.sp]; 
		M24Obj l = m24Store.stack[--m24Store.sp];
		M24Obj res = m24Store.heap[m24Store.hp++];
		res.a = l.a * r.a + l.b * r.e + l.c * r.i + l.d * r.m;
		res.b = l.a * r.b + l.b * r.f + l.c * r.j + l.d * r.n;
		res.c = l.a * r.c + l.b * r.g + l.c * r.k + l.d * r.o;
		res.d = l.a * r.d + l.b * r.h + l.c * r.l + l.d * r.p;
	
		res.e = l.e * r.a + l.f * r.e + l.g * r.i + l.h * r.m;
		res.f = l.e * r.b + l.f * r.f + l.g * r.j + l.h * r.n;
		res.g = l.e * r.c + l.f * r.g + l.g * r.k + l.h * r.o;
		res.h = l.e * r.d + l.f * r.h + l.g * r.l + l.h * r.p;
	
		m24Store.stack[m24Store.sp++]=res;
	}

	// M32 * 
	
	private final void mulM32M22(){
		M22Obj r = m22Store.stack[--m22Store.sp]; 
		M32Obj l = m32Store.stack[--m32Store.sp];
		M32Obj res = m32Store.heap[m32Store.hp++];
		res.a = l.a * r.a + l.b * r.c;
		res.b = l.a * r.b + l.b * r.d;

		res.c = l.c * r.a + l.d * r.c;
		res.d = l.c * r.b + l.d * r.d;

		res.e = l.e * r.a + l.f * r.c;
		res.f = l.e * r.b + l.f * r.d;
		m32Store.stack[m32Store.sp++]=res;
	}

	private final void mulM32M23(){
		M23Obj r = m23Store.stack[--m23Store.sp]; 
		M32Obj l = m32Store.stack[--m32Store.sp];
		M33Obj res = m33Store.heap[m33Store.hp++];
		res.a = l.a * r.a + l.b * r.d;
		res.b = l.a * r.b + l.b * r.e;
		res.c = l.a * r.c + l.b * r.f;

		res.d = l.c * r.a + l.d * r.d;
		res.e = l.c * r.b + l.d * r.e;
		res.f = l.c * r.c + l.d * r.f;

		res.g = l.e * r.a + l.f * r.d;
		res.h = l.e * r.b + l.f * r.e;
		res.i = l.e * r.c + l.f * r.f;

		m33Store.stack[m33Store.sp++]=res;
	}

	private final void mulM32M24(){
		M24Obj r = m24Store.stack[--m24Store.sp]; 
		M32Obj l = m32Store.stack[--m32Store.sp];
		M34Obj res = m34Store.heap[m34Store.hp++];
		res.a = l.a * r.a + l.b * r.e;
		res.b = l.a * r.b + l.b * r.f;
		res.c = l.a * r.c + l.b * r.g;
		res.d = l.a * r.d + l.b * r.h;

		res.e = l.c * r.a + l.d * r.e;
		res.f = l.c * r.b + l.d * r.f;
		res.g = l.c * r.c + l.d * r.g;
		res.h = l.c * r.d + l.d * r.h;

		res.i = l.e * r.a + l.f * r.e;
		res.j = l.e * r.b + l.f * r.f;
		res.k = l.e * r.c + l.f * r.g;
		res.l = l.e * r.d + l.f * r.h;

		m34Store.stack[m34Store.sp++]=res;
	}

	// M33 *
	
	private final void mulM33M32(){
		M32Obj r = m32Store.stack[--m32Store.sp]; 
		M33Obj l = m33Store.stack[--m33Store.sp];
		M32Obj res = m32Store.heap[m32Store.hp++];
		res.a = l.a * r.a + l.b * r.c + l.c * r.e;
		res.b = l.a * r.b + l.b * r.d + l.c * r.f;

		res.c = l.d * r.a + l.e * r.c + l.f * r.e;
		res.d = l.d * r.b + l.e * r.d + l.f * r.f;

		res.e = l.g * r.a + l.h * r.c + l.i * r.e;
		res.f = l.g * r.b + l.h * r.d + l.i * r.f;

		m32Store.stack[m32Store.sp++]=res;
	}

	private final void mulM33M33(){
		M33Obj r = m33Store.stack[--m33Store.sp]; 
		M33Obj l = m33Store.stack[--m33Store.sp];
		M33Obj res = m33Store.heap[m33Store.hp++];
		res.a = l.a * r.a + l.b * r.d + l.c * r.g;
		res.b = l.a * r.b + l.b * r.e + l.c * r.h;
		res.c = l.a * r.c + l.b * r.f + l.c * r.i;

		res.d = l.d * r.a + l.e * r.d + l.f * r.g;
		res.e = l.d * r.b + l.e * r.e + l.f * r.h;
		res.f = l.d * r.c + l.e * r.f + l.f * r.i;

		res.g = l.g * r.a + l.h * r.d + l.i * r.g;
		res.h = l.g * r.b + l.h * r.e + l.i * r.h;
		res.i = l.g * r.c + l.h * r.f + l.i * r.i;

		m33Store.stack[m33Store.sp++]=res;
	}

	private final void mulM33M34(){
		M34Obj r = m34Store.stack[--m34Store.sp]; 
		M33Obj l = m33Store.stack[--m33Store.sp];
		M34Obj res = m34Store.heap[m34Store.hp++];
		res.a = l.a * r.a + l.b * r.e + l.c * r.i;
		res.b = l.a * r.b + l.b * r.f + l.c * r.j;
		res.c = l.a * r.c + l.b * r.g + l.c * r.k;
		res.d = l.a * r.d + l.b * r.h + l.c * r.l;

		res.e = l.d * r.a + l.e * r.e + l.f * r.i;
		res.f = l.d * r.b + l.e * r.f + l.f * r.j;
		res.g = l.d * r.c + l.e * r.g + l.f * r.k;
		res.h = l.d * r.d + l.e * r.h + l.f * r.l;

		res.i = l.g * r.a + l.h * r.e + l.i * r.i;
		res.j = l.g * r.b + l.h * r.f + l.i * r.j;
		res.k = l.g * r.c + l.h * r.g + l.i * r.k;
		res.l = l.g * r.d + l.h * r.h + l.i * r.l;

		m34Store.stack[m34Store.sp++]=res;
	}

	// M34 *

	private final void mulM34M42(){
		M42Obj r = m42Store.stack[--m42Store.sp]; 
		M34Obj l = m34Store.stack[--m34Store.sp];
		M32Obj res = m32Store.heap[m32Store.hp++];
		res.a = l.a * r.a + l.b * r.c + l.c * r.e + l.d * r.g;
		res.b = l.a * r.b + l.b * r.d + l.c * r.f + l.d * r.h;

		res.c = l.e * r.a + l.f * r.c + l.g * r.e + l.h * r.g;
		res.d = l.e * r.b + l.f * r.d + l.g * r.f + l.h * r.h;

		res.e = l.i * r.a + l.j * r.c + l.k * r.e + l.l * r.g;
		res.f = l.i * r.b + l.j * r.d + l.k * r.f + l.l * r.h;

		m32Store.stack[m32Store.sp++]=res;
	}

	private final void mulM34M43(){
		M43Obj r = m43Store.stack[--m43Store.sp]; 
		M34Obj l = m34Store.stack[--m34Store.sp];
		M33Obj res = m33Store.heap[m33Store.hp++];
		res.a = l.a * r.a + l.b * r.d + l.c * r.g + l.d * r.j;
		res.b = l.a * r.b + l.b * r.e + l.c * r.h + l.d * r.k;
		res.c = l.a * r.c + l.b * r.f + l.c * r.i + l.d * r.l;

		res.d = l.e * r.a + l.f * r.d + l.g * r.g + l.h * r.j;
		res.e = l.e * r.b + l.f * r.e + l.g * r.h + l.h * r.k;
		res.f = l.e * r.c + l.f * r.f + l.g * r.i + l.h * r.l;

		res.g = l.i * r.a + l.j * r.d + l.k * r.g + l.l * r.j;
		res.h = l.i * r.b + l.j * r.e + l.k * r.h + l.l * r.k;
		res.i = l.i * r.c + l.j * r.f + l.k * r.i + l.l * r.l;

		m33Store.stack[m33Store.sp++]=res;
	}

	private final void mulM34M44(){
		M44Obj r = m44Store.stack[--m44Store.sp]; 
		M34Obj l = m34Store.stack[--m34Store.sp];
		M34Obj res = m34Store.heap[m34Store.hp++];
		res.a = l.a * r.a + l.b * r.e + l.c * r.i + l.d * r.m;
		res.b = l.a * r.b + l.b * r.f + l.c * r.j + l.d * r.n;
		res.c = l.a * r.c + l.b * r.g + l.c * r.k + l.d * r.o;
		res.d = l.a * r.d + l.b * r.h + l.c * r.l + l.d * r.p;

		res.e = l.e * r.a + l.f * r.e + l.g * r.i + l.h * r.m;
		res.f = l.e * r.b + l.f * r.f + l.g * r.j + l.h * r.n;
		res.g = l.e * r.c + l.f * r.g + l.g * r.k + l.h * r.o;
		res.h = l.e * r.d + l.f * r.h + l.g * r.l + l.h * r.p;

		res.i = l.i * r.a + l.j * r.e + l.k * r.i + l.l * r.m;
		res.j = l.i * r.b + l.j * r.f + l.k * r.j + l.l * r.n;
		res.k = l.i * r.c + l.j * r.g + l.k * r.k + l.l * r.o;
		res.l = l.i * r.d + l.j * r.h + l.k * r.l + l.l * r.p;

		m34Store.stack[m34Store.sp++]=res;
	}

	// M42 * 

	private final void mulM42M22(){
		M22Obj r = m22Store.stack[--m22Store.sp]; 
		M42Obj l = m42Store.stack[--m42Store.sp];
		M42Obj res = m42Store.heap[m42Store.hp++];
		res.a = l.a * r.a + l.b * r.c;
		res.b = l.a * r.b + l.b * r.d;

		res.c = l.c * r.a + l.d * r.c;
		res.d = l.c * r.b + l.d * r.d;

		res.e = l.e * r.a + l.f * r.c;
		res.f = l.e * r.b + l.f * r.d;

		res.g = l.g * r.a + l.h * r.c;
		res.h = l.g * r.b + l.h * r.d;

		m42Store.stack[m42Store.sp++]=res;
	}

	
	private final void mulM42M23(){
		M23Obj r = m23Store.stack[--m23Store.sp]; 
		M42Obj l = m42Store.stack[--m42Store.sp];
		M43Obj res = m43Store.heap[m43Store.hp++];
		res.a = l.a * r.a + l.b * r.d;
		res.b = l.a * r.b + l.b * r.e;
		res.c = l.a * r.c + l.b * r.f;

		res.d = l.c * r.a + l.d * r.d;
		res.e = l.c * r.b + l.d * r.e;
		res.f = l.c * r.c + l.d * r.f;

		res.g = l.e * r.a + l.f * r.d;
		res.h = l.e * r.b + l.f * r.e;
		res.i = l.e * r.c + l.f * r.f;

		res.j = l.g * r.a + l.h * r.d;
		res.k = l.g * r.b + l.h * r.e;
		res.l = l.g * r.c + l.h * r.f;

		m43Store.stack[m43Store.sp++]=res;
	}

	private final void mulM42M24(){
		M24Obj r = m24Store.stack[--m24Store.sp]; 
		M42Obj l = m42Store.stack[--m42Store.sp];
		M44Obj res = m44Store.heap[m44Store.hp++];
		res.a = l.a * r.a + l.b * r.e;
		res.b = l.a * r.b + l.b * r.f;
		res.c = l.a * r.c + l.b * r.g;
		res.d = l.a * r.d + l.b * r.h;

		res.e = l.c * r.a + l.d * r.e;
		res.f = l.c * r.b + l.d * r.f;
		res.g = l.c * r.c + l.d * r.g;
		res.h = l.c * r.d + l.d * r.h;

		res.i = l.e * r.a + l.f * r.e;
		res.j = l.e * r.b + l.f * r.f;
		res.k = l.e * r.c + l.f * r.g;
		res.l = l.e * r.d + l.f * r.h;

		res.m = l.g * r.a + l.h * r.e;
		res.n = l.g * r.b + l.h * r.f;
		res.o = l.g * r.c + l.h * r.g;
		res.p = l.g * r.d + l.h * r.h;
		m44Store.stack[m44Store.sp++]=res;
	}

	// M43 * 

	private final void mulM43M32(){
		M32Obj r = m32Store.stack[--m32Store.sp]; 
		M43Obj l = m43Store.stack[--m43Store.sp];
		M42Obj res = m42Store.heap[m42Store.hp++];
		res.a = l.a * r.a + l.b * r.c + l.c * r.e;
		res.b = l.a * r.b + l.b * r.d + l.c * r.f;

		res.c = l.d * r.a + l.e * r.c + l.f * r.e;
		res.d = l.d * r.b + l.e * r.d + l.f * r.f;

		res.e = l.g * r.a + l.h * r.c + l.i * r.e;
		res.f = l.g * r.b + l.h * r.d + l.i * r.f;

		res.g = l.j * r.a + l.k * r.c + l.l * r.e;
		res.h = l.j * r.b + l.k * r.d + l.l * r.f;
		m42Store.stack[m42Store.sp++]=res;
	}

	private final void mulM43M33(){
		M33Obj r = m33Store.stack[--m33Store.sp]; 
		M43Obj l = m43Store.stack[--m43Store.sp];
		M43Obj res = m43Store.heap[m43Store.hp++];
		res.a = l.a * r.a + l.b * r.d + l.c * r.g;
		res.b = l.a * r.b + l.b * r.e + l.c * r.h;
		res.c = l.a * r.c + l.b * r.f + l.c * r.i;

		res.d = l.d * r.a + l.e * r.d + l.f * r.g;
		res.e = l.d * r.b + l.e * r.e + l.f * r.h;
		res.f = l.d * r.c + l.e * r.f + l.f * r.i;

		res.g = l.g * r.a + l.h * r.d + l.i * r.g;
		res.h = l.g * r.b + l.h * r.e + l.i * r.h;
		res.i = l.g * r.c + l.h * r.f + l.i * r.i;

		res.j = l.j * r.a + l.k * r.d + l.l * r.g;
		res.k = l.j * r.b + l.k * r.e + l.l * r.h;
		res.l = l.j * r.c + l.k * r.f + l.l * r.i;
		m43Store.stack[m43Store.sp++]=res;
	}

	private final void mulM43M34(){
		M34Obj r = m34Store.stack[--m34Store.sp]; 
		M43Obj l = m43Store.stack[--m43Store.sp];
		M44Obj res = m44Store.heap[m44Store.hp++];
		res.a = l.a * r.a + l.b * r.e + l.c * r.i;
		res.b = l.a * r.b + l.b * r.f + l.c * r.j;
		res.c = l.a * r.c + l.b * r.g + l.c * r.k;
		res.d = l.a * r.d + l.b * r.h + l.c * r.l;

		res.e = l.d * r.a + l.e * r.e + l.f * r.i;
		res.f = l.d * r.b + l.e * r.f + l.f * r.j;
		res.g = l.d * r.c + l.e * r.g + l.f * r.k;
		res.h = l.d * r.d + l.e * r.h + l.f * r.l;

		res.i = l.g * r.a + l.h * r.e + l.i * r.i;
		res.j = l.g * r.b + l.h * r.f + l.i * r.j;
		res.k = l.g * r.c + l.h * r.g + l.i * r.k;
		res.l = l.g * r.d + l.h * r.h + l.i * r.l;

		res.m = l.j * r.a + l.k * r.e + l.l * r.i;
		res.n = l.j * r.b + l.k * r.f + l.l * r.j;
		res.o = l.j * r.c + l.k * r.g + l.l * r.k;
		res.p = l.j * r.d + l.k * r.h + l.l * r.l;
		m44Store.stack[m44Store.sp++]=res;
	}
	
	// M44 *
	
	private final void mulM44M42(){
		M42Obj r = m42Store.stack[--m42Store.sp]; 
		M44Obj l = m44Store.stack[--m44Store.sp];
		M42Obj res = m42Store.heap[m42Store.hp++];
		res.a = l.a * r.a + l.b * r.c + l.c * r.e + l.d * r.g;
		res.b = l.a * r.b + l.b * r.d + l.c * r.f + l.d * r.h;

		res.c = l.e * r.a + l.f * r.c + l.g * r.e + l.h * r.g;
		res.d = l.e * r.b + l.f * r.d + l.g * r.f + l.h * r.h;

		res.e = l.i * r.a + l.j * r.c + l.k * r.e + l.l * r.g;
		res.f = l.i * r.b + l.j * r.d + l.k * r.f + l.l * r.h;

		res.g = l.m * r.a + l.n * r.c + l.o * r.e + l.p * r.g;
		res.h = l.m * r.b + l.n * r.d + l.o * r.f + l.p * r.h;

		m42Store.stack[m42Store.sp++]=res;
	}

	private final void mulM44M43(){
		M43Obj r = m43Store.stack[--m43Store.sp]; 
		M44Obj l = m44Store.stack[--m44Store.sp];
		M43Obj res = m43Store.heap[m43Store.hp++];
		res.a = l.a * r.a + l.b * r.d + l.c * r.g + l.d * r.j;
		res.b = l.a * r.b + l.b * r.e + l.c * r.h + l.d * r.k;
		res.c = l.a * r.c + l.b * r.f + l.c * r.i + l.d * r.l;

		res.d = l.e * r.a + l.f * r.d + l.g * r.g + l.h * r.j;
		res.e = l.e * r.b + l.f * r.e + l.g * r.h + l.h * r.k;
		res.f = l.e * r.c + l.f * r.f + l.g * r.i + l.h * r.l;

		res.g = l.i * r.a + l.j * r.d + l.k * r.g + l.l * r.j;
		res.h = l.i * r.b + l.j * r.e + l.k * r.h + l.l * r.k;
		res.i = l.i * r.c + l.j * r.f + l.k * r.i + l.l * r.l;

		res.j = l.m * r.a + l.n * r.d + l.o * r.g + l.p * r.j;
		res.k = l.m * r.b + l.n * r.e + l.o * r.h + l.p * r.k;
		res.l = l.m * r.c + l.n * r.f + l.o * r.i + l.p * r.l;

		m43Store.stack[m43Store.sp++]=res;
	}

	private final void mulM44M44(){
		M44Obj r = m44Store.stack[--m44Store.sp]; 
		M44Obj l = m44Store.stack[--m44Store.sp];
		M44Obj res = m44Store.heap[m44Store.hp++];
		res.a = l.a * r.a + l.b * r.e + l.c * r.i + l.d * r.m;
		res.b = l.a * r.b + l.b * r.f + l.c * r.j + l.d * r.n;
		res.c = l.a * r.c + l.b * r.g + l.c * r.k + l.d * r.o;
		res.d = l.a * r.d + l.b * r.h + l.c * r.l + l.d * r.p;

		res.e = l.e * r.a + l.f * r.e + l.g * r.i + l.h * r.m;
		res.f = l.e * r.b + l.f * r.f + l.g * r.j + l.h * r.n;
		res.g = l.e * r.c + l.f * r.g + l.g * r.k + l.h * r.o;
		res.h = l.e * r.d + l.f * r.h + l.g * r.l + l.h * r.p;

		res.i = l.i * r.a + l.j * r.e + l.k * r.i + l.l * r.m;
		res.j = l.i * r.b + l.j * r.f + l.k * r.j + l.l * r.n;
		res.k = l.i * r.c + l.j * r.g + l.k * r.k + l.l * r.o;
		res.l = l.i * r.d + l.j * r.h + l.k * r.l + l.l * r.p;

		res.m = l.m * r.a + l.n * r.e + l.o * r.i + l.p * r.m;
		res.n = l.m * r.b + l.n * r.f + l.o * r.j + l.p * r.n;
		res.o = l.m * r.c + l.n * r.g + l.o * r.k + l.p * r.o;
		res.p = l.m * r.d + l.n * r.h + l.o * r.l + l.p * r.p;
		m44Store.stack[m44Store.sp++]=res;
	}
	
	private final void mulVnMnn(VecObj l,MatObj r){
		double[] ldata = l.toArrayVec();
		double[][] rdata = r.toArrayMat();
		int rrows = rdata.length;
		int rcols = rdata[0].length;
		double res[]=new double[rcols];
			for(int j=0;j<rcols;++j){
				double ele = ldata[0] * rdata[0][j];
				for(int k=1;k<rrows;++k)
					ele += ldata[k] * rdata[k][j];
				res[j]=ele;
			}
		pushVec(res);
	}

	private final void mulMnnVn(MatObj l,VecObj r){
		double[][] ldata = l.toArrayMat();
		double[] rdata = r.toArrayVec();
		int lrows = ldata.length;
		int lcols = ldata[0].length;
		double res[]=new double[lrows];
		for(int i=0;i<lrows;++i){
			double ele = ldata[i][0] * rdata[0];
				for(int k=1;k<lcols;++k)
					ele += ldata[i][k] * rdata[k];
				res[i]=ele;
			}
		pushVec(res);
	}

private final void mulMnnMnn(MatObj l,MatObj r){
	double[][] ldata = l.toArrayMat();
	double[][] rdata = r.toArrayMat();
	int lrows = ldata.length;
	int rrows = rdata.length;
	int rcols = rdata[0].length;
	double res[][]=new double[lrows][rcols];
	for(int i=0;i<lrows;++i)
		for(int j=0;j<rcols;++j){
			double ele = ldata[i][0] * rdata[0][j];
			for(int k=1;k<rrows;++k)
				ele += ldata[i][k] * rdata[k][j];
			res[i][j]=ele;
		}
	pushMat(res);
}
	private final void pushVec(double[] vec)
	{
		switch(vec.length) {
			case 2:
			{ 	V2Obj v2 = v2Store.heap[v2Store.hp++];
				v2.a = vec[0]; v2.b = vec[1];
				v2Store.stack[v2Store.sp++]=v2;
				break;
			}
			case 3:
			{ 	V3Obj v3 = v3Store.heap[v3Store.hp++];
				v3.a = vec[0]; v3.b = vec[1]; v3.c = vec[2];
				v3Store.stack[v3Store.sp++]=v3;
				break;
			}
			case 4:
			{ 	V4Obj v4 = v4Store.heap[v4Store.hp++];
				v4.a = vec[0]; v4.b = vec[1]; v4.c = vec[2]; v4.d = vec[3];
				v4Store.stack[v4Store.sp++]=v4;
				break;
			}
			default:
				vnStore.stack[vnStore.sp++] = new VnObj(vec);
				break;
		}
	}

	private final void pushMat(double[][] mat)
	{
		switch(mat.length) {
			case 2:
			switch(mat[0].length) {
				case 2:	{
			 	M22Obj m22 = m22Store.heap[m22Store.hp++];
				m22.a = mat[0][0]; m22.b = mat[0][1];
				m22.c = mat[1][0]; m22.d = mat[1][1];
				m22Store.stack[m22Store.sp++]=m22;
				return;
				}
				case 3:	{
			 	M23Obj m23 = m23Store.heap[m23Store.hp++];
				m23.a = mat[0][0]; m23.b = mat[0][1]; m23.c = mat[0][2];
				m23.d = mat[1][0]; m23.e = mat[1][1]; m23.f = mat[1][2];
				m23Store.stack[m23Store.sp++]=m23;
				return;
				}
				case 4:	{ 
				M24Obj m24 = m24Store.heap[m24Store.hp++];
				m24.a = mat[0][0]; m24.b = mat[0][1]; m24.c = mat[0][2]; m24.d = mat[0][3];
				m24.e = mat[1][0]; m24.f = mat[1][1]; m24.g = mat[1][2]; m24.h = mat[1][3];
				m24Store.stack[m24Store.sp++]=m24;
				return;
				}
			}
			break;

			case 3:
			switch(mat[0].length) {
				case 2:	{
				M32Obj m32 = m32Store.heap[m32Store.hp++];
				m32.a = mat[0][0]; m32.b = mat[0][1];
				m32.c = mat[1][0]; m32.d = mat[1][1];
				m32.e = mat[2][0]; m32.f = mat[2][1];
				m32Store.stack[m32Store.sp++]=m32;
				return;
				}
				case 3:	{
				M33Obj m33 = m33Store.heap[m33Store.hp++];
				m33.a = mat[0][0]; m33.b = mat[0][1]; m33.c = mat[0][2];
				m33.d = mat[1][0]; m33.e = mat[1][1]; m33.f = mat[1][2];
				m33.g = mat[2][0]; m33.h = mat[2][1]; m33.i = mat[2][2];
				m33Store.stack[m33Store.sp++]=m33;
				return;
				}
				case 4:	{ 
				M34Obj m34 = m34Store.heap[m34Store.hp++];
				m34.a = mat[0][0]; m34.b = mat[0][1]; m34.c = mat[0][2]; m34.d = mat[0][3];
				m34.e = mat[1][0]; m34.f = mat[1][1]; m34.g = mat[1][2]; m34.h = mat[1][3];
				m34.i = mat[2][0]; m34.j = mat[2][1]; m34.k = mat[2][2]; m34.l = mat[2][3];
				m34Store.stack[m34Store.sp++]=m34;
				return;
				}
			}
			break;
			
			case 4:
			switch(mat[0].length) {
				case 2:	{
				M42Obj m42 = m42Store.heap[m42Store.hp++];
				m42.a = mat[0][0]; m42.b = mat[0][1];
				m42.c = mat[1][0]; m42.d = mat[1][1];
				m42.e = mat[2][0]; m42.f = mat[2][1];
				m42.g = mat[3][0]; m42.h = mat[3][1];
				m42Store.stack[m42Store.sp++]=m42;
				return;
				}
				case 3:	{
				M43Obj m43 = m43Store.heap[m43Store.hp++];
				m43.a = mat[0][0]; m43.b = mat[0][1]; m43.c = mat[0][2];
				m43.d = mat[1][0]; m43.e = mat[1][1]; m43.f = mat[1][2];
				m43.g = mat[2][0]; m43.h = mat[2][1]; m43.i = mat[2][2];
				m43.j = mat[3][0]; m43.k = mat[3][1]; m43.l = mat[3][2];
				m43Store.stack[m43Store.sp++]=m43;
				return;
				}
				case 4:	{ 
				M44Obj m44 = m44Store.heap[m44Store.hp++];
				m44.a = mat[0][0]; m44.b = mat[0][1]; m44.c = mat[0][2]; m44.d = mat[0][3];
				m44.e = mat[1][0]; m44.f = mat[1][1]; m44.g = mat[1][2]; m44.h = mat[1][3];
				m44.i = mat[2][0]; m44.j = mat[2][1]; m44.k = mat[2][2]; m44.l = mat[2][3];
				m44.m = mat[3][0]; m44.n = mat[3][1]; m44.o = mat[3][2]; m44.p = mat[3][3];
				m44Store.stack[m44Store.sp++]=m44;
				return;
				}
			} // end switch
			break;
		} // end switch
		mnnStore.stack[mnnStore.sp++]=new MnnObj(mat);
	} 
	
	private static double LOG10 = Math.log(10.0);

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
	/**Removes observers and other cleanup needed when evaluator no longer used.
	 */
	public void cleanUp()
	{
		scalerStore.cleanUp();
		v2Store.cleanUp();
		v3Store.cleanUp();
		v4Store.cleanUp();
		vnStore.cleanUp();
		m22Store.cleanUp();
		m23Store.cleanUp();
		m24Store.cleanUp();
		m32Store.cleanUp();
		m33Store.cleanUp();
		m34Store.cleanUp();
		m42Store.cleanUp();
		m43Store.cleanUp();
		m44Store.cleanUp();
		mnnStore.cleanUp();
	}
}
