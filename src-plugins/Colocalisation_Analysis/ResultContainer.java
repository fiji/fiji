import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

public class ResultContainer<T extends RealType<T>> implements Iterable<Result>{

	Image<T> sourceImage1, sourceImage2;
	int ch1, ch2;
	List<Result> resultsObjectList = new ArrayList<Result>();

	public void add(Result result){
		resultsObjectList.add(result);
	}

	public Iterator<Result> iterator() {
		return resultsObjectList.iterator();
	}
}