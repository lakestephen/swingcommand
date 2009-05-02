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

import javax.swing.*;
import java.util.List;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Support for firing events to TaskListeners
 * Listeners are always notified on the AWT event thread - even if the fire method is called on a non-swing thread.
 */
class TaskListenerSupport {

    //pending is fired on the event thread using invoke later to avoid blocking a background thread which calls execute()
    //on the swing event queue (this can have very bad performance effects on busy background threads)
    static <P,E> void firePending(final List<TaskListener<? super E>> executionObservers, final Task<P,E> task) {
        for (final TaskListener<? super E> listener : executionObservers) {
            executeAsynchronouslyIfBackgroundThread(new Runnable(){
                public void run() {
                    listener.pending(task);
                }
            });
        }
    }

    static <P,E> void fireStarted(final List<TaskListener<? super E>> executionObservers, final Task<P,E> task) {
        for (final TaskListener<? super E> listener : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    listener.started(task);
                }
            });
        }
    }

    static <P,E> void fireFinished(final List<TaskListener<? super E>> executionObservers, final Task<P,E> task) {
        for (final TaskListener<? super E> listener : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    listener.finished(task);
                }
            });
        }
    }

    static <P,E> void fireError(final List<TaskListener<? super E>> executionObservers, final Task<P,E> task, final Throwable t) {
        for (final TaskListener<? super E> listener : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    listener.error(task, t);
                }
            });
        }
    }

    static <P,E> void fireProgress(final List<TaskListener<? super E>> executionObservers, final Task<P,E> task, final E progress) {
        for (final TaskListener<? super E> listener : executionObservers) {
            executeAsynchronouslyIfBackgroundThread(new Runnable(){
                public void run() {
                    //this synchronized block is to handle the case where the event thread might not otherwise
                    //see state changes to fields in the execution carried out in the background thread
                    //which is calling progress, due to the memory model
                    synchronized(this) {
                        listener.progress(task, progress);
                    }
                }
            });
        }
    }


    static <P,E> void fireSuccess(List<TaskListener<? super E>> executionObservers, final Task<P,E> task) {
        for (final TaskListener<? super E> listener : executionObservers) {
            executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    listener.success(task);
                }
            });
        }
    }

    static void executeSynchronouslyOnEventThread(Runnable task) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            try {
                task.run();
            } catch ( Throwable t) {
               t.printStackTrace();
            }
        }

    }

    static void executeAsynchronouslyIfBackgroundThread(Runnable task) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(task);
        } else {
            try {
                task.run();
            } catch ( Throwable t) {
               t.printStackTrace();
            }
        }

    }

}