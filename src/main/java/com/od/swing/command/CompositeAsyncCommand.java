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
 */
public abstract class CompositeAsyncCommand<E extends CompositeExecution> extends AbstractAsynchronousCommand<CompositeExecution> {

    //use synchronized list in case non-event thread adds child commands
    private List<Command<? extends CommandExecution>> childCommands = Collections.synchronizedList(new ArrayList<Command<? extends CommandExecution>>(3));

    public CompositeAsyncCommand(Command<CommandExecution>... childCommands) {
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    protected CompositeAsyncCommand(Executor executor, Command<CommandExecution>... childCommands) {
        super(executor);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public void addCommand(Command<? extends CommandExecution> command) {
        childCommands.add(command);
    }

    public void addCommands(Command<? extends CommandExecution>... commands) {
        childCommands.addAll(Arrays.asList(commands));
    }

    public void addCommands(Collection<Command<? extends CommandExecution>> commands) {
        childCommands.addAll(commands);
    }

    public void removeCommands(Command<? extends CommandExecution>... commands) {
        childCommands.removeAll(Arrays.asList(commands));
    }

    public void removeCommands(Collection<Command<? extends CommandExecution>> commands) {
        childCommands.removeAll(commands);
    }

    public void removeCommand(Command<? extends CommandExecution> command) {
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

        private List<Command<? extends CommandExecution>> executionCommands = new ArrayList<Command<? extends CommandExecution>>(3);
        private int totalChildCommands = executionCommands.size();
        private int currentCommandId;
        private volatile Command currentCommand;
        private volatile boolean isCancelled;
        private boolean abortOnError;
        private ExecutionObserverProxy executionObserverProxy = new ExecutionObserverProxy(this);

        public DefaultCompositeExecution() {
            executionCommands.addAll(childCommands);
        }

        /**
         * Execute the child commands here off the swing thread.
         * This will not start a new subthread - the child commands will run synchronously in the parent command's execution thread.
         *
         * @throws Exception
         */
        public void doInBackground() throws Exception {
            currentCommandId = 0;
            for (Command<? extends CommandExecution> command : executionCommands) {
                currentCommand = command;
                currentCommandId++;
                command.execute(executionObserverProxy);  //we are not in event thread here, so this should be synchronous

                //abort processing if a command has generated an error via the TaskServicesProxy
                if ((executionObserverProxy.isErrorOccurred() && abortOnError) || isCancelled) {
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

        public void addCommand(Command<? extends CommandExecution> command) {
            executionCommands.add(command);
        }

        public void addCommands(Command<? extends CommandExecution>... commands) {
            executionCommands.addAll(Arrays.asList(commands));
        }

        public void addCommands(Collection<Command<? extends CommandExecution>> commands) {
            executionCommands.addAll(commands);
        }

        public CommandExecution getCurrentChildExecution() {
            return executionObserverProxy.getCurrentChildExecution();
        }

        public int getCurrentChildId() {
            return currentCommandId;
        }

        public int getTotalChildren() {
            return totalChildCommands;
        }

        public void cancelExecution() {
            isCancelled = true;
            CommandExecution c = executionObserverProxy.getCurrentChildExecution();
            if ( c instanceof CancelableExecution ) {
                ((CancelableExecution)c).cancelExecution();
            }
        }

        public void done() throws Exception {
        }


        /**
         * Receives execution observer events from child commands and fires step reached events to
         * this composites observers
         */
        private class ExecutionObserverProxy implements ExecutionObserver<CommandExecution> {
            private boolean errorOccurred;
            private CompositeExecution commandExecution;
            private volatile CommandExecution currentChildExecution;

            public ExecutionObserverProxy(CompositeExecution commandExecution) {
                this.commandExecution = commandExecution;
            }

            public void starting(CommandExecution commandExecution) {
            }

            public void started(CommandExecution commandExecution) {
                this.currentChildExecution = commandExecution;
                fireStepReached(this.commandExecution);
            }

            public void stepReached(CommandExecution commandExecution) {
            }

            public void ended(CommandExecution commandExecution) {
            }

            public void error(CommandExecution commandExecution, Throwable e) {
                errorOccurred = true;
                fireError(this.commandExecution, e);
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


