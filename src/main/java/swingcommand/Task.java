package swingcommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Apr-2009
 * Time: 20:58:09
 *
 * The superclass for all Tasks which are created when a command is executed
 */
public abstract class Task<P> {

    private volatile ExecutionState executionState = ExecutionState.NOT_RUN;
    private volatile boolean cancelled;
    private volatile Throwable executionException;
    private final CopyOnWriteArrayList<TaskListener<? super P>> taskListeners = new CopyOnWriteArrayList<TaskListener<? super P>>();

    public abstract void doInEventThread() throws Exception;

    public final void cancel() {
        if ( !cancelled) {
            cancelled = true;
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

    public void setExecutionState(ExecutionState executionState) {
        this.executionState = executionState;
    }

    public ExecutionState getExecutionState() {
        return executionState;
    }

    public Throwable getExecutionException() {
        return executionException;
    }

    public void setExecutionException(Throwable executionException) {
        this.executionException = executionException;
    }

    //generally it is better to add listeners to the Command instance (they will be invoked for every task)
    //or by passing them as parameters to execute(), to listen to  a one off task.
    //I'll leave default visibility here for now, for that reason
    void addTaskListener(List<TaskListener<? super P>> listeners) {
        synchronized (taskListeners) {
            this.taskListeners.addAll(listeners);
        }
    }

    void removeTaskListener(List<TaskListener<? super P>> listeners) {
        synchronized (taskListeners) {
            this.taskListeners.removeAll(listeners);
        }
    }

    List<TaskListener<? super P>> getTaskListeners() {
        return this.taskListeners;
    }

    /**
     * Fire progress event to taskListener instances
     * Event will be received on the Swing event thread
     *
     * @param progress, objects containing a description of the progress made
     */
    protected void fireProgress(P progress) {
        TaskListenerSupport.fireProgress(taskListeners, this, progress);
    }
}
