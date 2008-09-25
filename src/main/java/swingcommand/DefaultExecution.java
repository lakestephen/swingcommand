/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

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
 * Default implementation for AsynchronousExecution
 */
public class DefaultExecution implements AsynchronousExecution {

    private ExecutionState executionState = ExecutionState.PENDING;
    private boolean isCancelled;
    private boolean isCancellable;
    private Throwable executionException;

    public void doInBackground() throws Exception {
    }

    public void doInEventThread() throws Exception {
    }

    public final void cancel() {
        if ( isCancellable() && ! isCancelled ) {
            doCancel();
            isCancelled = true;
        }
    }

    /**
     * Subclasses which wish to support cancel should override this method
     */
    protected void doCancel() {}

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isCancellable() {
        return isCancellable && ! isCancelled;
    }

    public void setCancellable(boolean cancellable) {
        isCancellable = cancellable;
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
}
