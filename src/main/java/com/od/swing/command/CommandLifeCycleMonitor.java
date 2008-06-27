package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Implement this interface to listen to the progress of a Command
 * Callbacks on this interface are guaranteed to be received on the AWT event thread, even if fired by an AsynchronousCommand
 * These callbacks provide an easy and safe way to update the GUI to show the progress of a task.
 */
public interface CommandLifeCycleMonitor<E> {

    void started(E commandExecution, String startMessage);


    void stepReached(E commandExecution, int currentStep, int totalSteps, String stepMessage);


    void ended(E commandExecution, String endMessage);


    void error(E commandExecution, String errorMessage, Throwable t);
}
