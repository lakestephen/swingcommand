package swingcommand2;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

    private ExecutionObserverSupport executionObserverSupport = new ExecutionObserverSupport();
    private Executor executor;

    public SwingCommand() {
        this(Executors.newSingleThreadExecutor());
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
        executeCommand(executor, execution, instanceExecutionObservers); //this will be asynchronous unless we have a synchronous executor or are already in a worker thread
        return execution;
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
        if ( t != null ) {
            throw new SwingCommandRuntimeException("Cannot run swingcommand \" + getClass().getName() + \" createExecution() threw an exception");
        } else if ( execution == null ) {
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
        ExecutionManager result = null; //TODO - support synchronous executions
        if ( execution instanceof AsyncExecution) {
            result = new AsyncExecutionManager(
                executor,
                (AsyncExecution)execution,
                observersForExecution
            );
        }
        return result;
    }

}
