import java.util.Iterator;

import ij.IJ;
import ij.ImagePlus;
import ij.text.TextWindow;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;

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
			} else if ( r instanceof Result.Histogram2DResult) {
				Result.Histogram2DResult result = (Result.Histogram2DResult)r;
				ImagePlus imp = ImageJFunctions.displayAsVirtualStack( result.getData() );
				imp.show();
			}
		}
		IJ.selectWindow("Results");
	}
}
