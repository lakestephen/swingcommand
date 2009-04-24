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
class TaskListenerSupport {



    static void firePending(final List<TaskListener> executionObservers, final SwingTask commandExecution) {
        for (final TaskListener observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.pending(commandExecution);
                }
            });
        }
    }

    static void fireStarted(final List<TaskListener> executionObservers, final SwingTask commandExecution) {
        for (final TaskListener observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.started(commandExecution);
                }
            });
        }
    }

    static void fireDone(final List<TaskListener> executionObservers, final SwingTask commandExecution) {
        for (final TaskListener observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.done(commandExecution);
                }
            });
        }
    }

    static void fireError(final List<TaskListener> executionObservers, final SwingTask commandExecution, final Throwable t) {
        for (final TaskListener observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.error(commandExecution, t);
                }
            });
        }
    }

    static void fireProgress(final List<TaskListener> executionObservers, final SwingTask commandExecution, final String description) {
        for (final TaskListener observer : executionObservers) {
            executeOnEventThread(new Runnable(){
                public void run() {
                    //this synchronized block is to handle the case where the event thread might not otherwise
                    //see state changes to fields in the execution carried out in the background thread
                    //which is calling progress, due to the memory model
                    synchronized(this) {
                        observer.progress(commandExecution, description);
                    }
                }
            }, true);
        }
    }


    static void fireSuccess(List<TaskListener> executionObservers, final SwingTask commandExecution) {
        for (final TaskListener observer : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    observer.success(commandExecution);
                }
            });
        }
    }


    static void executeSynchronouslyOnEventThread(Runnable task) {
        executeOnEventThread(task, false);
    }

    private static void executeOnEventThread(Runnable task, boolean isAsynchronous) {
        if (!SwingUtilities.isEventDispatchThread()) {
            if ( isAsynchronous ) {
                SwingUtilities.invokeLater(task);
            } else {
                try {
                    SwingUtilities.invokeAndWait(task);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                task.run();
            } catch ( Throwable t) {
               t.printStackTrace();
            }
        }

    }

}