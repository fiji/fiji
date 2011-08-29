package fiji.updater.ui;

import fiji.updater.util.Canceled;
import fiji.updater.util.Progress;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class ProgressDialog extends JDialog implements Progress {
	JProgressBar progress;
	JButton detailsToggle;
	int toggleHeight = -1;
	JScrollPane detailsScrollPane;
	Details details;
	Detail latestDetail;
	String title;
	boolean canceled;
	protected long latestUpdate;

	public ProgressDialog(Frame owner) {
		this(owner, null);
	}

	public ProgressDialog(Frame owner, String title) {
		super(owner);

		Container root = getContentPane();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		progress = new JProgressBar();
		progress.setMinimum(0);
		root.add(progress);

		JPanel buttons = new JPanel();
		detailsToggle = new JButton("Show Details");
		detailsToggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				toggleDetails();
			}
		});
		buttons.add(detailsToggle);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canceled = true;
				ProgressDialog.this.dispose();
			}
		});
		buttons.add(cancel);
		buttons.setMaximumSize(buttons.getMinimumSize());
		root.add(buttons);

		details = new Details();
		detailsScrollPane = new JScrollPane(details,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		detailsScrollPane.setVisible(false);
		root.add(detailsScrollPane);

		if (title != null)
			setTitle(title);
		pack();

		if (owner != null) {
			Dimension o = owner.getSize();
			Dimension size = getSize();
			if (size.width < o.width / 2) {
				size.width = o.width / 2;
				setSize(size);
			}
			setLocation(owner.getX() + (o.width - size.width) / 2,
				owner.getY() + (o.height - size.height) / 2);
		}

		KeyAdapter keyAdapter = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
					cancel();
			}
		};
		root.addKeyListener(keyAdapter);
		detailsToggle.addKeyListener(keyAdapter);
		cancel.addKeyListener(keyAdapter);

		setVisible(true);
	}

	public void cancel() {
		canceled = true;
	}

	protected void checkIfCanceled() {
		if (canceled)
			throw new Canceled();
	}

	public void setTitle(String title) {
		this.title = title;
		progress.setStringPainted(true);
		setTitle();
	}

	protected void setTitle() {
		checkIfCanceled();
		if (detailsScrollPane.isVisible() || latestDetail == null)
			progress.setString(title);
		else
			progress.setString(title + ": "
					+ latestDetail.getString());
		repaint();
	}

	public void setCount(int count, int total) {
		checkIfCanceled();
		if (updatesTooFast())
			return;
		progress.setMaximum(total);
		progress.setValue(count);
		repaint();
	}

	public void addItem(Object item) {
		checkIfCanceled();
		details.addDetail(item.toString());
		if (updatesTooFast() && !detailsScrollPane.isVisible())
			return;
		setTitle();
		validate();
		repaint();
	}

	public void setItemCount(int count, int total) {
		checkIfCanceled();
		if (updatesTooFast())
			return;
		latestDetail.setMaximum(total);
		latestDetail.setValue(count);
		repaint();
	}

	public void itemDone(Object item) {
		checkIfCanceled();
		latestDetail.setValue(latestDetail.getMaximum());
	}

	public void done() {
		if (latestDetail != null)
			latestDetail.setValue(latestDetail.getMaximum());
		progress.setValue(progress.getMaximum());
		dispose();
	}

	public void toggleDetails() {
		boolean show = !detailsScrollPane.isVisible();
		detailsScrollPane.setVisible(show);
		detailsScrollPane.invalidate();
		detailsToggle.setText(show ?  "Hide Details" : "Show Details");
		setTitle();

		Dimension dimension = getSize();
		if (toggleHeight == -1)
			toggleHeight = dimension.height + 100;
		setSize(new Dimension(dimension.width, toggleHeight));
		toggleHeight = dimension.height;
	}

	class Details extends JPanel {
		Details() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		}

		public void addDetail(String title) {
			addDetail(new Detail(title));
		}

		public void addDetail(Detail detail) {
			add(detail);
			JScrollBar vertical =
				detailsScrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());
			latestDetail = detail;
		}
	}

	class Detail extends JProgressBar {
		Detail(String text) {
			setStringPainted(true);
			setString(text);
		}
	}

	protected boolean updatesTooFast() {
		if (System.currentTimeMillis() - latestUpdate < 50)
			return true;
		latestUpdate = System.currentTimeMillis();
		return false;
	}

	public static void main(String[] args) {
		ProgressDialog dialog =
			new ProgressDialog(null, "Hello");
		dialog.addItem("Bello");
		dialog.setVisible(true);
	}
}
