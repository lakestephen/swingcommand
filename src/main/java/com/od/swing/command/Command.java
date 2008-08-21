package com.od.swing.command;


/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
public interface Command<E> {

    /**
     * @param lifeCycleMonitor a listener to receive lifecycle events during command processing
     */
    void addLifeCycleMonitor(LifeCycleMonitor<? super E>... lifeCycleMonitor);

    /**
     * @param lifeCycleMonitor a listener to receive lifecycle events during command processing
     */
    void removeLifeCycleMonitor(LifeCycleMonitor<? super E>... lifeCycleMonitor);

    /**
     * Run this command
     * @param lifeCycleMonitor life cycle monitor(s) which will be notified during this execution only
     * @return The execution instance for this execution of the command
     */
    E execute(LifeCycleMonitor<? super E>... lifeCycleMonitor);
}
