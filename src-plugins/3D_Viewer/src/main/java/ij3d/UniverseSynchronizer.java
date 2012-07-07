package ij3d;

import java.util.HashMap;

import com.sun.j3d.utils.universe.MultiTransformGroup;

import javax.media.j3d.View;

import ij3d.DefaultUniverse.GlobalTransform;

public class UniverseSynchronizer {

	private HashMap<Image3DUniverse, UniverseListener> universes =
		new HashMap<Image3DUniverse, UniverseListener>();


	void addUniverse(final Image3DUniverse u) {
		UniverseListener l = new UniverseAdapter() {
			public void transformationUpdated(View view) {
				GlobalTransform xform = new GlobalTransform();
				u.getGlobalTransform(xform);
				for(Image3DUniverse o : universes.keySet())
					if(!o.equals(u))
						setGlobalTransform(o, xform);
			}

			public void universeClosed() {
				removeUniverse(u);
			}
		};
		u.addUniverseListener(l);
		universes.put(u, l);
	}

	void removeUniverse(Image3DUniverse u) {
		if(!universes.containsKey(u))
			return;
		UniverseListener l = universes.get(u);
		u.removeUniverseListener(l);
		universes.remove(u);
	}

	/* Need to implement this here again in order to prevent
	 * firing transformationUpdated() */
	private static final GlobalTransform old = new GlobalTransform();
	private static final void setGlobalTransform(
			Image3DUniverse u, GlobalTransform transform) {
		u.getGlobalTransform(old);
		if(equals(old, transform))
			return;
		MultiTransformGroup group =
			u.getViewingPlatform().getMultiTransformGroup();
		int num = group.getNumTransforms();
		for (int i = 0; i < num; i++)
			group.getTransformGroup(i).setTransform(
				transform.transforms[i]);
		u.fireTransformationUpdated();
	}

	private static final boolean equals(
			GlobalTransform t1, GlobalTransform t2) {
		int num = t1.transforms.length;
		for (int i = 0; i < num; i++)
			if(!t1.transforms[i].equals(t2.transforms[i]))
				return false;
		return true;
	}

	private class UniverseAdapter implements UniverseListener {

		public void transformationStarted(View view) {}

		public void transformationUpdated(View view) {}
		public void transformationFinished(View view) {}

		public void contentAdded(Content c) {}
		public void contentRemoved(Content c) {}
		public void contentChanged(Content c) {}
		public void contentSelected(Content c) {}
		public void canvasResized() {}
		public void universeClosed() {}
	}
}
