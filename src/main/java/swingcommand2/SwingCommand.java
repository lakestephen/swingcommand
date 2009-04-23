package swingcommand2;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Apr-2009
 * Time: 19:50:53
 * To change this template use File | Settings | File Templates.
 */
public abstract class SwingCommand {

    private static ExecutorService DEFAULT_ASYNC_EXECUTOR = Executors.newCachedThreadPool();
    private static Executor DEFAULT_SIMPLE_EXECUTOR = new IfSubThreadInvokeLaterExecutor();
    private static ExecutorFactory DEFAULT_EXECUTOR_FACTORY = new DefaultExecutorFactory();

    private ExecutionObserverSupport executionObserverSupport = new ExecutionObserverSupport();
    private volatile Executor executor;

    public SwingCommand() {
    }

    /**
     * @param executor Executor to run this command
     */
    public SwingCommand(Executor executor) {
        this.executor = executor;
    }

    /**
     * @param executor, the Executor used to run this command
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Execution execute(ExecutionObserver... instanceExecutionObservers) {
        return execute(executor, instanceExecutionObservers);
    }

    public Execution execute(Executor executor, ExecutionObserver... instanceExecutionObservers) {
        Execution execution = performCreateExecution();
        if (executor == null) {
            executor = createExecutor(execution);
        }
        executeCommand(executor, execution, instanceExecutionObservers);
        return execution;
    }

    public Execution execute(ExecutorFactory executorFactory, ExecutionObserver... instanceExecutionObservers) {
        Execution execution = performCreateExecution();
        Executor executor = executorFactory.getExecutor(execution);
        executeCommand(executor, execution, instanceExecutionObservers);
        return execution;
    }

    protected Executor createExecutor(Execution execution) {
        return DEFAULT_EXECUTOR_FACTORY.getExecutor(execution);
    }

    public void addExecutionObserver(ExecutionObserver... executionObservers) {
        executionObserverSupport.addExecutionObservers(executionObservers);
    }

    public void removeExecutionObserver(ExecutionObserver... executionObservers) {
        executionObserverSupport.removeExecutionObservers(executionObservers);
    }

    /**
     * Create an execution.
     * It is important this is done on the event thread because, while creating
     * the execution, state from the ui models or components likely has to be copied/cloned to use as
     * parameters for the async processing. For safety only the event thread should interact with ui
     * components/models. Cloning state from the ui models ensures the background thread has its own
     * copy during execution, and there are no potential race conditions
     */
    private Execution performCreateExecution() {

        class CreateExecutionRunnable implements Runnable {

            volatile Execution execution;

            public Execution getExecution() {
                return execution;
            }

            public void run() {
                execution = createExecution();
            }
        }

        CreateExecutionRunnable r = new CreateExecutionRunnable();
        Throwable t = ExecutionObserverSupport.executeSynchronouslyOnEventThread(r, false);
        Execution execution = r.getExecution();  //for some reason some jdk need the cast to E to compile
        if (t != null) {
            throw new SwingCommandRuntimeException("Cannot run swingcommand \" + getClass().getName() + \" createExecution() threw an exception");
        } else if (execution == null) {
            throw new SwingCommandRuntimeException("Cannot run swingcommand " + getClass().getName() + " createExecution() returned null");
        }
        return execution;
    }

    /**
     * @return an Execution for this asynchronous command
     */
    protected abstract Execution createExecution();


    private void executeCommand(Executor executor, Execution execution, ExecutionObserver... instanceExecutionObservers) {

        //get a snapshot list of the execution observers which will receive the events for this execution
        final List<ExecutionObserver> observersForExecution = executionObserverSupport.getExecutionObserverSnapshot();
        observersForExecution.addAll(Arrays.asList(instanceExecutionObservers));

        //create a new execution controller for this execution
        ExecutionManager executionManager = createExecutionManager(executor, execution, observersForExecution);
        executionManager.executeCommand();
    }

    //subclasses may override this to provide a custom ExecutionManager
    protected ExecutionManager createExecutionManager(Executor executor, Execution execution, List<ExecutionObserver> observersForExecution) {
        ExecutionManager result;
        if (execution instanceof AsyncExecution) {
            result = new AsyncExecutionManager(
                    executor,
                    (AsyncExecution) execution,
                    observersForExecution
            );
        } else {
            result = new SimpleExecutionManager(executor, execution, observersForExecution);
        }
        return result;
    }

    static class DefaultExecutorFactory implements ExecutorFactory {
        public Executor getExecutor(Execution e) {
            return (e instanceof AsyncExecution) ? DEFAULT_ASYNC_EXECUTOR : DEFAULT_SIMPLE_EXECUTOR;
        }
    }

    static class IfSubThreadInvokeLaterExecutor implements Executor {

        public void execute(Runnable command) {
            if (SwingUtilities.isEventDispatchThread()) {
                command.run();
            } else {
                //if command kicked off on a subthread we don't want to block it on the event thread
                //longer than necessary for performance reasons, so use invoke later rather than invokeAndWait
                SwingUtilities.invokeLater(command);
            }
        }
    }

    public static interface ExecutorFactory {
        Executor getExecutor(Execution e);
    }
}
