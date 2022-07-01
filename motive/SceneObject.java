package motive;

import vector.Vector3D;

public class SceneObject {
    private Vector3D location;
    private Quaternion rotation;

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

}
