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

    private final List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors = new ArrayList<CommandLifeCycleMonitor<? super E>>();

    public final void addLifeCycleMonitor(CommandLifeCycleMonitor<? super E> lifeCycleMonitor) {
        synchronized (lifeCycleMonitors) {
            lifeCycleMonitors.add(lifeCycleMonitor);
        }
    }

    public final void removeLifeCycleMonitor(CommandLifeCycleMonitor<? super E> lifeCycleMonitor) {
        //find the proxy for the real interface and remove
        synchronized (lifeCycleMonitors) {
            lifeCycleMonitors.remove(lifeCycleMonitor);
        }
    }

    /**
     * @return a snapshot of the listener list
     */
    public List<CommandLifeCycleMonitor<? super E>> getLifeCycleMonitorSnapshot()  {
        synchronized (lifeCycleMonitors) {
            return new ArrayList<CommandLifeCycleMonitor<? super E>>(lifeCycleMonitors);
        }
    }

    public static <E> void fireStarted(final List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors, final E commandExecution, final String startMessage) {
        executeSynchronouslyOnEventThread(new Runnable(){
            public void run() {
                for (CommandLifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
                    monitor.started(commandExecution, startMessage);
                }
            }
        }, true);
    }

    public static <E> void fireEnded(final List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors, final E commandExecution, final String endMessage) {
        executeSynchronouslyOnEventThread(new Runnable(){
            public void run() {
                for (CommandLifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
                    monitor.ended(commandExecution, endMessage);
                }
            }
        }, true);
    }

    public static <E> void fireError(final List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors, final E commandExecution, final String errorMessage, final Throwable t) {
        executeSynchronouslyOnEventThread(new Runnable(){
            public void run() {
                for (CommandLifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
                    monitor.error(commandExecution, errorMessage, t);
                }
            }
        }, true);
    }

    public static <E> void fireStepReached(final List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors, final E commandExecution, final int currentStep, final int totalStep, final String stepMessage) {
        executeSynchronouslyOnEventThread(new Runnable(){
            public void run() {
                for (CommandLifeCycleMonitor<? super E> monitor : lifeCycleMonitors) {
                    monitor.stepReached(commandExecution, currentStep, totalStep, stepMessage);
                }
            }
        }, true);

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