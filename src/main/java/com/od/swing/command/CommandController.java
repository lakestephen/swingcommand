package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 * 
 * Defines the methods to be implemented by a command controlling subsystem.
 *
 * A CommandController implementation must be thread safe - there is no guarantee which thread
 * will invoke the CommandController methods
 *
 * <p/>
 * Command Controllers
 * a) May be integrated with a UI in some manner to provide an indication of the commands currently running
 * b) May provide error handling for exceptions raised during command processing
 * c) May impose a limit on the number of concurrent commands or stop a command from running
 */
public interface CommandController<E> {

    /**
     * This method is called to notify the controller when a command wishes to run
     *
     * This method may block if a limit on concurrent commands has been reached, placing the calling
     * thread in the wait state. When a slot becomes available the calling thread should be notified to continue
     *
     * Throwing an execption here will abort the command.
     * commandError will be called with the Exception instance and commandStopped will still be called
     *
     * @param commandName name describing command
     * @param commandExecution, execution for the task which wishes to execute
     * @throws Exception, if the command cannot not be allowed to proceed.
     */
    public void commandStarting(String commandName, E commandExecution) throws Exception;

    /**
     * When a command has finished processing, this method is called to notify the controller
     * commandStopped will always be called provided commandStarting returned true
     *
     * @param commandName name describing command
     * @param commandExecution, execution for the task which wishes to execute
     */
    public void commandStopped(String commandName, E commandExecution);

    /**
     * If an error occurs during command processing, this method is called to notify the controller
     *
     * @param commandName name describing command
     * @param commandExecution, execution for the task which wishes to execute
     * @param t - throwable describing the error
     */
    public void commandError(String commandName, E commandExecution, Throwable t);
}
