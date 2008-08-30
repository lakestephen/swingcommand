package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Implement this interface to listen to the progress of a execution
 * Callbacks on this interface are guaranteed to be received on the AWT event thread, even if fired by an AsynchronousCommand
 *
 * These callbacks provide an easy and safe way to update the UI to show the progress of a task.
 *
 */
public interface ExecutionObserver<E> {

    /**
     * This callback takes place when the execution is created in response to Command.execute(), but before the asynchronous
     * part of the execution takes place.
     *
     * This callback is made before Executor.execute() is called, so at this point the asynchronous part of the execution
     * has not yet been started. It will start at some point in the future, depending upon the implementation of the Executor
     * in use. If the Executor blocks, for example while waiting to obtain a thread from a fixed size thread pool, then
     * it may be some time before started() is called
     *
     * @param commandExecution, the execution which is starting
     */
    void starting(E commandExecution);

    /**
     * This callback takes place when the asynchronous part of the execution has started, but before
     * the doInBackground is called
     *
     * @param commandExecution, the execution which has started
     */
    void started(E commandExecution);

    /**
     * This callback may take place at any time during execution, to indicate progress
     * The execution may optionally implement an interface which provides methods to make available
     * more details of the progress
     *
     * @param commandExecution, the execution which has made progress
     */
    void progress(E commandExecution);

    /**
     * This callback takes place once the execution has finished (the doInBackground and Done have been completed)
     *
     * n.b. if doInBackground raised an exeception, done will not have been invoked, and there will have been a
     * callback to executionObserver.error() before the stopped callback takes place
     *
     * @param commandExecution, the execution which has ended
     */
    void ended(E commandExecution);

    /**
     * This callback takes place if an exeception is raised during doInBackground or done
     *
     * @param commandExecution, the execution for which an error occurred
     * @param error error which occurred
     */
    void error(E commandExecution, Throwable error);
}
