package com.od.swing.command;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
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
class ExecutionObserverSupport<E> {

    private final List<ExecutionObserver<? super E>> executionObservers = new ArrayList<ExecutionObserver<? super E>>();

    public final void addExecutionObservers(ExecutionObserver<? super E>... observers) {
        synchronized (executionObservers) {
            for (ExecutionObserver<? super E> observer : observers ) {
                executionObservers.add(observer);
            }
        }
    }

    public final void removeExecutionObservers(ExecutionObserver<? super E>... observers) {
        //find the proxy for the real interface and remove
        synchronized (executionObservers) {
            for (ExecutionObserver<? super E> observer : observers ) {
                executionObservers.remove(observer);
            }
        }
    }

    /**
     * @return a snapshot of the listener list
     */
    public List<ExecutionObserver<? super E>> getExecutionObserverSnapshot()  {
        synchronized (executionObservers) {
            return new ArrayList<ExecutionObserver<? super E>>(executionObservers);
        }
    }

    public static <E> void fireStarting(final List<ExecutionObserver<? super E>> executionObservers, final E commandExecution) {
        for (final ExecutionObserver<? super E> observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.starting(commandExecution);
                }
            }, true);
        }
    }

    public static <E> void fireStarted(final List<ExecutionObserver<? super E>> executionObservers, final E commandExecution) {
        for (final ExecutionObserver<? super E> observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.started(commandExecution);
                }
            }, true);
        }
    }

    public static <E> void fireEnded(final List<ExecutionObserver<? super E>> executionObservers, final E commandExecution) {
        for (final ExecutionObserver<? super E> observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.ended(commandExecution);
                }
            }, true);
        }
    }

    public static <E> void fireError(final List<ExecutionObserver<? super E>> executionObservers, final E commandExecution, final Throwable t) {
        for (final ExecutionObserver<? super E> observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.error(commandExecution, t);
                }
            }, true);
        }
    }

    public static <E> void fireStepReached(final List<ExecutionObserver<? super E>> executionObservers, final E commandExecution) {
        for (final ExecutionObserver<? super E> observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.stepReached(commandExecution);
                }
            }, true);
        }
    }


    /**
     * Safely run on the event thread
     * If not on the event thead already does an invoke and wait
     *
     * @param task task to run
     * @param logError - whether any Exeception should be logged
     * @return an Exception - as thrown by the Runnable throws one, or null
     */
    public static Throwable executeSynchronouslyOnEventThread(Runnable task, boolean logError) {
        Throwable error = null;
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(task);
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