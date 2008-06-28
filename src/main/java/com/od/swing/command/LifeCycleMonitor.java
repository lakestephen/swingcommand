package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Implement this interface to listen to the progress of a Command
 * Callbacks on this interface are guaranteed to be received on the AWT event thread, even if fired by an AsynchronousCommand
 *
 * These callbacks provide an easy and safe way to update the UI to show the progress of a task.
 *
 */
public interface LifeCycleMonitor<E> {

    void started(String commandName, E commandExecution);


    void stepReached(String commandName, E commandExecution);


    void ended(String commandName, E commandExecution);


    void error(String commandName, E commandExecution, Throwable error);
}
