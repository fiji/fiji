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

		printTextStatistics(container);

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

	protected void printTextStatistics(DataContainer container){
		textWindow.getTextPanel().appendLine("Ch1 Mean\t" + container.getMeanCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Mean\t" + container.getMeanCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Min\t" + container.getMinCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Min\t" + container.getMinCh2() + "\n");
		textWindow.getTextPanel().appendLine("Ch1 Max\t" + container.getMaxCh1() + "\n");
		textWindow.getTextPanel().appendLine("Ch2 Max\t" + container.getMaxCh2() + "\n");
	}
}
