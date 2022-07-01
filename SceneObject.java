import java.awt.Point;

import motive.Quaternion;
import vector.Vector3D;

public class SceneObject {
    private Vector3D location;
    private Quaternion rotation;

    public SceneObject() {
        location = new Vector3D(0, 0, 0);
        rotation = new Quaternion(0, 0, 0, 1);
    }

    public void moveTo(double x, double y, double z) {
        location.x = x;
        location.y = y;
        location.z = z;
    }

    public void rotateTo(double a, double b, double c, double d) {
        rotation.a = a;
        rotation.b = b;
        rotation.c = c;
        rotation.d = d;   
    }

    public Vector3D getLocation() {
        return location;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public Point getScreenLocation(double roomXLowerBound, double roomYLowerBound,
            double roomWidth, double roomLength, int screenWidth, int screenHeight) {
        Point p = new Point();
        p.x = (int) ((location.x - roomXLowerBound) / roomWidth * screenWidth);
        p.y = (int) -((location.y + roomYLowerBound) / roomLength * screenHeight);
        return p;
    }

}
