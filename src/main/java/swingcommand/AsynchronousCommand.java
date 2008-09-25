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

import java.util.concurrent.Executor;


/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 *  @param <E> The type of AsynchronousExecution this asynchronous command will use
 */
public interface AsynchronousCommand<E extends AsynchronousExecution> extends Command<E> {

    /**
     * @param executor executor to use for this execution only
     * @param executionObservers extra observers to be notified during this execution only
     * @return an object which represents the result of this command execution, or for asynchronous commands, the execution in progress
     */
    E execute(Executor executor, ExecutionObserver<? super E>... executionObservers);

    /**
     * @param executor the default executor to use for this Command
     */
    void setExecutor(Executor executor);
}
