package swingcommand;

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
 */
public class DefaultCompositeCommand<C extends CommandExecution> extends AbstractCompositeCommand<CompositeExecution<C>, C> {

    private static final Executor synchronousExecutor = new Executor() {
        public void execute(Runnable command) {
           command.run();
        }
    };

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
    protected class DefaultCompositeExecution extends DefaultExecution implements CompositeExecution<C> {

        private List<Command<? extends C>> executionCommands = new ArrayList<Command<? extends C>>();
        private int totalChildCommands = executionCommands.size();
        private int currentCommandId;
        private volatile boolean isCancelled;
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
                    executeAsyncChildCommand((AsynchronousCommand)command);
                } else {
                    executeChildCommand(command);
                }

                if ( executionObserverProxy.isErrorOccurred()) {
                    throw new CompositeCommandException(executionObserverProxy.getLastCommandError());
                }

                isCancelled |= executionObserverProxy.isLastCommandCancelled();
                //abort processing if a swingcommand has generated an error via the TaskServicesProxy
                if (isCancelled) {
                    break;
                }
            }
        }

        public void doInEventThread() throws Exception {}

        @SuppressWarnings("unchecked")
        private void executeAsyncChildCommand(AsynchronousCommand command) {
            //we are not in event thread here, so this call should be synchronous
            command.execute(synchronousExecutor, executionObserverProxy);
        }

        private void executeChildCommand(final Command<? extends C> command) {
            //non async commands should only run on the event thread
            ExecutionObserverSupport.executeSynchronouslyOnEventThread(new Runnable() {
                public void run() {
                    command.execute(executionObserverProxy);
                }
            }, true);
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
            if ( c.isCancellable()) {
                c.cancelExecution();
            }
        }

        public boolean isCancelled() {
            return isCancelled;
        }

        public boolean isCancellable() {
            return true;
        }

        /**
         * Receives execution observer events from child commands and fires step reached events to
         * this composites observers
         */
        private class ExecutionObserverProxy extends ExecutionObserverAdapter<C> {
            private final DefaultCompositeExecution commandExecution;
            private volatile boolean errorOccurred;
            private volatile C currentChildExecution;
            private volatile boolean lastCommandCancelled;
            private volatile Throwable lastCommandError;

            public ExecutionObserverProxy(DefaultCompositeExecution commandExecution) {
                this.commandExecution = commandExecution;
            }

            public void started(C commandExecution) {
                this.currentChildExecution = commandExecution;
                fireProgress(this.commandExecution);
            }

            public void stopped(C commandExecution) {
                lastCommandCancelled = commandExecution.isCancelled();
            }

            public void error(C commandExecution, Throwable e) {
                errorOccurred = true;
                lastCommandError = e;
            }

            public boolean isErrorOccurred() {
                return errorOccurred;
            }

            public C getCurrentChildExecution() {
                return currentChildExecution;
            }

            public boolean isLastCommandCancelled() {
                return lastCommandCancelled;
            }

            public Throwable getLastCommandError() {
                return lastCommandError;
            }
        }
    }

    private static class CompositeCommandException extends Exception {

        private CompositeCommandException(Throwable cause) {
            super("Error while executing composite command", cause);
        }
    }
}
