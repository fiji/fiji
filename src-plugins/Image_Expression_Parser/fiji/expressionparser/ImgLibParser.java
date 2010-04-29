package fiji.expressionparser;

import java.util.ArrayList;

import mpicbg.imglib.type.numeric.RealType;

import org.nfunk.jep.JEP;
import org.nfunk.jep.type.NumberFactory;

import fiji.expressionparser.function.ImgLibAbs;
import fiji.expressionparser.function.ImgLibArcCosine;
import fiji.expressionparser.function.ImgLibArcSine;
import fiji.expressionparser.function.ImgLibArcTangent;
import fiji.expressionparser.function.ImgLibArcTangent2;
import fiji.expressionparser.function.ImgLibCosine;
import fiji.expressionparser.function.ImgLibExp;
import fiji.expressionparser.function.ImgLibFunction;
import fiji.expressionparser.function.ImgLibGaussConv;
import fiji.expressionparser.function.ImgLibLog;
import fiji.expressionparser.function.ImgLibModulus;
import fiji.expressionparser.function.ImgLibPower;
import fiji.expressionparser.function.ImgLibSine;
import fiji.expressionparser.function.ImgLibSquareRoot;
import fiji.expressionparser.function.ImgLibTangent;

public class ImgLibParser <T extends RealType<T>> extends JEP {

	public ImgLibParser() {
		super();
		opSet = new ImgLibOperatorSet<T>();
	}

	public ImgLibParser(JEP j) {
		super(j);
		opSet = new ImgLibOperatorSet<T>();
	}

	public ImgLibParser(boolean traverseIn, boolean allowUndeclaredIn,
			boolean implicitMulIn, NumberFactory numberFactoryIn) {
		super(traverseIn, allowUndeclaredIn, implicitMulIn, numberFactoryIn);
		opSet = new ImgLibOperatorSet<T>();
	}
	
	@Override
	public void addStandardFunctions() {
		
		ImgLibSine<T> sine = new ImgLibSine<T>(); 
		ImgLibCosine<T> cosine = new ImgLibCosine<T>();
		ImgLibTangent<T> tangent = new ImgLibTangent<T>();
		ImgLibArcSine<T> arcsine = new ImgLibArcSine<T>();
		ImgLibArcCosine<T> arccosine = new ImgLibArcCosine<T>();
		ImgLibArcTangent<T> arctangent = new ImgLibArcTangent<T>();
		ImgLibArcTangent2<T> arctangent2 = new ImgLibArcTangent2<T>();
		ImgLibLog<T> log = new ImgLibLog<T>();
		ImgLibExp<T> exp = new ImgLibExp<T>();
		ImgLibPower<T> power = new ImgLibPower<T>();
		ImgLibSquareRoot<T> sqrt = new ImgLibSquareRoot<T>();
		ImgLibAbs<T> abs = new ImgLibAbs<T>();
		ImgLibModulus<T> modulus = new ImgLibModulus<T>();
		
		ArrayList<ImgLibFunction<T>> il_funs = new ArrayList<ImgLibFunction<T>>();
		il_funs.add(sine);
		il_funs.add(cosine);
		il_funs.add(tangent);
		il_funs.add(arcsine);
		il_funs.add(arccosine);
		il_funs.add(arctangent);
		il_funs.add(arctangent2);
		il_funs.add(log);
		il_funs.add(exp);
		il_funs.add(power);
		il_funs.add(sqrt);
		il_funs.add(abs);
		il_funs.add(modulus);
		
		for (ImgLibFunction<T> il_fun : il_funs) {
			funTab.put(il_fun.getFunctionString(), il_fun);
		}
		
	}
	
	/**
	 * Add ImgLib algorithms to the parser, such as gaussian convolution.
	 */
	public void addImgLibAlgorithms() {
		ImgLibGaussConv<T> gauss = new ImgLibGaussConv<T>();
		
		ArrayList<ImgLibFunction<T>> il_algos = new ArrayList<ImgLibFunction<T>>();
		il_algos.add(gauss);
		
		for (ImgLibFunction<T> il_algo : il_algos) {			
			funTab.put(il_algo.getFunctionString(), il_algo);
		}
	}

}
