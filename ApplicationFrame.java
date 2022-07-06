import javax.swing.JFrame;

/**
 * This class defines the application's main window.
 * 
 * The frame contains a single child component, an ApplicationCanvas.
 * 
 * @author Lauren Knight
 */
public class ApplicationFrame extends JFrame {

    private final ApplicationCanvas canvas;
    
    public ApplicationFrame() {
        super("Goal Chase Challenge");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add a canvas to the frame
        canvas = new ApplicationCanvas();
        add(canvas);

        // resize the frame to fit the menu bar and canvas component
        pack();
    }

}
