package motive;

/**
 * A listener interface that allows listening for
 * frame updates from Motive.
 * 
 * This should be paired with RigidBodyUpdateListener if
 * the programmer wants to know when a frame has finished
 * being processed
 * 
 * @author Lauren Knight
 */
public interface FrameUpdateListener {
    void frameUpdateReceived();
}
