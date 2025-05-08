package polimi.ds.dsnapshot.Connection;

/**
 * Manager of the locking phases of the application.
 * The application is locked (i.e. cannot send messages) when either at least a node has crashed or a snapshot is being restored.
 */
public class PanicManager {
    /**
     * Boolean lock for when a node crashes.
     */
    private boolean panicModeLock=false;
    /**
     * Boolean lock for when a snapshot is being restored.
     */
    private boolean snapshotLock=false;

    /**
     * Method to check if the application is unlocked.
     * @return True if the application is unlocked.
     */
    public synchronized boolean isUnlocked(){
        return !(this.panicModeLock || this.snapshotLock);
    }

    /**
     * Method to check if the application is locked.
     * @return True if the application is locked.
     */
    public synchronized boolean isLocked(){
        return this.panicModeLock || this.snapshotLock;
    }

    /**
     * Method to manually set the panic mode when a node crashes.
     * @param panicMode Boolean indicating if the panic mode is activated or not.
     */
    public synchronized void setPanicMode(boolean panicMode){
        this.panicModeLock=panicMode;
    }

    /**
     * Method to manually set the locking mode when a snapshot is started/ended.
     * @param snapshotLock Boolean indicating if the snapshot lock is activated or not.
     */
    public synchronized void setSnapshotLock(boolean snapshotLock){
        this.snapshotLock=snapshotLock;
    }
}
