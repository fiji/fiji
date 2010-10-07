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
				return "<html>Hi all!<br>this is a help text.</html>";
			}
			
			return null;
		}
	}
	
}
