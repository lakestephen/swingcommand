package com.od.swing.command;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Can be used to combine child async commands so that they execute together as one unit
 * The child commands are executed in the order they are added
 *
 * All child command in the composite a provided with a proxy ExecutionObserver by the parent command
 * during processing. This allows the parent command to fire progress events on its execution observers when each child task starts
 * (e.g so a progress bar can repaint each time a child task completes).
 *
 * If a child command throws an exception, this will abort processing, and the exception will be propagated to the parents'
 * ExecutionObservers.
 *
 * Child tasks' ExecutionObserver instances will recieve events as normal as each child task is processed.
 *
 * The execution for CompositeAsyncCommand implements Cancelable
 * Cancelling the execution will cause the command to abort after the currently processing child command finished execution
 *
 * E - the type of CompositeExecution, C the type of CommmandExecution the child commands will use
 */
public abstract class AbstractCompositeCommand<E extends CompositeExecution, C extends CommandExecution> extends AbstractAsynchronousCommand<E> {

    //use synchronized list in case non-event thread adds child commands
    private List<AsynchronousCommand<C>> childCommands = Collections.synchronizedList(new ArrayList<AsynchronousCommand<C>>());

    public AbstractCompositeCommand(AsynchronousCommand<C>... childCommands) {
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    protected AbstractCompositeCommand(Executor executor, AsynchronousCommand<C>... childCommands) {
        super(executor);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public void addCommand(AsynchronousCommand<C> command) {
        childCommands.add(command);
    }

    public void addCommands(AsynchronousCommand<C>... commands) {
        childCommands.addAll(Arrays.asList(commands));
    }

    public void addCommands(Collection<AsynchronousCommand<C>> commands) {
        childCommands.addAll(commands);
    }

    public void removeCommands(AsynchronousCommand<C>... commands) {
        childCommands.removeAll(Arrays.asList(commands));
    }

    public void removeCommands(Collection<AsynchronousCommand<C>> commands) {
        childCommands.removeAll(commands);
    }

    public void removeCommand(AsynchronousCommand<C> command) {
        childCommands.remove(command);
    }

    public List<AsynchronousCommand<C>> getChildCommands() {
        return Collections.unmodifiableList(childCommands);
    }
}


