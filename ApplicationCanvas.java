import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import motive.CommandStreamManager;
import motive.FrameUpdateListener;
import motive.RigidBodyUpdateListener;
import vector.Quaternion;
import vector.Vector2D;
import vector.Vector3D;

public class ApplicationCanvas extends JPanel implements RigidBodyUpdateListener,
        FrameUpdateListener, KeyListener {
    
    // the width and height of the canvas, in pixels
    private static final int CANVAS_WIDTH_HEIGHT = 600;

    // the default room lower X and Y limit
    private static final double DEFAULT_ROOM_X_LOWER_LIMIT = -0.5;
    private static final double DEFAULT_ROOM_Y_LOWER_LIMIT = -0.5;

    // the default room length and width
    private static final double DEFAULT_ROOM_LENGTH = 1.0;
    private static final double DEFAULT_ROOM_WIDTH = 1.0;

    private static final double GOAL_LOCATION_TOLERANCE = 0.105;

    private static final int PLAYER_CAR = 0;
    private static final int ALIGNMENT_TOOL = 1;

    private double roomXLowerBound = DEFAULT_ROOM_X_LOWER_LIMIT;
    private double roomYLowerBound = DEFAULT_ROOM_Y_LOWER_LIMIT;
    private double roomWidth = DEFAULT_ROOM_WIDTH;
    private double roomLength = DEFAULT_ROOM_LENGTH;

    private SceneObject alignmentTool;
    private SceneObject playerCar;

    private Vector3D alignmentToolInitialPosition;
    private Vector3D playerCarInitialPosition;
    
    private Quaternion playerCarInitialRotation;

    private double[] initialRotationsRadians = new double[2];
    private double[] rotations = new double[2];

    private Vector2D alignmentVector;
    private SceneObject goal;

    private boolean playing;

    private SceneObject[] sceneObjects = new SceneObject[2];

    private BufferedImage[] carImages = new BufferedImage[3];

    private double rotationOffsetRadians;

    public ApplicationCanvas() {
        // set size of the canvas
        setPreferredSize(new Dimension(CANVAS_WIDTH_HEIGHT, CANVAS_WIDTH_HEIGHT));

        alignmentTool = new SceneObject();
        playerCar = new SceneObject();
        goal = new SceneObject();

        try {
            carImages[0] = ImageIO.read(new File("images/car-green.png"));
            carImages[1] = ImageIO.read(new File("images/car-yellow.png"));
            carImages[2] = ImageIO.read(new File("images/car-purple.png"));
        } catch (IOException e) {
            System.out.println("Unable to load car images!");
            e.printStackTrace();
        }

        for (int i = 0; i < carImages.length; i++) {
            carImages[i] = squarifyImage(carImages[i]);
        }

        sceneObjects[ALIGNMENT_TOOL] = alignmentTool;
        sceneObjects[PLAYER_CAR] = playerCar;

        // begin listening for updates from Motive
        CommandStreamManager streamManager = new CommandStreamManager();
        streamManager.addRigidBodyUpdateListener(this);
        streamManager.addFrameUpdateListener(this);
        new Thread(streamManager).start();
        setFocusable(true);
        requestFocus();
        addKeyListener(this);
    }

    private BufferedImage squarifyImage(BufferedImage img) {
        int newWidthHeight = Math.max(img.getWidth(), img.getHeight());
        BufferedImage newImg = new BufferedImage(newWidthHeight, newWidthHeight, BufferedImage.TYPE_4BYTE_ABGR);
        newImg.getGraphics().drawImage(img, 0, newWidthHeight / 4, null);
        return newImg;
    }

    // colors for the dots drawn to the screen
    private static final Color BACKGROUND_COLOR = new Color(51, 51, 51);

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        // turn on shape anti-aliasing (reduces jagged pixels)
        setRenderingHints(g);
        final int width = getWidth();
        final int height = getHeight();

        // draw over the previous frame with the background color
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
        
        drawCar(g2d, width, height, PLAYER_CAR);
        g.setColor(Color.GREEN);
        Point p = goal.getScreenLocation(roomXLowerBound, roomYLowerBound, roomWidth, roomLength, width, height);
        g.drawOval(p.x - 8, p.y - 8, 17, 17);
    }

    private final Random rng = new Random();

    private void moveGoal() {
        Vector3D location = goal.getLocation();
        location.x = rng.nextDouble() * roomWidth + roomXLowerBound;
        location.y = rng.nextDouble() * roomLength + roomYLowerBound;
    }

    private void drawCar(Graphics2D g, int width, int height, int car) {
        Point p = sceneObjects[car].getScreenLocation(roomXLowerBound, roomYLowerBound,
                roomWidth, roomLength, width, height);
        BufferedImage image = carImages[1];
        int halfImageWidth = image.getWidth(null) / 2;
        int halfImageHeight = image.getHeight(null) / 2;

        p.x -= halfImageWidth;
        p.y -= halfImageHeight;

        double r = -(rotationOffsetRadians + rotations[car] - initialRotationsRadians[car]);
        if (Double.isNaN(r)) {
            r = -rotationOffsetRadians;
        }
        // if (car == PLAYER_CAR) {
        //     System.out.printf("%.2f vs %.2f\n", rotations[car], initialRotations[car]);
        // }

        AffineTransform tx = AffineTransform.getRotateInstance(
                r, halfImageWidth, halfImageHeight);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        // BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        // op.filter(image, tmp);

        // Drawing the rotated image at the required drawing locations
        g.drawImage(op.filter(image, null), p.x, p.y, null);

        // g.drawImage(tmp, p.x, p.y, null);
    }

    /**
     * Tweaks rendering hints for the scene
     * @param graphics The Graphics object to set rendering hints on
     */
    private static void setRenderingHints(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        // Enable anti-aliasing for shapes
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

    @Override
    public void rigidBodyUpdateReceived(int id, float x, float y, float z,
            float qw, float qx, float qy, float qz) {
        if (id > 1) {
            return;
        }
        Quaternion quaternion = new Quaternion(qw, qx, qy, qz);
        double rotationRadians = quaternion.toForwardVector().to2DDirectionVector().getTheta();
        rotations[id] = rotationRadians;
        switch (id) {
            case ALIGNMENT_TOOL:
                if (alignmentToolInitialPosition == null) {
                    alignmentToolInitialPosition = new Vector3D(x, y, z);
                    initialRotationsRadians[ALIGNMENT_TOOL] = rotationRadians;
                }
                break;
            case PLAYER_CAR:
                if (playerCarInitialPosition == null) {
                    playerCarInitialPosition = new Vector3D(x, y, z);
                    initialRotationsRadians[PLAYER_CAR] = rotationRadians;
                    roomXLowerBound = x - roomWidth / 2;
                    roomYLowerBound = y - roomLength / 2;
                }
                goal.getLocation().z = z;
                
        }
        SceneObject obj = sceneObjects[id];
        obj.moveTo(x, y, z);
        obj.rotateTo(qw, qx, qy, qz);
    }

    @Override
    public void frameUpdateReceived() {
        if (playing && playerCar.getLocation().distanceFrom(goal.getLocation()) < GOAL_LOCATION_TOLERANCE) {
            moveGoal();
        }
        if (alignmentVector == null && alignmentToolInitialPosition != null
                && playerCarInitialPosition != null) {
            Vector2D it2d = new Vector2D(alignmentToolInitialPosition.x, alignmentToolInitialPosition.y);
            Vector2D pc2d = new Vector2D(playerCarInitialPosition.x, playerCarInitialPosition.y);
            alignmentVector = pc2d.directionTowards(it2d);
            rotationOffsetRadians = Math.atan2(alignmentVector.y, alignmentVector.x);
        }
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        moveGoal();
        playing = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        
    }

}
