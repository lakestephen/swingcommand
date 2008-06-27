package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * An adapter in the sense of Swing listener adapters - can be exteded by classes
 * which want to inherit a default implementation of the monitor methods
 */
public class LifeCycleMonitorAdapter<E> implements CommandLifeCycleMonitor<E> {

    public void started(E commandExecution, String startMessage) {
    }

    public void stepReached(E commandExecution, int currentStep, int totalStep, String stepMessage) {
    }

    public void ended(E commandExecution, String endMessage) {
    }

    public void error(E commandExecution, String errorMessage, Throwable t) {
    }
}