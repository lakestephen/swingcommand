package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 * 
 * Defines the methods to be implemented by a command controlling subsystem.
 * A CommandController implementation must be thread safe - there is no guarantee which thread
 * will invoke the CommandController methods
 *
 * <p/>
 * Command Controllers
 * a) May be integrated with a UI in some manner to provide an indication of the commands currently running
 * b) May Provide error handling for exceptions raised
 * c) May Optionally impose a limit on the number of concurrent tasks by blocking on the startCommand callback
 */
public interface CommandController<E> {

    /**
     * This method is called to notify the controller when a command wishes to run
     *
     * This method may block if a limit on concurrent commands has been reached, placing the calling
     * thread in the wait state. When a slot becomes available the calling thread should be notified to continue
     *
     * Alternatively the controller may cancel the command outright by returning false
     *
     * @param commandExecution, execution for the task which wishes to execute
     * @param message, message to display on task start
     * @return true to allow the command to continue, false to prevent it running
     */
    public boolean commandStarting(E commandExecution, String message);

    /**
     * When a command has finished processing, this method is called to notify the controller
     *
     * @param commandExecution, execution for the task which wishes to execute
     * @param message - message to display on task stop
     */
    public void commandStopped(E commandExecution, String message);

    /**
     * If an error occurs during command processing, this method is called to notify the controller
     *
     * @param commandExecution, execution for the task which wishes to execute
     * @param errorMessage - message to display describing error
     * @param t - throwable describing the error
     */
    public void handleCommandError(E commandExecution, String errorMessage, Throwable t);
}
