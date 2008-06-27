package com.od.swing.command;


/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
public interface Command<E> {

    /**
     * @param lifeCycleMonitor a listener to receive lifecycle events during command processing
     */
    void addLifeCycleMonitor(CommandLifeCycleMonitor<? super E>... lifeCycleMonitor);

    /**
     * @param lifeCycleMonitor a listener to receive lifecycle events during command processing
     */
    void removeLifeCycleMonitor(CommandLifeCycleMonitor<? super E>... lifeCycleMonitor);

    /**
     * Run this command
     * @param lifeCycleMonitor life cycle monitor which will be notified during this execution only
     */
    void execute(CommandLifeCycleMonitor<? super E>... lifeCycleMonitor);
}
