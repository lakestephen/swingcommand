package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * An adapter in the sense of Swing listener adapters - can be exteded by classes
 * which want to inherit a default implementation of the monitor methods
 */
public class LifeCycleMonitorAdapter<E> implements LifeCycleMonitor<E>
{

    public void started(String commandName, E commandExecution) {
    }

    public void stepReached(String commandName, E commandExecution) {
    }

    public void ended(String commandName, E commandExecution) {
    }

    public void error(String commandName, E commandExecution, Throwable error) {
    }
}