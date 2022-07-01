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
    private static final double ROOM_X_LOWER_LIMIT = -1.0;
    private static final double ROOM_Y_LOWER_LIMIT = -1.0;

    // the default room length and width
    private static final double ROOM_LENGTH = 2.0;
    private static final double ROOM_WIDTH = 2.0;

    private static final double LOCATION_TOLERANCE = 0.1;
    private static final double ROTATION_TOLERANCE = 0.03;

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

        sceneObjects[0] = frontCar;
        sceneObjects[1] = backCar;
        sceneObjects[2] = playerCar;

        rotations[0] = 0.79;

        frontCar.moveTo(0.5, 0.5, 0);
        backCar.moveTo(-0.5, -0.5, 0);

        Vector2D fc2d = new Vector2D(frontCar.getLocation().x, frontCar.getLocation().y);
        Vector2D pc2d = new Vector2D(playerCar.getLocation().x, playerCar.getLocation().y);

        laneDirection = pc2d.directionTowards(fc2d);
        // atan2 converts unit vector to radians
        rot = Math.atan2(laneDirection.y, laneDirection.x);
        // rot = Math.PI / 2;
        System.out.printf("%.2f %.2f; %.2f", laneDirection.x, laneDirection.y, rot);

        // begin listening for updates from Motive
        CommandStreamManager streamManager = new CommandStreamManager();
        streamManager.addRigidBodyUpdateListener(this);
        new Thread(streamManager).start();
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
            drawCar(g2d, width, height, carImages[car], sceneObjects[car]);
        }
    }

    private void drawCar(Graphics2D g, int width, int height, BufferedImage image, SceneObject car) {
        Point p = car.getScreenLocation(roomXLowerBound, roomYLowerBound,
                roomWidth, roomLength, width, height);
        int halfImageWidth = image.getWidth(null) / 2;
        int halfImageHeight = image.getHeight(null) / 2;

        p.x -= halfImageWidth;
        p.y -= halfImageHeight;

        AffineTransform tx = AffineTransform.getRotateInstance(-(rot + rotations[0] - initialRotations[0]), halfImageWidth, halfImageHeight);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        // Drawing the rotated image at the required drawing locations
        g.drawImage(op.filter(image, null), p.x, p.y, null);

        // g.drawImage(image, p.x, p.y, null);
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
            float qx, float qy, float qz, float qw) {
        Quaternion quaternion = new Quaternion(qx, qy, qz, qw);
        double rotation = quaternion.toForwardVector().to2DDirectionVector().getTheta();
        rotations[id] = rotation;
        switch (id) {
            case FRONT_CAR:
                if (frontCarInitialPosition == null) {
                    frontCarInitialPosition = new Vector3D(x, y, z);
                    frontCarInitialRotation = quaternion;
                    initialRotations[0] = rotation;
                }
                break;
            case BACK_CAR:
                if (backCarInitialPosition == null) {
                    backCarInitialPosition = new Vector3D(x, y, z);
                    backCarInitialRotation = quaternion;
                    initialRotations[1] = rotation;
                }
                break;
            case PLAYER_CAR:
                if (playerCarGoalPosition == null) {
                    playerCarGoalPosition = new Vector3D(x, y, z);
                    playerCarGoalRotation = quaternion;
                    initialRotations[2] = rotation;
                }
        }
        SceneObject obj = sceneObjects[id];
        obj.moveTo(x, y, z);
        obj.rotateTo(qx, qy, qz, qw);
    }

    @Override
    public void frameUpdateReceived() {
        if (frontCar.getLocation().distanceFrom(frontCarInitialPosition) > LOCATION_TOLERANCE) {
            System.out.println("Game over! Front car bumped!");
        }
        if (backCar.getLocation().distanceFrom(backCarInitialPosition) > LOCATION_TOLERANCE) {
            System.out.println("Game over! Rear car bumped!");
        }
        if (playing && playerCar.getLocation().distanceFrom(playerCarGoalPosition) < LOCATION_TOLERANCE
                && Math.abs(rotations[PLAYER_CAR] - initialRotations[PLAYER_CAR]) < 0.087) {
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
