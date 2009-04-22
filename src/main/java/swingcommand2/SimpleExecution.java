package swingcommand2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Apr-2009
 * Time: 20:58:09
 * To change this template use File | Settings | File Templates.
 */
public abstract class SimpleExecution implements Execution {

    private volatile ExecutionState executionState = ExecutionState.PENDING;
    private volatile boolean cancelled;
    private volatile boolean cancellable;
    private volatile Throwable executionException;
    private final CopyOnWriteArrayList<ExecutionObserver> executionObservers = new CopyOnWriteArrayList<ExecutionObserver>();

    public abstract void doInEventThread();

    public final void cancel() {
        if ( !cancelled) {
            cancelled = true;
            cancellable = false;
            doCancel();
        }
    }

    /**
     * Subclasses which wish to support cancel should override this method
     */
    protected void doCancel() {}

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public void setState(ExecutionState executionState) {
        this.executionState = executionState;
    }

    public ExecutionState getState() {
        return executionState;
    }

    public ExecutionState getExecutionState() {
        return executionState;
    }

    public void setExecutionState(ExecutionState executionState) {
        this.executionState = executionState;
    }

    public Throwable getExecutionException() {
        return executionException;
    }

    public void setExecutionException(Throwable executionException) {
        this.executionException = executionException;
    }

    public void addExecutionObserver(ExecutionObserver... o) {
        synchronized (executionObservers) {
            this.executionObservers.addAll(Arrays.asList(o));
        }
    }

    public void removeExecutionObserver(ExecutionObserver... o) {
        synchronized (executionObservers) {
            this.executionObservers.addAll(Arrays.asList(o));
        }
    }

    public List<ExecutionObserver> getExecutionObservers() {
        return this.executionObservers;
    }

    /**
     * Fire progress event to ExecutionObserver instances
     * Event will be received on the Swing event thread
     *
     * @param description, objects containing a description of the progress made
     */
    protected void fireProgress(String description) {
        ExecutionObserverSupport.fireProgress(executionObservers, this, description);
    }
}
