package com.od.swing.command;


/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
public interface Command<E> {

    /**
     * @param executionObservers a listener to receive execution observer events during command processing
     */
    void addExecutionObservers(ExecutionObserver<? super E>... executionObservers);

    /**
     * @param executionObservers a listener to receive execution observer events during command processing
     */
    void removeExecutionObservers(ExecutionObserver<? super E>... executionObservers);

    /**
     * Run this command
     * @param executionObservers will be notified during this execution only
     * @return The execution instance for this execution of the command
     */
    E execute(ExecutionObserver<? super E>... executionObservers);
}
