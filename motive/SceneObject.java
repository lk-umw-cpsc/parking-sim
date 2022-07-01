package motive;

import vector.Vector3D;

public class SceneObject {
    private Vector3D location;

    public void moveTo(double x, double y, double z) {
        location.x = x;
        location.y = y;
        location.z = z;
    }

    public Vector3D getLocation() {
        return location;
    }

}
