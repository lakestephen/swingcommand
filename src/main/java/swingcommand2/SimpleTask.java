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
public abstract class SimpleTask {

    private volatile ExecutionState executionState = ExecutionState.NOT_RUN;
    private volatile boolean cancelled;
    private volatile boolean cancellable;
    private volatile Throwable executionException;
    private final CopyOnWriteArrayList<TaskListener> taskListeners = new CopyOnWriteArrayList<TaskListener>();

    public abstract void doInEventThread() throws Exception;

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

    protected void addTaskListener(TaskListener... o) {
        synchronized (taskListeners) {
            this.taskListeners.addAll(Arrays.asList(o));
        }
    }

    protected void removeTaskListener(TaskListener... o) {
        synchronized (taskListeners) {
            this.taskListeners.addAll(Arrays.asList(o));
        }
    }

    protected List<TaskListener> getTaskListeners() {
        return this.taskListeners;
    }

    /**
     * Fire progress event to taskListener instances
     * Event will be received on the Swing event thread
     *
     * @param description, objects containing a description of the progress made
     */
    protected void fireProgress(String description) {
        TaskListenerSupport.fireProgress(taskListeners, this, description);
    }
}
