package com.od.swing.command;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 27-Jun-2008
 * Time: 09:49:24
 */
interface CommandExecutor<E> {

    List<LifeCycleMonitor<? super E>> getLifeCycleMonitors();

    /**
     * @return a reference to the Thread which was used to execute the command
     */
    Thread executeCommand();
}
