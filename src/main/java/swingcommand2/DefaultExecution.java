/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand2;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Sep-2008
 * Time: 14:52:16
 *
 * Default implementation for AsynchronousExecution
 */
public class DefaultExecution implements AsynchronousExecution {

    private volatile ExecutionState executionState = ExecutionState.PENDING;
    private volatile boolean cancelled;
    private volatile boolean cancellable;
    private volatile Throwable executionException;
    private final CopyOnWriteArrayList<ExecutionObserver> executionObservers = new CopyOnWriteArrayList<ExecutionObserver>();

    public void doInBackground() throws Exception {
    }

    public void doInEventThread() throws Exception {
    }

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