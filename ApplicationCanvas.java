import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import motive.CommandStreamManager;
import motive.FrameUpdateListener;
import motive.RigidBodyUpdateListener;
import motive.SceneObject;

public class ApplicationCanvas extends JPanel implements RigidBodyUpdateListener, FrameUpdateListener {
    
    // the width and height of the canvas, in pixels
    private static final int CANVAS_WIDTH_HEIGHT = 600;

    // the default room lower X and Y limit
    private static final double ROOM_X_LOWER_LIMIT = -1.0;
    private static final double ROOM_Y_LOWER_LIMIT = -1.0;

    // the default room length and width
    private static final double ROOM_LENGTH = 2.0;
    private static final double ROOM_WIDTH = 2.0;

    private static final double TOLERANCE = 0.1;

    private double roomXLowerBound = ROOM_X_LOWER_LIMIT;
    private double roomYLowerBound = ROOM_Y_LOWER_LIMIT;
    private double roomWidth = ROOM_WIDTH;
    private double roomLength = ROOM_LENGTH;

    private SceneObject frontCar;
    private SceneObject backCar;
    private SceneObject playerCar;

    private SceneObject[] sceneObjects = new SceneObject[3];

    public ApplicationCanvas() {
        // set size of the canvas
        setPreferredSize(new Dimension(CANVAS_WIDTH_HEIGHT, CANVAS_WIDTH_HEIGHT));

        sceneObjects[0] = frontCar;
        sceneObjects[1] = backCar;
        sceneObjects[2] = playerCar;

        // begin listening for updates from Motive
        CommandStreamManager streamManager = new CommandStreamManager();
        streamManager.addRigidBodyUpdateListener(this);
        new Thread(streamManager).start();
    }

    // colors for the dots drawn to the screen
    private static final Color BACKGROUND_COLOR = new Color(51, 51, 51);
    private static final Color PLAYER_DOT_COLOR = new Color(227, 0, 170);
    private static final Color PICKUP_DOT_COLOR = new Color(154, 189, 0);

    // radius of the dots drawn to screen, in pixels
    private static final int PLAYER_DOT_RADIUS = 15;
    private static final int PICKUP_DOT_RADIUS = 15;

    @Override
    public void paint(Graphics g) {
        // turn on shape anti-aliasing (reduces jagged pixels)
        setRenderingHints(g);
        final int width = getWidth();
        final int height = getHeight();

        // draw over the previous frame with the background color
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
        
        // draw each dot
    }

    /**
     * Tweaks rendering hints for the scene
     * @param graphics The Graphics object to set rendering hints on
     */
    private static void setRenderingHints(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;
        // Enable anti-aliasing for shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /**
     * Converts a 3d coordinate's X coordinate to a screen coordinate
     * @param x the X coordinate to translate
     * @return a translated X coordinate
     */
    private int coordinate3dToScreenCoordinateX(double x) {
        final int canvasWidth = getWidth();
        return (int) ((x - roomXLowerBound) / roomWidth * canvasWidth);
    }

    /**
     * Converts a 3d coordinate's Y coordinate to a screen coordinate
     * @param y the Y coordinate to translate
     * @return a translated Y coordinate
     */
    private int coordinate3dToScreenCoordinateY(double y) {
        final int canvasHeight = getHeight();
        return (int) -((y + roomYLowerBound) / roomLength * canvasHeight);
    }

    /**
     * Updates the room dimensions used by the application and then
     * redraws the scene.
     * @param xLowerBound the left-most X coordinate that should correlate to the left of the screen
     * @param yLowerBound the bottom-most Y coordinate that should correlate to the bottom of the screen
     * @param width the width of the room
     * @param length the height of the room
     */
    public void setRoomDimensions(double xLowerBound, double yLowerBound, double width, double length) {
        roomXLowerBound = xLowerBound;
        roomYLowerBound = yLowerBound;
        roomWidth = width;
        roomLength = length;
        repaint();
    }

    /**
     * Method called by motive when the RC vehicle's location is updated
     */
    @Override
    public void rigidBodyUpdateReceived(int id, float x, float y, float z,
            float a, float b, float c, float d) {
        SceneObject obj = sceneObjects[id];
        obj.moveTo(x, y, z);
        obj.rotateTo(a, b, c, d);
    }

    @Override
    public void frameUpdateReceived() {
        repaint();
    }

}
