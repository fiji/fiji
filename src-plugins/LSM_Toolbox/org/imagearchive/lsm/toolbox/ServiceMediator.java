package org.imagearchive.lsm.toolbox;

import org.imagearchive.lsm.toolbox.gui.ControlPanelFrame;
import org.imagearchive.lsm.toolbox.gui.DetailsFrame;
import org.imagearchive.lsm.toolbox.gui.InfoFrame;

public class ServiceMediator {

	private MasterModel masterModel;

	private ControlPanelFrame controlPanelFrame;

	private InfoFrame infoFrame;

	private DetailsFrame detailsFrame;

	public MasterModel getMasterModel() {
		return masterModel;
	}

	public void registerMasterModel(MasterModel masterModel) {
		this.masterModel = masterModel;
	}

	public void registerControlPanelFrame(ControlPanelFrame controlPanelFrame) {
		this.controlPanelFrame = controlPanelFrame;
	}

	public void registerInfoFrame(InfoFrame infoFrame) {
		this.infoFrame = infoFrame;
	}

	public void registerDetailsFrame(DetailsFrame detailsFrame) {
		this.detailsFrame = detailsFrame;
	}

	public ControlPanelFrame getControlPanelFrame() {
		return controlPanelFrame;
	}

	public InfoFrame getInfoFrame() {
		return infoFrame;
	}

	public DetailsFrame getDetailsFrame() {
		return detailsFrame;
	}
}
