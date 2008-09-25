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
 * Date: 29-Aug-2008
 * Time: 23:06:47
 * To change this template use File | Settings | File Templates.
 *
 * A reusable command, which can be observed by CommandObserver instances.
 *
 * Each time the command is executed, a CommandExecution is created, which contains the logic to process the command and
 * enacapsulates the parameters/state
 *
 *  @param <E> The type of Execution this command will use
 */
public interface Command<E extends CommandExecution> {

    /**
     * @param executionObservers a listener to receive execution observer events during command processing
     */
    void addExecutionObserver(ExecutionObserver<? super E>... executionObservers);

    /**
     * @param executionObservers a listener to receive execution observer events during command processing
     */
    void removeExecutionObserver(ExecutionObserver<? super E>... executionObservers);

    /**
     * @param executionObservers extra observers to be notified during this execution only
     * @return an object which represents the result of this command execution, or for asynchronous commands, the execution in progress
     */
    E execute(ExecutionObserver<? super E>... executionObservers);
}
