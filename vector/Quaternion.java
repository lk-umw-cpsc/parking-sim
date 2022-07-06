package vector;

/**
 * This class defines a quaternion with w, x, y, and z components, which
 * are used by Motive to define the rotation of a tracked object.
 * 
 * @author Lauren Knight
 */
public class Quaternion {
    public double w, x, y, z;

    public Quaternion(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D toUpVector() {
        return new Vector3D(
            2 * (x*y - w*z),
            1 - 2 * (x * x + z * z),
            2 * (y * z + w * x)
        );
        // return new Vector3D(
        //     2 * (x*z + w*y),
        //     2 * (y*z - w*x),
        //     1 - 2 * (x*x + y*y)
        // );
    }

}
