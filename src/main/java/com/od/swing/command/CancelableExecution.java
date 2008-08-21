package com.od.swing.command;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Aug-2008
 * Time: 17:43:43
 */
public interface CancelableExecution extends CommandExecution {

    /**
     * Call this method to request the the execution be cancelled
     */
    void cancelExecution();
    
}
