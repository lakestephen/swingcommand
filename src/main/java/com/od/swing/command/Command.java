package com.od.swing.command;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 29-Aug-2008
 * Time: 23:06:47
 * To change this template use File | Settings | File Templates.
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
     * @param executionObservers extra observers to be notified during this execution only
     * @return an object which represents the result of this command execution, or for asynchronous commands, the execution in progress
     */
    E execute(ExecutionObserver<? super E>... executionObservers);
}
