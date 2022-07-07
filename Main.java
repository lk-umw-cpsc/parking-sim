import javax.swing.SwingUtilities;

/**
 * Program entry point class.
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
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}