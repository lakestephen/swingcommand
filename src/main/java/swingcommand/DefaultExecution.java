package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Sep-2008
 * Time: 14:52:16
 *
 * Supplied default implementation for Undoable and Cancellable
 */
public class DefaultExecution implements AsynchronousExecution {

    public void cancelExecution() {
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isCancellable() {
        return false;
    }

    public void undo() {
    }

    public boolean isUndoable() {
        return false;
    }

    public boolean isUndone() {
        return false;
    }

    public void doInBackground() throws Exception {
    }

    public void doInEventThread() throws Exception {
    }
}
