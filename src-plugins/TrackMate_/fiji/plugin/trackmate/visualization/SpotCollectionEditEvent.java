package fiji.plugin.trackmate.visualization;

import java.util.EventObject;

import fiji.plugin.trackmate.Spot;

public class SpotCollectionEditEvent extends EventObject {
	
	private static final long serialVersionUID = -5460002669831526802L;
	public static final int 	SPOT_CREATED = 0;
	public static final int 	SPOT_DELETED = 1;
	public static final int 	SPOT_MODIFIED = 2;
	public static final int 	SPOT_FRAME_CHANGED = 3;

	
	protected Spot[] spots;
	protected int flag;
	protected Integer fromFrame;
	protected Integer toFrame;

	public SpotCollectionEditEvent(Object source, Spot[] spots, int flag, Integer fromFrame, Integer toFrame) {
		super(source);
		this.spots = spots;
		this.flag = flag;
		this.fromFrame = fromFrame;
		this.toFrame = toFrame;
	}
	
	public Spot[] getSpots() {
		return spots;
	}
	
	public int getFlag() {
		return flag;
	}
	
	public Integer getToFrame() {
		return toFrame;
	}
	
	public Integer getFromFrame() {
		return fromFrame;
	}
	
	
}
