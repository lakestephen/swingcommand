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

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Support for firing events to LifeCycleObserver listeners
 *
 * Listeners are always notified on the AWT event thread -
 * even if the fire method is called on a non-swing thread.
 *
 * This provides an easy mechanism to update the UI to represent the progress of a command
 */
class ExecutionObserverSupport {

    private final List<ExecutionObserver> executionObservers = new ArrayList<ExecutionObserver>();

    public final void addExecutionObservers(ExecutionObserver... observers) {
        synchronized (executionObservers) {
            executionObservers.addAll(Arrays.asList(observers));
        }
    }

    public final void removeExecutionObservers(ExecutionObserver... observers) {
        synchronized (executionObservers) {
            executionObservers.removeAll(Arrays.asList(observers));
        }
    }

    /**
     * @return a snapshot of the listener list
     */
    public List<ExecutionObserver> getExecutionObserverSnapshot()  {
        synchronized (executionObservers) {
            return new ArrayList<ExecutionObserver>(executionObservers);
        }
    }

    static void firePending(final List<ExecutionObserver> executionObservers, final Execution commandExecution) {
        for (final ExecutionObserver observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.pending(commandExecution);
                }
            }, true);
        }
    }

    static void fireStarted(final List<ExecutionObserver> executionObservers, final Execution commandExecution) {
        for (final ExecutionObserver observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.started(commandExecution);
                }
            }, true);
        }
    }

    static void fireDone(final List<ExecutionObserver> executionObservers, final Execution commandExecution) {
        for (final ExecutionObserver observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.done(commandExecution);
                }
            }, true);
        }
    }

    static void fireError(final List<ExecutionObserver> executionObservers, final Execution commandExecution, final Throwable t) {
        for (final ExecutionObserver observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.error(commandExecution, t);
                }
            }, true);
        }
    }

    static void fireProgress(final List<ExecutionObserver> executionObservers, final Execution commandExecution, final String description) {
        for (final ExecutionObserver observer : executionObservers) {
            executeOnEventThread(new Runnable(){
                public void run() {
                    //this synchronized block is to handle the case where the event thread might not otherwise
                    //see state changes to fields in the execution carried out in the background thread
                    //which is calling progress, due to the memory model
                    synchronized(this) {
                        observer.progress(commandExecution, description);
                    }
                }
            }, true, true);
        }
    }


    static void fireSuccess(List<ExecutionObserver> executionObservers, final Execution commandExecution) {
        for (final ExecutionObserver observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.success(commandExecution);
                }
            }, true);
        }
    }


    static Throwable executeSynchronouslyOnEventThread(Runnable task, boolean logError) {
        return executeOnEventThread(task, logError, false);
    }

    /**
     * TODO - refactor this
     *
     * Safely run on the event thread
     * If not on the event thead already does an invoke and wait
     *
     * @param task task to run
     * @param logError - whether any Exeception should be logged
     * @return an Exception - as thrown by the Runnable throws one, or null
     */
    static Throwable executeOnEventThread(Runnable task, boolean logError, boolean isAsynchronous) {
        Throwable error = null;
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                if ( isAsynchronous ) {
                    SwingUtilities.invokeLater(task);
                } else {
                    SwingUtilities.invokeAndWait(task);
                }
            }
            catch (InvocationTargetException e) {
                error = e.getTargetException() == null ? e : e.getTargetException();
            } catch (Throwable t) {
                error = t;
            }
        } else {
            try {
                task.run();
            } catch ( Throwable t) {
                error = t;
            }
        }

        if ( error != null && logError) {
            error.printStackTrace();
        }

        return error;
    }

}