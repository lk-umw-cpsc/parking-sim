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

/**
 * This class defines the behavior of the animated panel for the game
 * where the car, score, etc. appear.
 * 
 * The game "loop" is defined within this class.
 * 
 * The animation of the panel is driven by the frameUpdateReceived method,
 * which is called by the CommandStreamManager created within this class's
 * constructor.
 * 
 * @author Lauren Knight
 */
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

    private static final int ID_PLAYER_CAR = 0;
    private static final int ID_ALIGNMENT_TOOL = 1;

    private static final int TIME_PER_ROUND = 3 * 60 * 1000; // 3 minutes

    private double roomXLowerBound = DEFAULT_ROOM_X_LOWER_LIMIT;
    private double roomYLowerBound = DEFAULT_ROOM_Y_LOWER_LIMIT;
    private double roomYLowerBoundGoal;
    private double roomWidth = DEFAULT_ROOM_WIDTH;
    private double roomLength = DEFAULT_ROOM_LENGTH;

    private SceneObject alignmentTool;
    private SceneObject playerCar;

    private Vector3D alignmentToolInitialPosition;
    private Vector3D playerCarInitialPosition;
    
    private double[] initialRotationsRadians = new double[2];
    private double[] rotationsRadians = new double[2];

    private Vector2D alignmentVector;
    private SceneObject goal;

    private final Random rng = new Random();

    private boolean playing;
    private int score;
    private int highscore;
    private long roundOverTime;

    private SceneObject[] sceneObjects = new SceneObject[2];

    private BufferedImage carImage;

    private double rotationOffsetRadians;

    /**
     * Initializes the ApplicationCanvas
     */
    public ApplicationCanvas() {
        // set size of the canvas
        setPreferredSize(new Dimension(CANVAS_WIDTH_HEIGHT, CANVAS_WIDTH_HEIGHT));

        alignmentTool = new SceneObject();
        playerCar = new SceneObject();
        goal = new SceneObject();

        try {
            carImage = squarifyImage(ImageIO.read(new File("images/car-yellow.png")));
        } catch (IOException e) {
            System.out.println("Unable to load car images!");
            e.printStackTrace();
        }

        sceneObjects[ID_PLAYER_CAR] = playerCar;
        sceneObjects[ID_ALIGNMENT_TOOL] = alignmentTool;

        // begin listening for updates from Motive
        CommandStreamManager streamManager = new CommandStreamManager();
        streamManager.addRigidBodyUpdateListener(this);
        streamManager.addFrameUpdateListener(this);
        new Thread(streamManager).start();

        setFocusable(true);
        requestFocus();

        addKeyListener(this);
    }

    /**
     * Expands the dimensions of an image without altering the original
     * image, making the width and height equal and centering the original
     * image in the middle of the new image.
     * @param img The image to 'squarify'
     * @return A new BufferedImage containing the expanded image
     */
    private BufferedImage squarifyImage(BufferedImage img) {
        int newWidthHeight = Math.max(img.getWidth(), img.getHeight());
        BufferedImage newImg = new BufferedImage(newWidthHeight, newWidthHeight, BufferedImage.TYPE_4BYTE_ABGR);
        newImg.getGraphics().drawImage(img, 0, newWidthHeight / 4, null);
        return newImg;
    }

    // Colors for the paint method
    private static final Color BACKGROUND_COLOR = new Color(51, 51, 51);
    private static final Color GOAL_COLOR = Color.GREEN;
    private static final Color TEXT_COLOR = Color.WHITE;

    /**
     * This method is called each time the component needs to redraw itself.
     * This method is called once each time repaint() is called.
     */
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
        
        drawCar(g2d, width, height);

        if (playing) {
            g.setColor(GOAL_COLOR);
            Point p = goal.getScreenLocation(roomXLowerBound, roomYLowerBound,
                    roomWidth, roomLength, width, height);
            g.fillOval(p.x - 8, p.y - 8, 17, 17);
        }

        g.setColor(TEXT_COLOR);
        long timeRemaining = (roundOverTime - System.currentTimeMillis()) / 1000;
        long minutes = timeRemaining / 60;
        long seconds = timeRemaining % 60;
        if (playing) {
            g.drawString(String.format("%d:%02d", minutes, seconds), 8, 20);
            g.drawString(String.format("Score: %d", score), 8, 36);
            if (highscore > 0) {
                g.drawString(String.format("Highscore: %d", highscore), 8, 52);
            }
        } else {
            g.drawString("Press any key to play!", 240, 385);
            if (score > 0) {
                g.drawString(String.format("Previous score: %d", score), 250, 411);
            }
            if (highscore > 0) {
                g.drawString(String.format("Highscore: %d", highscore), 264, 427);
            }
        }
    }

    /**
     * Moves the goal to a new, random location
     */
    private void moveGoal() {
        Vector3D location = goal.getLocation();
        Vector3D playerLocation = playerCar.getLocation();
        do {
            location.x = (rng.nextDouble() * 0.85 + 0.075) * roomWidth + roomXLowerBound;
            location.y = (rng.nextDouble() * 0.85 + 0.075) * roomLength + roomYLowerBoundGoal;
        } while (playerLocation.distanceFrom(location) < GOAL_LOCATION_TOLERANCE);
    }

    /**
     * Draws the player's car to the screen based on its location in the scene.
     * Rotates the car image based on the rotation of the physical car.
     * @param g The canvas to draw to
     * @param width The width of the canvas
     * @param height The height of the canvas
     */
    private void drawCar(Graphics2D g, int width, int height) {
        Point p = playerCar.getScreenLocation(roomXLowerBound, roomYLowerBound,
                roomWidth, roomLength, width, height);
        BufferedImage image = carImage;
        int halfImageWidth = image.getWidth(null) / 2;
        int halfImageHeight = image.getHeight(null) / 2;

        p.x -= halfImageWidth;
        p.y -= halfImageHeight;

        double r = -(rotationOffsetRadians + rotationsRadians[ID_PLAYER_CAR] 
                - initialRotationsRadians[ID_PLAYER_CAR]);
        if (Double.isNaN(r)) {
            r = -rotationOffsetRadians;
        }

        AffineTransform tx = AffineTransform.getRotateInstance(
                r, halfImageWidth, halfImageHeight);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        // Drawing the rotated image at the required drawing locations
        g.drawImage(op.filter(image, null), p.x, p.y, null);
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
     * Initializes the round, resetting the score to 0,
     * moving the goal and setting the timer.
     */
    private void initRound() {
        score = 0;
        moveGoal();
        roundOverTime = System.currentTimeMillis() + TIME_PER_ROUND;
    }

    /**
     * Determines whether the game timer is up.
     * @return true if the timer has passed, otherwise false.
     */
    private boolean isRoundOver() {
        return System.currentTimeMillis() >= roundOverTime;
    }

    @Override
    /**
     * Method called once per frame for each rigid body being tracked
     * by Motive.
     */
    public void rigidBodyUpdateReceived(int id, float x, float y, float z,
            float qw, float qx, float qy, float qz) {
        if (id > 1) {
            return;
        }
        Quaternion quaternion = new Quaternion(qw, qx, qy, qz);
        double rotationRadians = quaternion.toUpVector().to2DDirectionVector().getTheta();
        rotationsRadians[id] = rotationRadians;
        switch (id) {
            case ID_ALIGNMENT_TOOL:
                if (alignmentToolInitialPosition == null) {
                    alignmentToolInitialPosition = new Vector3D(x, y, z);
                    initialRotationsRadians[ID_ALIGNMENT_TOOL] = rotationRadians;
                }
                break;
            case ID_PLAYER_CAR:
                if (playerCarInitialPosition == null) {
                    playerCarInitialPosition = new Vector3D(x, y, z);
                    initialRotationsRadians[ID_PLAYER_CAR] = rotationRadians;
                    roomXLowerBound = x - roomWidth / 2;
                    roomYLowerBound = -y - roomLength / 2;
                    roomYLowerBoundGoal = y - roomLength / 2;
                }
                goal.getLocation().z = z;
                
        }
        SceneObject obj = sceneObjects[id];
        obj.moveTo(x, y, z);
        obj.rotateTo(qw, qx, qy, qz);
    }

    /**
     * Method called each time a frame packet is received from Motive
     */
    @Override
    public void frameUpdateReceived() {
        if (playing && playerCar.getLocation().distanceFrom(goal.getLocation()) < GOAL_LOCATION_TOLERANCE) {
            score++;
            moveGoal();
        }
        if (alignmentVector == null && alignmentToolInitialPosition != null
                && playerCarInitialPosition != null) {
            Vector2D at2d = new Vector2D(alignmentToolInitialPosition.x, alignmentToolInitialPosition.y);
            Vector2D pc2d = new Vector2D(playerCarInitialPosition.x, playerCarInitialPosition.y);
            alignmentVector = pc2d.directionTowards(at2d);
            rotationOffsetRadians = Math.atan2(alignmentVector.y, alignmentVector.x);
        }
        if (isRoundOver()) {
            playing = false;
            if (score > highscore) {
                highscore = score;
            }
        }
        repaint();
    }

    @Override
    /**
     * Method called by Swing when a key is pressed while the
     * window has focus
     */
    public void keyPressed(KeyEvent e) {
        if (!playing) {
            initRound();
            playing = true;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

}
