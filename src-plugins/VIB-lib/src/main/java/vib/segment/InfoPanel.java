package vib.segment;

import java.awt.*;

public class InfoPanel extends Panel {
	private Label lPosX, lPosY, lPosZ;
	private Label lIndexX, lIndexY, lIndexZ;
	private Label lMaterialName, lVoxelValueNum;
	private Font font;
	
	public InfoPanel(Font font) {
		super();
		this.font = font;
		GridBagConstraints constr = new GridBagConstraints();
		constr.weightx = 1.0;
		constr.fill = GridBagConstraints.HORIZONTAL;
		
		setLayout(new GridBagLayout());
		
		addLabel("Pos:", constr);
		lPosX = addLabel("     X     ", constr);
		lPosY = addLabel("     Y     ", constr);
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		lPosZ = addLabel("     Z     ", constr);

		constr.gridwidth = 1;
		addLabel("Index:", constr);
		lIndexX = addLabel("     X     ", constr);
		lIndexY = addLabel("     Y     ", constr);
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		lIndexZ = addLabel("     Z     ", constr);
		
		constr.gridwidth = 1;
		addLabel("Material:", constr);
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		lMaterialName = addLabel("name ", constr);
		
		constr.gridwidth = 1;
		addLabel("Voxel Value:", constr);
		
		constr.gridwidth = GridBagConstraints.REMAINDER;
		lVoxelValueNum = addLabel("______ ~ ______", constr);
	}

	public void updateLabels() {
		lIndexX.setText("    -    ");
		lIndexY.setText("    -    ");
		lIndexZ.setText("    -    ");
		lPosX.setText("    X    ");
		lPosY.setText("    Y    ");
		lPosZ.setText("    Z    ");
		lMaterialName.setText("    -    ");
		lVoxelValueNum.setText("    -    ");
	}

	public void updateLabels(int x, int y, int z,
			double posX, double posY, double posZ,
			int value, String name) {
		lIndexX.setText("" + x);
		lIndexY.setText("" + y);
		lIndexZ.setText("" + z);
		lPosX.setText("" + posX);
		lPosY.setText("" + posY);
		lPosZ.setText("" + posZ);
		lMaterialName.setText(name);
		lVoxelValueNum.setText("" + value);
	}

	private Label addLabel(String s, GridBagConstraints c) {
		Label label = new Label(s);
		label.setFont(font);
		this.add(label, c);
		return label;
	}
}
