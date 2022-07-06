package vector;

/**
 * This class represents a three-dimensional vector, with x, y and z components.
 * 
 * @author Lauren Knight
 */
public class Vector3D {
    public double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Converts this 3D vector to a normalized 2D vector, using
     * the vector's x and y components.
     * @return a new Vector2D containing the x and y components of the
     * normalized vector.
     */
    public Vector2D to2DDirectionVector() {
        double dx = x;
        double dy = y;
        double d = Math.sqrt(dx * dx + dy * dy);
        return new Vector2D(dx / d, dy / d);
    }

    /**
     * Calculates the pathagorean distance between two 3D vectors
     * @param v the vector to calculate with
     * @return the magnitude of the distance between this vector and v
     */
    public double distanceFrom(Vector3D v) {
        double dx = v.x - x;
        double dy = v.y - y;
        double dz = v.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

}
