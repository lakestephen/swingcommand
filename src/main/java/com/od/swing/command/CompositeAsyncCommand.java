package com.od.swing.command;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Can be used to combine child async commands so that they execute together as one unit
 * The child commands are executed in the order they are added
 *
 * All child command in the composite a provided with a proxy LifeCycleMonitor by the parent command
 * during processing. This allows the parent command to fire step reached events on its life cycle monitors when each child task starts
 * (e.g so a progress bar can repaint each time a child task completes).
 *
 * If a child command throws an exception, this will abort processing, and the exception will be propagated to the parents'
 * LifeCycleMonitor. 
 *
 * Child tasks' LifeCycleMonitor instances will recieve events as normal as each child task is processed.
 *
 * The execution for CompositeAsyncCommand implements Cancelable
 * Cancelling the execution will cause the command to abort after the currently processing child command finished execution
 */
public abstract class CompositeAsyncCommand<E extends CompositeExecution> extends AbstractAsynchronousCommand<CompositeExecution> {

    //use synchronized list in case non-event thread adds child commands
    private List<Command<CommandExecution>> childCommands = Collections.synchronizedList(new ArrayList<Command<CommandExecution>>(3));

    public CompositeAsyncCommand(String name, Command<CommandExecution>... childCommands) {
        super(name);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    protected CompositeAsyncCommand(String name, CommandController<? super CompositeExecution> commandController, Command<CommandExecution>... childCommands) {
        super(name, commandController);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    protected CompositeAsyncCommand(String name, Executor executor, Command<CommandExecution>... childCommands) {
        super(name, executor);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    protected CompositeAsyncCommand(String name, Executor executor, CommandController<? super CompositeExecution> commandController, Command<CommandExecution>... childCommands) {
        super(name, executor, commandController);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    protected CompositeAsyncCommand(String name, Executor executor, CommandController<? super CompositeExecution> commandController, boolean isRunSynchronously, Command<CommandExecution>... childCommands) {
        super(name, executor, commandController, isRunSynchronously);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public void addCommand(Command<CommandExecution> command) {
        childCommands.add(command);
    }

    public void addCommands(Command<CommandExecution>... commands) {
        childCommands.addAll(Arrays.asList(commands));
    }

    public void addCommands(Collection<Command<CommandExecution>> commands) {
        childCommands.addAll(commands);
    }

    public void removeCommands(Command<CommandExecution>... commands) {
        childCommands.removeAll(Arrays.asList(commands));
    }

    public void removeCommands(Collection<Command<CommandExecution>> commands) {
        childCommands.removeAll(commands);
    }

    public void removeCommand(Command<CommandExecution> command) {
        childCommands.remove(command);
    }

    /**
     * DefaultCompositeExecution provides the logic to execute the set of child commands added to the parent composite command
     * When it is constructed it takes a copy of the commands child commands to use for this execution. Changes to the parent command's list
     * of child commands which are made during asynchronous execution should therefore not have any side effects.
     *
     * By default the execution runs with the list of child commands configured on the owning CompositeCommand.
     * Subclass executions can add extra child commands, or modify the command list to be used for this execution only - sometimes it is
     * convenient to decide on the required child commands only at the point the composite execution is created.
     */
    protected abstract class DefaultCompositeExecution implements CompositeExecution {

        private List<Command<CommandExecution>> executionCommands = new ArrayList<Command<CommandExecution>>(3);
        private int totalChildCommands = executionCommands.size();
        private int currentCommandId;
        private volatile Command currentCommand;
        private volatile boolean isCancelled;
        private boolean abortOnError;
        private LifeCycleMonitorProxy lifeCycleProxy = new LifeCycleMonitorProxy(this);

        public DefaultCompositeExecution() {
            executionCommands.addAll(childCommands);
        }

        /**
         * Execute the child commands here off the swing thread.
         * This will not start a new subthread - the child commands will run synchronously in the parent command's execution thread.
         *
         * @throws Exception
         */
        public void doExecuteAsync() throws Exception {
            currentCommandId = 0;
            for (Command<CommandExecution> command : executionCommands) {
                currentCommand = command;
                currentCommandId++;
                command.execute(lifeCycleProxy);  //we are not in event thread here, so this should be synchronous

                //abort processing if a command has generated an error via the TaskServicesProxy
                if ((lifeCycleProxy.isErrorOccurred() && abortOnError) || isCancelled) {
                    break;
                }
            }
        }

         /**
         * @param abortOnError - whether an error in a command causes the remaining commands to be aborted.
         */
        public void setAbortOnError(boolean abortOnError) {
            this.abortOnError = abortOnError;
        }

        public void addCommand(Command<CommandExecution> command) {
            executionCommands.add(command);
        }

        public void addCommands(Command<CommandExecution>... commands) {
            executionCommands.addAll(Arrays.asList(commands));
        }

        public void addCommands(Collection<Command<CommandExecution>> commands) {
            executionCommands.addAll(commands);
        }

        public String getCurrentCommandDescription() {
            return currentCommand.toString();
        }

        public int getCurrentCommand() {
            return currentCommandId;
        }

        public int getTotalCommands() {
            return totalChildCommands;
        }

        public void cancelExecution() {
            isCancelled = true;
            CommandExecution c = lifeCycleProxy.getCurrentChildExecution();
            if ( c instanceof CancelableExecution ) {
                ((CancelableExecution)c).cancelExecution();
            }
        }

        public void doAfterExecute() throws Exception {
        }


        /**
         * Receives lifecycle events from child commands and fires step reached events to
         * this composites lifecycle listeners
         */
        private class LifeCycleMonitorProxy implements LifeCycleMonitor<CommandExecution>  {
            private boolean errorOccurred;
            private CompositeExecution commandExecution;
            private volatile CommandExecution currentChildExecution;

            public LifeCycleMonitorProxy(CompositeExecution commandExecution) {
                this.commandExecution = commandExecution;
            }

            public void started(String commandName, CommandExecution commandExecution) {
                this.currentChildExecution = commandExecution;
                fireStepReached(commandName, this.commandExecution);
            }

            public void stepReached(String commandName, CommandExecution commandExecution) {
            }

            public void ended(String commandName, CommandExecution commandExecution) {
            }

            public void error(String commandName, CommandExecution commandExecution, Throwable e) {
                errorOccurred = true;
                fireError(commandName, this.commandExecution, e);
            }

            public boolean isErrorOccurred() {
                return errorOccurred;
            }

            public CommandExecution getCurrentChildExecution() {
                return currentChildExecution;
            }
        }

    }
}


