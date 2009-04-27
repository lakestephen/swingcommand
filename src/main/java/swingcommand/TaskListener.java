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
 * even if fired by an AsynchronousCommand
 *
 * These callbacks provide an easy and safe way to update the UI to show the progress of a task.
 *
 */
public interface TaskListener<P> {

    /**
     * The callback to pending is triggered by the thread which calls command.execute()
     * If the Executor associated with the task queues the task or blocks waiting for a thread, it may be some time
     * before started() is invoked.
     *
     * @param task, the task which is pending
     */
    void pending(Task task);

    /**
     * Called when the task starts processing.For BackgroundTask this callback takes place just before
     * doInBackground is called. For SimpleTask just before doInEventThread
     *
     * @param task, the task which has started
     */
    void started(Task task);

    /**
     * This callback may take place at any time during task execution, to indicate progress
     *
     * @param task, the task which has made progress
     * @param progress, an object describing the progress made
     */
    void progress(Task task, P progress);

    /**
     * This callback takes place once a command has successfully completed.
     * If an exception is generated during the doInBackground or doInEventThread methods, this callback will not occur.
     * In this case, a callback to error() will occur instead
     *
     * @param task, the task which has been successful
     */
    void success(Task task);

    /**
     * This callback takes place if an exception is raised during task execution, which prevents
     * successful completion. If this callback occurs, the callback to success will not occur.
     *
     * @param task, the task for which an error occurred
     * @param error, the error which occurred
     */
    void error(Task task, Throwable error);

    /**
     * This callback always takes place once the task has finished, whether or not the task executed successfully or
     * generated errors during doInBackground or doInEventThread methods
     *
     * @param task, the task which has stopped
     */
    void finished(Task task);
}