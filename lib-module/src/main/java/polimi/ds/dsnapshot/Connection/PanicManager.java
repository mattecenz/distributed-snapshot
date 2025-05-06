package polimi.ds.dsnapshot.Connection;

public class PanicManager {
    private boolean panicModeLock=false;
    private boolean snapshotLock=false;

    public synchronized boolean isUnlocked(){
        return !(this.panicModeLock || this.snapshotLock);
    }

    public synchronized boolean isLocked(){
        return this.panicModeLock || this.snapshotLock;
    }

    public synchronized void setPanicMode(boolean panicMode){
        this.panicModeLock=panicMode;
    }

    public synchronized void setSnapshotLock(boolean snapshotLock){
        this.snapshotLock=snapshotLock;
    }
}
