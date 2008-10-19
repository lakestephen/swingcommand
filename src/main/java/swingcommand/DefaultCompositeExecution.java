package swingcommand;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * DefaultCompositeExecution provides the logic to execute the set of child commands added to the parent composite command
 * When it is constructed it takes a copy of the commands child commands to use for this execution. Changes to the parent command's list
 * of child commands which are made during asynchronous execution should therefore not have any side effects.
 *
 * By default the execution runs with the list of child commands configured on the owning CompositeCommand.
 * Subclass executions can add extra child commands, or modify the command list to be used for this execution only - sometimes it is
 * convenient to decide on the required child commands only at the point the composite execution is created.
 */
public class DefaultCompositeExecution<E extends CompositeExecution, C extends CommandExecution> extends DefaultExecution implements SequentialExecution<C> {

    private static final Executor synchronousExecutor = new Executor() {
        public void execute(Runnable command) {
           command.run();
        }
    };

    private List<Command<? extends C>> executionCommands = new ArrayList<Command<? extends C>>();
    private int totalChildCommands = executionCommands.size();
    private int currentCommandId;
    private ExecutionObserverProxy executionObserverProxy = new ExecutionObserverProxy((E)this);
    private AbstractCompositeCommand<E, C> compositeCommand;

    public DefaultCompositeExecution(AbstractCompositeCommand<E,C> compositeCommand) {
        this.compositeCommand = compositeCommand;
        setCancellable(true);
        executionCommands.addAll(compositeCommand.getChildCommands());
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

            if ( executionObserverProxy.isLastCommandCancelled() ) {
                cancel();
            }

            //abort processing if a swingcommand has generated an error via the TaskServicesProxy
            if (isCancelled()) {
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

    public C getCurrentExecution() {
        return executionObserverProxy.getCurrentChildExecution();
    }

    public int getCompletedCommandCount() {
        return currentCommandId;
    }

    public int getTotalCommands() {
        return totalChildCommands;
    }

    public List<Command<? extends C>> getChildCommands() {
        return Collections.unmodifiableList(executionCommands);
    }

    public void doCancel() {
        C c = executionObserverProxy.getCurrentChildExecution();
        if ( c instanceof Cancellable && ((Cancellable)c).isCancellable()) {
            ((Cancellable)c).cancel();
        }
    }

    /**
     * Receives execution observer events from child commands and fires step reached events to
     * this composites observers
     */
    private class ExecutionObserverProxy extends ExecutionObserverAdapter<C> {
        private final E commandExecution;
        private volatile boolean errorOccurred;
        private volatile C currentChildExecution;
        private volatile boolean lastCommandCancelled;
        private volatile Throwable lastCommandError;

        public ExecutionObserverProxy(E commandExecution) {
            this.commandExecution = commandExecution;
        }

        public void started(C commandExecution) {
            this.currentChildExecution = commandExecution;
            compositeCommand.fireProgress(this.commandExecution, currentChildExecution.toString());
        }

        public void done(C commandExecution) {
            lastCommandCancelled = commandExecution instanceof Cancellable &&
                    ((Cancellable) commandExecution).isCancelled();
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


    protected static class CompositeCommandException extends Exception {

        private CompositeCommandException(Throwable cause) {
            super("Error while executing composite command", cause);
        }
    }
}
