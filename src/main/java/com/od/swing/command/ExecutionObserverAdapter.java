package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * An adapter in the sense of Swing listener adapters - can be exteded by classes
 * which want to inherit a default implementation of the observer methods
 */
public class ExecutionObserverAdapter<E> implements ExecutionObserver<E> {

    public void starting(E commandExecution) {}

    public void started(E commandExecution) {}

    public void progress(E commandExecution) {}

    public void stopped(E commandExecution) {}

    public void error(E commandExecution, Throwable error) {}
}