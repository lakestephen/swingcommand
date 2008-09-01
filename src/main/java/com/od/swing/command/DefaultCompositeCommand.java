package com.od.swing.command;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 29-Aug-2008
 * Time: 22:57:01
 * To change this template use File | Settings | File Templates.
 *
 * A simple composite command which can be used out of the box, or subclassed
 */
public class DefaultCompositeCommand<C extends CommandExecution> extends AbstractCompositeCommand<CompositeExecution<C>, C> {

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
    public CompositeExecution<C> createExecution() {
        return new DefaultCompositeExecution();
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
    protected class DefaultCompositeExecution implements CompositeExecution<C> {

        private List<Command<? extends C>> executionCommands = new ArrayList<Command<? extends C>>();
        private int totalChildCommands = executionCommands.size();
        private int currentCommandId;
        private volatile boolean isCancelled;
        private boolean abortOnError;
        private ExecutionObserverProxy executionObserverProxy = new ExecutionObserverProxy(this);

        public DefaultCompositeExecution() {
            executionCommands.addAll(getChildCommands());
        }

        /**
         * Execute the child commands here off the swing thread.
         * This will not start a new subthread - the child commands will run synchronously in the parent command's execution thread.
         *
         * @throws Exception
         */
        public void doInBackground() throws Exception {
            currentCommandId = 0;
            for (final Command<? extends C> command : executionCommands) {
                currentCommandId++;

                if ( command instanceof AsynchronousCommand ) {
                    executeAsyncChildCommand(command);
                } else {
                    executeChildCommand(command);
                }

                //abort processing if a command has generated an error via the TaskServicesProxy
                if ((executionObserverProxy.isErrorOccurred() && abortOnError) || isCancelled) {
                    break;
                }
            }
        }

        private void executeAsyncChildCommand(Command<? extends C> command) {
            //we are not in event thread here, so this call should be synchronous
            command.execute(executionObserverProxy);
        }

        private void executeChildCommand(final Command<? extends C> command) {
            //non async commands should only run on the event thread
            ExecutionObserverSupport.executeSynchronouslyOnEventThread(new Runnable() {
                public void run() {
                    command.execute(executionObserverProxy);
                }
            }, true);
        }

        /**
         * @param abortOnError - whether an error in a command causes the remaining commands to be aborted.
         */
        public void setAbortOnError(boolean abortOnError) {
            this.abortOnError = abortOnError;
        }

        public void addCommand(Command<? extends C> command) {
            executionCommands.add(command);
        }

        public void addCommands(Command<? extends C>... commands) {
            executionCommands.addAll(Arrays.asList(commands));
        }

        public void addCommands(Collection<Command<? extends C>> commands) {
            executionCommands.addAll(commands);
        }

        public C getCurrentChildExecution() {
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
            C c = executionObserverProxy.getCurrentChildExecution();
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
        private class ExecutionObserverProxy implements ExecutionObserver<C> {
            private final DefaultCompositeExecution commandExecution;
            private volatile boolean errorOccurred;
            private volatile C currentChildExecution;

            public ExecutionObserverProxy(DefaultCompositeExecution commandExecution) {
                this.commandExecution = commandExecution;
            }

            public void starting(C commandExecution) {
            }

            public void started(C commandExecution) {
                this.currentChildExecution = commandExecution;
                fireProgress(this.commandExecution);
            }

            public void progress(C commandExecution) {
            }

            public void stopped(C commandExecution) {
            }

            public void error(C commandExecution, Throwable e) {
                errorOccurred = true;
                fireError(this.commandExecution, e);
            }

            public boolean isErrorOccurred() {
                return errorOccurred;
            }

            public C getCurrentChildExecution() {
                return currentChildExecution;
            }
        }

    }
}
