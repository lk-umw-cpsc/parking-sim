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
    private static final double ROOM_X_LOWER_LIMIT = -0.5;
    private static final double ROOM_Y_LOWER_LIMIT = -0.5;

    // the default room length and width
    private static final double ROOM_LENGTH = 1.0;
    private static final double ROOM_WIDTH = 1.0;

    private static final double LOCATION_TOLERANCE = 0.05;
    private static final double GOAL_LOCATION_TOLERANCE = 0.02;
    private static final double BUMP_TOLERANCE = 0.005;
    private static final double GOAL_ROTATION_TOLERANCE = 7;

    private static final int FRONT_CAR = 0;
    private static final int BACK_CAR = 1;
    private static final int PLAYER_CAR = 2;

    private double roomXLowerBound = ROOM_X_LOWER_LIMIT;
    private double roomYLowerBound = ROOM_Y_LOWER_LIMIT;
    private double roomWidth = ROOM_WIDTH;
    private double roomLength = ROOM_LENGTH;

    private SceneObject frontCar;
    private SceneObject backCar;
    private SceneObject playerCar;

    private Vector3D frontCarInitialPosition;
    private Vector3D backCarInitialPosition;
    private Vector3D playerCarGoalPosition;

    private Quaternion frontCarInitialRotation;
    private Quaternion backCarInitialRotation;
    private Quaternion playerCarGoalRotation;

    private double[] initialRotations = new double[3];
    private double[] rotations = new double[3];

    private Vector2D laneDirection;

    private boolean playing;

    private SceneObject[] sceneObjects = new SceneObject[3];

    private BufferedImage[] carImages = new BufferedImage[3];

    private double rot;

    public ApplicationCanvas() {
        // set size of the canvas
        setPreferredSize(new Dimension(CANVAS_WIDTH_HEIGHT, CANVAS_WIDTH_HEIGHT));

        frontCar = new SceneObject();
        backCar = new SceneObject();
        playerCar = new SceneObject();

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

        sceneObjects[FRONT_CAR] = frontCar;
        sceneObjects[BACK_CAR] = backCar;
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
        
        for (int car = 0; car < 3; car++) {
            drawCar(g2d, width, height, car);
        }
    }

    private void drawCar(Graphics2D g, int width, int height, int car) {
        Point p = sceneObjects[car].getScreenLocation(roomXLowerBound, roomYLowerBound,
                roomWidth, roomLength, width, height);
        BufferedImage image = carImages[car];
        int halfImageWidth = image.getWidth(null) / 2;
        int halfImageHeight = image.getHeight(null) / 2;

        // halfImageWidth = 64;
        // halfImageHeight = 64;

        p.x -= halfImageWidth;
        p.y -= halfImageHeight;

        double r = -(rot + rotations[car] - initialRotations[car]);
        if (Double.isNaN(r)) {
            r = -rot;
        }

        System.out.printf("%d: %.2f\n", car, r);
        AffineTransform tx = AffineTransform.getRotateInstance(
                r, 
                halfImageWidth, halfImageHeight);
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
        Quaternion quaternion = new Quaternion(qw, qx, qy, qz);
        double rotation = quaternion.toForwardVector().to2DDirectionVector().getTheta();
        rotations[id] = rotation;
        switch (id) {
            case FRONT_CAR:
                if (frontCarInitialPosition == null) {
                    frontCarInitialPosition = new Vector3D(x, y, z);
                    frontCarInitialRotation = quaternion;
                    initialRotations[FRONT_CAR] = rotation;
                }
                break;
            case BACK_CAR:
                if (backCarInitialPosition == null) {
                    backCarInitialPosition = new Vector3D(x, y, z);
                    backCarInitialRotation = quaternion;
                    initialRotations[BACK_CAR] = rotation;
                }
                break;
            case PLAYER_CAR:
                if (playerCarGoalPosition == null) {
                    playerCarGoalPosition = new Vector3D(x, y, z);
                    playerCarGoalRotation = quaternion;
                    initialRotations[PLAYER_CAR] = rotation;
                }
        }
        SceneObject obj = sceneObjects[id];
        obj.moveTo(x, y, z);
        obj.rotateTo(qw, qx, qy, qz);
    }

    @Override
    public void frameUpdateReceived() {
        if (frontCar.getLocation().distanceFrom(frontCarInitialPosition) > BUMP_TOLERANCE) {
            System.out.println("Game over! Front car bumped!");
        }
        if (backCar.getLocation().distanceFrom(backCarInitialPosition) > BUMP_TOLERANCE) {
            System.out.println("Game over! Rear car bumped!");
            Vector3D v = backCar.getLocation();
            System.out.printf("%.2f %.2f %.2f vs %.2f %.2f %.2f\n",
                    v.x, v.y, v.z, backCarInitialPosition.x, backCarInitialPosition.y, backCarInitialPosition.z);
        }
        if (playing && playerCar.getLocation().distanceFrom(playerCarGoalPosition) < GOAL_LOCATION_TOLERANCE
                && Math.abs(rotations[PLAYER_CAR] - initialRotations[PLAYER_CAR]) < GOAL_ROTATION_TOLERANCE) {
            System.out.println("You win!");
        }
        if (laneDirection == null && frontCarInitialPosition != null
                && playerCarGoalPosition != null) {
            Vector2D fc2d = new Vector2D(frontCarInitialPosition.x, frontCarInitialPosition.y);
            Vector2D pc2d = new Vector2D(playerCarGoalPosition.x, playerCarGoalPosition.y);
            laneDirection = pc2d.directionTowards(fc2d);
            rot = Math.atan2(laneDirection.y, laneDirection.x);
        }
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        playing = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
        
    }

}
