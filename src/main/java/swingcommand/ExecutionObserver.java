package swingcommand;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Implement this interface to listen to the progress of a command execution
 * Callbacks on this interface are guaranteed to be received on the AWT event thread,
 * even if fired by an AsynchronousCommand
 *
 * These callbacks provide an easy and safe way to update the UI to show the progress of a task.
 * 
 */
public interface ExecutionObserver<E> {

    /**
     * This callback takes place when the execution is created in response to Command.execute(),
     * but before the asynchronous execution takes place in the case of an AsynchronousCommand
     *
     * @param commandExecution, the execution which is starting
     */
    void starting(E commandExecution);

    /**
     * For asynchronous commands this callback takes place when the asynchronous part of the execution has started,
     * but before doInBackground is called.
     *
     * There may be some delay between starting() and started(), depending upon the Executor in use.
     * For example, if the Executor blocks, while waiting to obtain a thread from a fixed size thread pool, then
     * there will be a delay until a new thread becomes available
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
     * This callback takes place  once a command has successfully completed.
     * If an exception is generated during processing, a callback to error will occur instead
     *
     * @param commandExecution, the execution which has made progress
     */
    void success(E commandExecution);

    /**
     * This callback takes place if an exeception is raised during command execution, which prevents
     * successful completion
     *
     * @param commandExecution, the execution for which an error occurred
     * @param error error which occurred
     */
    void error(E commandExecution, Throwable error);

    /**
     * This callback takes place once the execution has finished, wheher or not the command executed successfully
     *
     * @param commandExecution, the execution which has stopped
     */
    void stopped(E commandExecution);
}