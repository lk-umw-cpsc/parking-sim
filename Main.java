import javax.swing.SwingUtilities;

/**
 * Program entry point class.
 * 
 * NOTE: Settings required for use:
 * 
 * A 1m x 1m space is needed, centered in camera view.
 * Use Motive version 2.1.1.
 * Under "View -> Data Streaming Pane" in Motive, 
 * enable "Broadcast Frame Data",
 * set Local Interface to "loopback",
 * set Up Axis to "Z Up",
 * 
 * See "images/Motive Setting.PNG" for exact settings used.
 * 
 * Instructions for use:
 * 
 * Calibrate Motive cameras if needed,
 * Turn on RC car and its remote,
 * Place car in center of 1m x 1m space,
 * Place alignment tool directly in front of car,
 * Within Motive, create a rigid body with the RC car's markers,
 * giving it an ID of 0 via the Properties panel -> Streaming ID
 * 
 * Create another rigid body with the alignment tool's markers,
 * giving it an ID of 1.
 * 
 * Start the Java program, using this class as the main class.
 * Press any key to start playing!
 * 
 * The alignment tool ensures that the virtual car faces the correct
 * direction.
 * 
 * @author Lauren Knight
 */
public class Main {

    /**
     * Program entry point
     * @param args Arguments passed to the program by the OS
     */
    public static void main(String[] args) {
        // initialize the GUI elements on the Swing event thread (required)
        SwingUtilities.invokeLater(Main::initializeSwingComponents);
    }

    /**
     * This method will be called on the Swing UI event thread.
     * 
     * Creates the main frame for the application and all of its
     * children, causing the program to start.
     */
    private static void initializeSwingComponents() {
        // create a new ApplicationFrame (our main window) and make it visible
        ApplicationFrame frame = new ApplicationFrame();
        // center the frame
        frame.setLocationRelativeTo(null);
        // make it visible
        frame.setVisible(true);
    }

}