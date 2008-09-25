/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 30-Aug-2008
 * Time: 00:40:16
 * To change this template use File | Settings | File Templates.
 *
 * Each time a Command is executed, a CommandExecution is created.
 *
 * The CommandExecution contains the logic to run the command, and encapsulates the state/parameters used for the
 * command's execution.
 */
public interface CommandExecution {

     /**
     * Called in the Swing event thread to execute the command
     *
     * For asynchronous commands this gives the command a chance to update the
     * Swing UI views & models once asynchronous processing is completed
     *
     * @throws Exception, to abort execution if an error condition occurs
     */
    void doInEventThread() throws Exception;

    /**
     * @param executionState, the new state
     */
    void setState(ExecutionState executionState);

    /**
     * @return the execution state
     */
    ExecutionState getState();

    /**
     * @return ExecutionExeception, if the execution ended in ExecutionState.ERROR, or null
     */
    Throwable getExecutionException();

    /**
     * @param t, Throwable which caused the execution to fail
     */
    void setExecutionException(Throwable t);
}
