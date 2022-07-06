import java.awt.Point;

import vector.Quaternion;
import vector.Vector3D;

/**
 * This class defines a motion-tracked object, including a
 * location (Vector3D) and rotation (Quaternion).
 * 
 * The class includes a method to translate the 3D space coordinate
 * to a screen coordinate.
 * 
 * @author Lauren Knight
 */
public class SceneObject {
    private Vector3D location;
    private Quaternion rotation;

    public SceneObject() {
        location = new Vector3D(0, 0, 0);
        rotation = new Quaternion(1, 0, 0, 0);
    }

    /**
     * Updates the object's location
     * @param x the new x component
     * @param y the new y component
     * @param z the new z component
     */
    public void moveTo(double x, double y, double z) {
        location.x = x;
        location.y = y;
        location.z = z;
    }

    /**
     * Updates the object's rotation
     * @param w the new w component
     * @param x the new x component
     * @param y the new y component
     * @param z the new z component
     */
    public void rotateTo(double w, double x, double y, double z) {
        rotation.w = w;   
        rotation.x = x;
        rotation.y = y;
        rotation.z = z;
    }

    /**
     * Accesses this object's location
     * @return the Vector3D with the object's x, y and z coordinates
     */
    public Vector3D getLocation() {
        return location;
    }

    /**
     * Accesses this object's rotation
     * @return the Quaternion with the object's rotational w, x, y, and z.
     */
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * Translates this object's 3D location to a screen location
     * @param roomXLowerBound the x coordinate that should line up with the lower-left corner of the screen
     * @param roomYLowerBound the y coordinate that should line up with the lower-left corner of the screen
     * @param roomWidth the width of the 3D space
     * @param roomLength the length of the 3D space
     * @param screenWidth the width of the screen/canvas
     * @param screenHeight the height of the screen/canvas
     * @return a Point containing the x and y coordinate of the translation
     */
    public Point getScreenLocation(double roomXLowerBound, double roomYLowerBound,
            double roomWidth, double roomLength, int screenWidth, int screenHeight) {
        Point p = new Point();
        p.x = (int) ((location.x - roomXLowerBound) / roomWidth * screenWidth);
        p.y = (int) -((location.y + roomYLowerBound) / roomLength * screenHeight);
        return p;
    }

}
