package fiji.plugin.trackmate.tracking;

public class TrackerSettings {

	public enum TrackerType {
		LAP_TRACKER;
		
		@Override
		public String toString() {
			switch(this) {
			case LAP_TRACKER:
				return "LAP tracker";
			}
			return null;
		}

		public TrackerSettings createSettings() {
			switch(this) {
			case LAP_TRACKER:
				return new TrackerSettings();			
			}
			return null;
		}

		public String getInfoText() {
			switch(this) {
			case LAP_TRACKER:
				return "<html>" +
						"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
						"Its implementation is derived from the following paper: <br>" +
						"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
						"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
						" </html>";
			}
			
			return null;
		}
	}
	
}
