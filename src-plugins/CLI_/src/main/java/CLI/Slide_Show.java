package CLI;
import ij.IJ;
import ij.io.Opener;
import ij.io.DirectoryChooser;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ij.Macro;
import ij.plugin.frame.Recorder;
import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class Slide_Show implements PlugIn {

	String folder_name;
	String file_name;
	String[] image_file;
	int seconds;
	boolean close;
	boolean pause;
	int index; //the current slide
	JButton b_play;
	
	public void run(String args) {

		int f=0, end=0;

		String arg = Macro.getOptions(); //what a strange way of getting the options passed to a plugin! One would expect to grab them from the String args in the arguments of the run function.
		if (null == arg) {
			arg = ""; //just to make sure statements below don't fail because of a null
		}
		
		//Get folder name from argument or ask for the user to choose it
		folder_name = null;
		f = arg.indexOf("folder=");
		if (-1 != f) {
			end = arg.indexOf(' ', f);
			if (-1 == end) {
				end = arg.length();
			}
			folder_name = arg.substring(f+7, end);
		} else {
			DirectoryChooser dc = new DirectoryChooser("Select a directory");
			folder_name = dc.getDirectory(); //it includes the final slash already
			//finish here if it was canceled
			if (null == folder_name) return;
		}

		//Get file name if any
		file_name = null;
		f = arg.indexOf("file=");
		if (-1 != f) {
			end = arg.indexOf(' ', f);
			if (-1 == end) {
				end = arg.length();
			}
			file_name = arg.substring(f+5, end);
		}

		//Get timer event if any
		seconds = 5;
		f = arg.indexOf("time=");
		if (-1 != f) {
			end = arg.indexOf(' ', f);
			if (-1 == end) {
				end = arg.length();
			}
			seconds = Integer.parseInt(arg.substring(f+5, end));
		}

		//Set the macro arguments for the recorder:

		//NO! //Macro.setOptions("folder=" + folder_name + file_name!=null?" file="+file_name:" " + "time=" + seconds);
		Recorder.setCommand("Slide Show");
		Recorder.recordOption("folder", folder_name);
		if (null != file_name) Recorder.recordOption("file", file_name);
		Recorder.recordOption("time", ""+seconds);
		Recorder.saveCommand();

		//Get a list of all image files in directory
		
		File folder = new File(folder_name);
		if (!folder.exists()) {
			IJ.showMessage("Selected folder "+folder.getName()+" does not exist.");
			return;
		}
		image_file = folder.list(new ImageFileFilter(file_name));
		//above: if file_name is null all image files in the folder will be considered
		
		//show GUI
		final JFrame frame = new JFrame("Slide Show");
		final JPanel panel = new JPanel();
		b_play = new JButton("  Pause ");
		b_play.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = b_play.getActionCommand();
				if (command == "  Pause ") {
					pause = true;
					b_play.setText("Continue");
					b_play.setActionCommand("Continue");
				} else {
					pause = false;
					b_play.setText("  Pause ");
					b_play.setActionCommand("  Pause ");
					frame.pack(); //recalculate GUI size
					new SlideShow().start();
				}
			}
		});
		final JButton b_keep = new JButton("Keep open");
		b_keep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close = false;
			}
		});
		final JSlider slider = new JSlider(1, 60, 5);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				seconds = slider.getValue();
			}
		});
		panel.add(b_play);
		panel.add(b_keep);
		panel.add(slider);
		frame.getContentPane().add(panel);
		frame.pack();
		frame.show();
		
		//Open every image after that many seconds, and close the previous
		index = 0; //start from first slide
		close = true;
		new SlideShow().start();
	}

	private class SlideShow extends Thread {
		
		public void run() {
			Opener o = new Opener();
			ImagePlus img = null;

			for (int i=index; i<image_file.length; i++) {
				try {
					img = o.openImage(folder_name, image_file[i]);
					if (null != img) {
						img.show();
						long waiting_time = seconds * 1000; //in miliseconds
						this.sleep(waiting_time);
						if (pause) {
							index = i+1; //so next time starts at the next one
							return;
						}
						if (null != img && close) {
							img.hide();
							img.flush();
						} else {
							//don't close it
							// and reset the close value for the next
							close = true;
							//WindowManager keeps a pointer to it so there's no need to do so here.
						}
					} else {
						IJ.log("Couldn't open image " + image_file[i]);
					}
				}catch(Exception e) {
					IJ.log("Some error ocurred, but continuing happily.\n" + e);
					// Cleanup
					if (null != img) {
						img.hide();
						img.flush();
						img = null;
					}
				}
			}
			//we got to the end of the image_file list, so reset all:
			//reset index
			index = 0;
			b_play.setText("Restart ");
			b_play.setActionCommand("Continue");
		}
	}
}
