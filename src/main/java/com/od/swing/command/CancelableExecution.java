package com.od.swing.command;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 22-Aug-2008
 * Time: 14:06:13
 */
public interface CancelableExecution extends CommandExecution {

    /**
     * Call this method to request execution be cancelled
     */
     void cancelExecution();
}
