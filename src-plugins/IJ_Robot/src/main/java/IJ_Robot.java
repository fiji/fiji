import ij.*;
import ij.gui.*;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;

public class IJ_Robot implements PlugIn {
    
    public void run(String arg) {
        if (!IJ.isJava2()) {
            IJ.error("IJ_Robot", "Java 1.3 or later required");
            return;
        }

		GenericDialog gd = new GenericDialog("IJ_Robot", IJ.getInstance());
		gd.addMessage("IJ Robot v 1.0 by G. Landini");

		String [] DisplayOption={ "Left_Click", "Right_Click", "Middle_Click", "Left_Down", "Left_Up", "Right_Down", "Right_Up", "Middle_Down", "Middle_Up", "Move", "KeyPress", "GetPixel", "CaptureScreen"};

		gd.addChoice("Order", DisplayOption, DisplayOption[0]);
		gd.addMessage("Only 'Clicks' and 'Move' require X, Y coordinates.");
		gd.addNumericField ("X_point:",  0, 0);
		gd.addNumericField ("Y_point:",  0, 0);
		gd.addMessage("'Delay' is the time spent in one click.");
		gd.addNumericField ("Delay (ms):",  300, 0);
		gd.addMessage("KeyPress supports 0-9 a-z A-Z space /.,-\nTo emulate the [enter] key, type '!'\nOther characters are converted to '.'");
		gd.addStringField ("KeyPress:",  "",10);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String robotOption = gd.getNextChoice ();
		int robotX = (int)gd.getNextNumber();
		int robotY = (int)gd.getNextNumber();
		int robotDelay = (int)gd.getNextNumber();
		String robotText = gd.getNextString();
	
		try {
			Robot robot = new Robot();

			if (robotOption.equals("Left_Click")){
				robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.delay(robotDelay);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			}

			if (robotOption.equals("Left_Down")){
				//robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON1_MASK);
			}

			if (robotOption.equals("Left_Up")){
				//robot.mouseMove(robotX, robotY);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			}

			if (robotOption.equals("Right_Click")){
				robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON3_MASK);
				robot.delay(robotDelay);
				robot.mouseRelease(InputEvent.BUTTON3_MASK);
			}

			if (robotOption.equals("Right_Down")){
				//robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON3_MASK);
			}

			if (robotOption.equals("Right_Up")){
				//robot.mouseMove(robotX, robotY);
				robot.mouseRelease(InputEvent.BUTTON3_MASK);
			}

			if (robotOption.equals("Middle_Down")){
				//robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON2_MASK);
			}

			if (robotOption.equals("Middle_Up")){
				//robot.mouseMove(robotX, robotY);
				robot.mouseRelease(InputEvent.BUTTON2_MASK);
			}
			if (robotOption.equals("Middle_Click")){
				robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON2_MASK);
				robot.delay(robotDelay);
				robot.mouseRelease(InputEvent.BUTTON2_MASK);
			}

			if (robotOption.equals("Move")){
				robot.mouseMove(robotX, robotY);
			}

			if (robotOption.equals("KeyPress")){
				//robot.mouseMove(robotX, robotY);
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.delay(robotDelay);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);

				for (int i=0;i<robotText.length();i++){
					char c = robotText.charAt(i);
					int kc;
					if (c >= '0' && c <= '9')
						kc = Character.getNumericValue(c) + 48;
					else if (c >= 'a' && c <= 'z')
						kc = Character.getNumericValue(c) + 55;
					else if (c == '/')
						kc = KeyEvent.VK_SLASH;
					//else if (c == '@')
					//	kc = KeyEvent.VK_AT; //not working in linux
					else if (c == ' ')
						kc = KeyEvent.VK_SPACE;
					else if (c == '-')
						kc = KeyEvent.VK_MINUS;
					//else if (c == '_')
					//	kc = KeyEvent.VK_UNDERSCORE; //not working in linux, returns '-'
					else if (c == '.')
						kc = KeyEvent.VK_PERIOD;
					else if (c == ',')
						kc = KeyEvent.VK_COMMA;
					else if (c == '!')
						kc = KeyEvent.VK_ENTER;
					else if (c >= 'A' && c <= 'Z')
						kc = Character.getNumericValue(c) + 55;
					else
						kc=KeyEvent.VK_PERIOD;

					if (c >= 'A' && c <= 'Z')
						robot.keyPress(KeyEvent.VK_SHIFT);// shift on

					// press and release
					robot.keyPress(kc);
					robot.keyRelease(kc);

					if (c >= 'A' && c <= 'Z')
						robot.keyRelease(KeyEvent.VK_SHIFT); //shift off
				}
			}

			if (robotOption.equals("GetPixel")){
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				IJ.log("Width: "+ (int) toolkit.getScreenSize().getWidth()+"  Height: "+(int) toolkit.getScreenSize().getHeight());
				IJ.log("Pixel at x="+robotX+"  y="+robotY+" : "+robot.getPixelColor(robotX, robotY));
			}

			if (robotOption.equals("CaptureScreen")){
			//	try {
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Dimension dimension = toolkit.getScreenSize();
				Rectangle r = new Rectangle(dimension);
				Image img = robot.createScreenCapture(r);
				if (img!=null)
					new ImagePlus("Screen", img).show();
			}
		}
		catch(Exception e) {
		}
	}
}
