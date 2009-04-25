package swingcommand;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt (refactored from EBondMarketService)
 * Date: 23-Apr-2009
 * Time: 10:55:25
 */
public class CompositeCommandTask extends BackgroundTask<String> {

    private static final Executor SYNCHRONOUS_EXECUTOR = new Executor() {
        public void execute(Runnable command) {
            command.run();
        }
    };

    private static final Executor INVOKE_AND_WAIT_EXECUTOR = new IfSubThreadInvokeAndWaitExecutor();
    private static final SwingCommand.ExecutorFactory COMPOSITE_EXECUTOR_FACTORY = new CompositeExecutorFactory();

    private List<SwingCommand> executionCommands = new ArrayList<SwingCommand>();
    private int totalChildCommands = executionCommands.size();
    private int currentCommandId;
    private ExecutionObserverProxy executionObserverProxy = new ExecutionObserverProxy();

    public CompositeCommandTask() {
    }

    public CompositeCommandTask(SwingCommand... commands) {
        executionCommands.addAll(Arrays.asList(commands));
    }

    public CompositeCommandTask(Collection<SwingCommand> commands) {
        executionCommands.addAll(commands);
    }

    /**
     * Execute the child commands here off the swing thread.
     * This will not start a new subthread - the child commands will run synchronously in the parent command's execution thread.
     *
     * @throws Exception
     */
    public void doInBackground() throws Exception {
        currentCommandId = 0;
        for (final SwingCommand command : executionCommands) {
            currentCommandId++;

            //we are not in event thread here, so this call should be synchronous
            //noinspection unchecked
            command.execute(COMPOSITE_EXECUTOR_FACTORY, executionObserverProxy);

            if (executionObserverProxy.isErrorOccurred()) {
                throw new CompositeCommandException(executionObserverProxy.getLastCommandError());
            }

            if (executionObserverProxy.isLastCommandCancelled()) {
                cancel();
            }

            if (isCancelled()) {
                break;
            }
        }
    }

    public void doInEventThread() throws Exception {
    }

    public void addCommand(SwingCommand command) {
        executionCommands.add(command);
    }

    public void addCommands(SwingCommand... commands) {
        executionCommands.addAll(Arrays.asList(commands));
    }

    public void addCommands(Collection<SwingCommand> commands) {
        executionCommands.addAll(commands);
    }

    public Task getCurrentExecution() {
        return executionObserverProxy.getCurrentChildExecution();
    }

    public int getCompletedCommandCount() {
        return currentCommandId;
    }

    public int getTotalCommands() {
        return totalChildCommands;
    }

    public List<SwingCommand> getChildCommands() {
        return Collections.unmodifiableList(executionCommands);
    }

    public void doCancel() {
        executionObserverProxy.getCurrentChildExecution().cancel();
    }

    /**
     * Receives execution observer events from child commands and fires step reached events to
     * this composites observers
     */
    private class ExecutionObserverProxy extends TaskListenerAdapter {
        private volatile boolean errorOccurred;
        private volatile Task currentChildExecution;
        private volatile boolean lastCommandCancelled;
        private volatile Throwable lastCommandError;

        public ExecutionObserverProxy() {
        }

        public void started(Task commandExecution) {
            this.currentChildExecution = commandExecution;
            fireProgress(currentChildExecution.toString());
        }

        public void finished(Task commandExecution) {
            lastCommandCancelled = commandExecution.isCancelled();
        }

        public void error(Task commandExecution, Throwable e) {
            errorOccurred = true;
            lastCommandError = e;
        }

        public boolean isErrorOccurred() {
            return errorOccurred;
        }

        public Task getCurrentChildExecution() {
            return currentChildExecution;
        }

        public boolean isLastCommandCancelled() {
            return lastCommandCancelled;
        }

        public Throwable getLastCommandError() {
            return lastCommandError;
        }
    }

    private static class CompositeCommandException extends Exception {

        private CompositeCommandException(Throwable cause) {
            super("Error while executing composite command", cause);
        }
    }

    private static class CompositeExecutorFactory implements SwingCommand.ExecutorFactory {

        public Executor getExecutor(Task e) {
            if (e instanceof BackgroundTask) {
                return SYNCHRONOUS_EXECUTOR;
            } else {
                return INVOKE_AND_WAIT_EXECUTOR;
            }
        }
    }

    /**
     * Created by IntelliJ IDEA.
     * User: Nick Ebbutt (refactored from EBondMarketService)
     * Date: 23-Apr-2009
     * Time: 11:43:44
     */
    static class IfSubThreadInvokeAndWaitExecutor implements Executor {
        public void execute(Runnable command) {
            if (SwingUtilities.isEventDispatchThread()) {
                command.run();
            } else {
                //if command kicked off on a subthread we don't want to block it on the event thread
                //longer than necessary for performance reasons, so use invoke later rather than invokeAndWait
                try {
                    SwingUtilities.invokeAndWait(command);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
