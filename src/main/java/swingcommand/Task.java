/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package swingcommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Apr-2009
 * Time: 20:58:09
 *
 * The superclass for Tasks which are created when a command is executed
 *
 * Tasks which do not need to run in the background should extend this class directly.
 * Tasks which have some background processing to perform in a subthread should extend BackgroundTask instead
 */
public abstract class Task<P> {

    private volatile ExecutionState executionState = ExecutionState.NOT_RUN;
    private volatile boolean cancelled;
    private volatile Throwable executionException;
    private final CopyOnWriteArrayList<TaskListener<? super P>> taskListeners = new CopyOnWriteArrayList<TaskListener<? super P>>();

    protected abstract void doInEventThread() throws Exception;

    public final void cancel() {
        if ( ! isCancelled()) {
            doCancel();
            cancelled = true;
        }
    }

    /**
     * Subclasses which wish to support cancel should override this method
     */
    protected void doCancel() {
    }

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

    public void addTaskListener(TaskListener<? super P> t) {
        synchronized (taskListeners) {
            this.taskListeners.add(t);
        }
    }

    public void addTaskListeners(List<TaskListener<? super P>> listeners) {
        synchronized (taskListeners) {
            this.taskListeners.addAll(listeners);
        }
    }

    public void removeTaskListener(TaskListener<? super P> t) {
        synchronized (taskListeners) {
            this.taskListeners.remove(t);
        }
    }

    public void removeTaskListeners(List<TaskListener<? super P>> listeners) {
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

    public static enum ExecutionState {

        NOT_RUN,
        PENDING,
        STARTED,
        SUCCESS,
        ERROR;

        public boolean isFinalState() {
            return this == SUCCESS || this == ERROR;
        }
    }
}
