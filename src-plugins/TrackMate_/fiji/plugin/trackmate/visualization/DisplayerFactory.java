package fiji.plugin.trackmate.visualization;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Listable;

public class DisplayerFactory implements Listable<SpotDisplayer> {

	protected List<SpotDisplayer> displayers;
	
	public DisplayerFactory() {
		this.displayers = createDisplayerList();
	}
	

	protected List<SpotDisplayer> createDisplayerList() {
		ArrayList<SpotDisplayer> list = new ArrayList<SpotDisplayer>();
		list.add(new HyperStackDisplayer());
		list.add(new SpotDisplayer3D());
		return list;
	}


	@Override
	public List<SpotDisplayer> getList() {
		return displayers;
	}

}
