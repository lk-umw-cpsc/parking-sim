package motive;

/**
 * A listener interface that allows listening for movement 
 * of a rigid body within Motive
 * 
 * The listener's rigidBodyUpdateReceived method will be called
 * once for each rigid body tracked by Motive each time a frame
 * is received from Motive (this occurs 30-60 FPS by default).
 * 
 * @author Lauren Knight
 */
public interface RigidBodyUpdateListener {
    void rigidBodyUpdateReceived(int id, float x, float y, float z,
            float qw, float qx, float qy, float qz);
}
