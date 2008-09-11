package swingcommand;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Sep-2008
 * Time: 14:52:16
 *
 * Supplied default implementation for Undoable and Cancellable executions
 */
public class DefaultExecution implements AsynchronousExecution {

    private boolean isCancelled;
    private boolean isUndone;
    private boolean isRedone;

    private Set<ExecutionAttribute> attributes = new HashSet<ExecutionAttribute>();

    public DefaultExecution(ExecutionAttribute... executionAttributes) {
        this.attributes.addAll(Arrays.asList(executionAttributes));
    }

    /**
     * Subclasses which wish to support cancel should override this method to provide the
     * implementation
     */
    protected void doCancel() {}

    /**
     * Subclasses which wish to support undo should override this method to provide
     * the implementation
     */
    protected void doUndo() {}

    /**
     * Subclasses which wish to support undo should override this method to provide
     * the implementation
     */
    protected void doRedo() {}


    public final void cancelExecution() {
        if ( isCancellable() && ! isCancelled ) {
            doCancel();
            isCancelled = true;
        }
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isCancellable() {
        return attributes.contains(ExecutionAttribute.Cancellable) && ! isCancelled;
    }

    public final void undo() {
        if ( isUndoable()) {
            doUndo();
            isUndone = true;
            isRedone = false;
        }
    }

    public boolean isUndoable() {
        return attributes.contains(ExecutionAttribute.Undoable) && ! isUndone && ! isCancelled;
    }

    public boolean isUndone() {
        return isUndone;
    }

    public final void redo() {
        if ( isRedoable()) {
            doRedo();
            isRedone = true;
            isUndone = false;
        }
    }

    public boolean isRedoable() {
        return attributes.contains(ExecutionAttribute.Redoable) && isUndone;
    }

    public boolean isRedone() {
        return isRedone;
    }

    public void doInBackground() throws Exception {
    }

    public void doInEventThread() throws Exception {
    }

}
