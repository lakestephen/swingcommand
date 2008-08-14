package com.od.swing.command;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Support for firing events to LifeCycleMonitor listeners
 *
 * Listeners are always notified on the AWT event thread -
 * even if the fire method is called on a non-swing thread.
 *
 * This provides an easy mechanism to update the UI to represent the progress of a command
 */
class LifeCycleMonitoringSupport<E> {

    private final List<LifeCycleMonitor<? super E>> lifeCycleMonitors = new ArrayList<LifeCycleMonitor<? super E>>();

    public final void addLifeCycleMonitor(LifeCycleMonitor<? super E>... monitors) {
        synchronized (lifeCycleMonitors) {
            for (LifeCycleMonitor<? super E> monitor : monitors ) {
                lifeCycleMonitors.add(monitor);
            }
        }
    }

    public final void removeLifeCycleMonitor(LifeCycleMonitor<? super E>... monitors) {
        //find the proxy for the real interface and remove
        synchronized (lifeCycleMonitors) {
            for (LifeCycleMonitor<? super E> monitor : monitors ) {
                lifeCycleMonitors.remove(monitor);
            }
        }
    }

    /**
     * @return a snapshot of the listener list
     */
    public List<LifeCycleMonitor<? super E>> getLifeCycleMonitorSnapshot()  {
        synchronized (lifeCycleMonitors) {
            return new ArrayList<LifeCycleMonitor<? super E>>(lifeCycleMonitors);
        }
    }

    public static <E> void fireStarted(final List<LifeCycleMonitor<? super E>> lifeCycleMonitors, final String commandName, final E commandExecution) {
        for (final LifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    monitor.started(commandName, commandExecution);
                }
            }, true);
        }
    }

    public static <E> void fireEnded(final List<LifeCycleMonitor<? super E>> lifeCycleMonitors, final String commandName, final E commandExecution) {
        for (final LifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    monitor.ended(commandName, commandExecution);
                }
            }, true);
        }
    }

    public static <E> void fireError(final List<LifeCycleMonitor<? super E>> lifeCycleMonitors, final String commandName, final E commandExecution, final Throwable t) {
        for (final LifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    monitor.error(commandName, commandExecution, t);
                }
            }, true);
        }
    }

    public static <E> void fireStepReached(final List<LifeCycleMonitor<? super E>> lifeCycleMonitors, final String commandName, final E commandExecution) {
        for (final LifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    monitor.stepReached(commandName, commandExecution);
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