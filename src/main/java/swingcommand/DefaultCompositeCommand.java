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
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 29-Aug-2008
 * Time: 22:57:01
 * To change this template use File | Settings | File Templates.
 *
 * A simple composite command which can be used out of the box, or subclassed
 * This composite command executes its child commands sequentially, in the order they were added
 *
 * The composite command supplies an executor when it executes each of the child commands so
 * that the child commands run on the composite's own execution thread - ie. any executors
 * configured on the child commands directly are ignored while executing as part of a composite
 *
 * If a child command fails by throwing an Exception, the parent command will also fail, and
 * propagate an exception to its ExecutionObservers with the child exception as the cause,
 * before any subsequent commands are executed.
 *
 * If a cancellable child command's execution is cancelled, the parent will also be cancelled, before any
 * subsequent commands are executed
 *
 * If the composite execution itself is cancelled, the current child command execution will be cancelled
 * if it supports cancellation, and no subsequent child commands will be executed
 *
 * @param <C> The type of CommandExecution this composite command's child commands will use
 */
public class DefaultCompositeCommand<C extends CommandExecution> extends AbstractCompositeCommand<SequentialExecution<C>, C> {

    public DefaultCompositeCommand(Command<C>... childCommands) {
        super(childCommands);
    }

    public DefaultCompositeCommand(Executor executor, Command<C>... childCommands) {
        super(executor, childCommands);
    }

    /**
     * Subclasses may override this method, to return a DefaultCompositeExecution with extra child commands added, for example
     *
     * @return a CompositeExecution
     */
    public SequentialExecution<C> createExecution() {
        return new AbstractCompositeExecution<C>(getChildCommands()) {

            protected void fireProgress(String s) {
                DefaultCompositeCommand.this.fireProgress(this, s);
            }
        };
    }

}
