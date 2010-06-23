import java.util.Iterator;

import ij.IJ;
import ij.text.TextWindow;

public class EasyDisplay extends Display{

	protected static TextWindow textWindow;


	//implement the abstract display method.
	public void display(DataContainer container){

		//test if the results windows is already there, if so use it.
		if (textWindow == null || !textWindow.isVisible())
			textWindow = new TextWindow("Results",
				"Result\tValue\n", "", 400, 250);
		Iterator<Result> iterator = container.iterator();
		while (iterator.hasNext()){
			Result r = iterator.next();
			if (r instanceof Result.SimpleValueResult){
				Result.SimpleValueResult result = (Result.SimpleValueResult)r;
				textWindow.getTextPanel().appendLine(result.getName() + "\t" + result.getValue() + "\n");
			}
		}
		IJ.selectWindow("Results");
	}
}
