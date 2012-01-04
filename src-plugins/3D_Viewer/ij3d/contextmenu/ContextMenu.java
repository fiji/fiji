package ij3d.contextmenu;

import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Executer;
import ij3d.Image3DUniverse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class ContextMenu implements ActionListener, ItemListener, ContentConstants {

	private JPopupMenu popup = new JPopupMenu();

	private Image3DUniverse univ;
	private Executer executer;

	private Content content;

	private JMenuItem slices, updateVol, fill, smoothMesh, smoothAllMeshes, smoothDialog, colorSurface, decimateMesh;
	private JCheckBoxMenuItem shaded, saturated;

	public ContextMenu (Image3DUniverse univ) {

		this.univ = univ;
		this.executer = univ.getExecuter();

		slices = new JMenuItem("Adjust slices");
		slices.addActionListener(this);
		popup.add(slices);

		updateVol = new JMenuItem("Update Volume");
		updateVol.addActionListener(this);
		popup.add(updateVol);

		fill = new JMenuItem("Fill selection");
		fill.addActionListener(this);
		popup.add(fill);

		JMenu smooth = new JMenu("Smooth");
		popup.add(smooth);

		smoothMesh = new JMenuItem("Smooth mesh");
		smoothMesh.addActionListener(this);
		smooth.add(smoothMesh);

		smoothAllMeshes = new JMenuItem("Smooth all meshes");
		smoothAllMeshes.addActionListener(this);
		smooth.add(smoothAllMeshes);

		decimateMesh = new JMenuItem("Decimate mesh");
		decimateMesh.addActionListener(this);
		popup.add(decimateMesh);

		smoothDialog = new JMenuItem("Smooth control");
		smoothDialog.addActionListener(this);
		smooth.add(smoothDialog);

		shaded = new JCheckBoxMenuItem("Shade surface");
		shaded.setState(true);
		shaded.addItemListener(this);
		popup.add(shaded);

		saturated = new JCheckBoxMenuItem("Saturated volume rendering");
		saturated.setState(false);
		saturated.addItemListener(this);
		popup.add(saturated);

		colorSurface = new JMenuItem("Color surface from image");
		colorSurface.addActionListener(this);
		popup.add(colorSurface);

	}

	public void showPopup(MouseEvent e) {
		content = univ.getPicker().getPickedContent(e.getX(), e.getY());
		if(content == null)
			return;
		univ.select(content);
		shaded.setState(content.isShaded());
		saturated.setState(content.isSaturatedVolumeRendering());
		if(popup.isPopupTrigger(e))
			popup.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object src = e.getSource();
		if(src == shaded)
			executer.setShaded(content, shaded.getState());
		else if(src == saturated)
			executer.setSaturatedVolumeRendering(content, saturated.getState());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == updateVol)
			executer.updateVolume(content);
		else if (src == slices)
			executer.changeSlices(content);
		else if (src == fill)
			executer.fill(content);
		else if (src == smoothMesh)
			executer.smoothMesh(content);
		else if (src == smoothAllMeshes)
			executer.smoothAllMeshes();
		else if (src == smoothDialog)
			executer.smoothControl();
		else if (src == decimateMesh)
			executer.decimateMesh();
		else if(src == colorSurface)
			executer.applySurfaceColors(content);
	}
}
