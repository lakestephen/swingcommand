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

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Implement this interface to listen to the progress of a task
 * Callbacks on this interface are guaranteed to be received on the AWT event thread,
 * even if fired by an BackgroundTask
 *
 * These callbacks provide an easy and safe way to update the UI to show the progress of a task.
 *
 * <P> - the type of Progress object that this TaskListener will receive
 */
public interface TaskListener<P> {

    /**
     * The callback to pending is triggered when command.execute() is called
     * If the Executor running the task queues the task or blocks waiting for
     * a thread, it may be some time before started() is invoked.
     */
    void pending(Task task);

    /**
     * Called when the task starts processing. For BackgroundTask this callback
     * takes place just before doInBackground is called. For SimpleTask just before doInEventThread
     */
    void started(Task task);

    /**
     * This callback may take place at any time during task execution, to indicate progress
     */
    void progress(Task task, P progress);

    /**
     * Called when the task reaches the SUCCESS end state
     * This callback takes place once a command has successfully completed, was not cancelled, and did not
     * throw an Exception resulting in a callback to error()
     */
    void success(Task task);

    /**
     * Called when the task reaches the ERROR end state
     * This callback takes place if an exception is raised during task execution, which prevents
     * successful completion, and the task was not cancelled.
     */
    void error(Task task, Throwable error);

    /**
     * Called when the task reaches the CANCELLED end state
     * This callback takes place if the task is cancelled.
     * @param task
     */
    void cancelled(Task task);

    /**
     * This callback always takes place once the task has finished, whether or not the task finishes in ERROR,
     * SUCCESS or CANCELLED states
     */
    void finished(Task task);
}