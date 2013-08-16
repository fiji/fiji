package fiji.util.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class JFrameCommandFinder extends CommandFinderBase {
	JMenuBar menuBar;

	public JFrameCommandFinder(String title, JFrame frame) {
		super(title);
		this.menuBar = frame.getJMenuBar();
	}

	@Override
	public void populateActions() {
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			JMenu menu = menuBar.getMenu(i);
			populateActions(menu, menu.getLabel());
		}
	}

	protected void populateActions(JMenu menu, String menuLocation) {
		for (int i = 0; i < menu.getItemCount(); i++) {
			JMenuItem item = menu.getItem(i);
			if (item == null)
				continue;
			String location = menuLocation + ">" + item.getLabel();
			if (item instanceof JMenu)
				populateActions((JMenu)item, location);
			else
				actions.add(new JMenuItemAction(item, location));
		}
	}

	protected class JMenuItemAction extends Action {
		JMenuItem menuItem;

		public JMenuItemAction(JMenuItem menuItem, String menuLocation) {
			super(menuItem.getLabel(), menuLocation);
			this.menuItem = menuItem;
		}

		public void run() {
			ActionEvent event = new ActionEvent(menuItem, ActionEvent.ACTION_PERFORMED, label);
			for (ActionListener listener : menuItem.getActionListeners())
				listener.actionPerformed(event);
		}
	}

	public static void main(String[] args) {
		JFrame frame = null;
		for (java.awt.Frame f : ij.WindowManager.getNonImageWindows())
			if (f instanceof JFrame)
				frame = (JFrame)f;
		new JFrameCommandFinder("JFrame Command Finder Demo", frame).setVisible(true);
	}
}