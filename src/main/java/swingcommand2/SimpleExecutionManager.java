package swingcommand2;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt (refactored from EBondMarketService)
 * Date: 23-Apr-2009
 * Time: 10:40:31
 */
public class SimpleExecutionManager implements ExecutionManager {

    private Executor executor;
    private Execution execution;
    private ExecutionObserver[] executionObservers;

    public SimpleExecutionManager(Executor executor, Execution execution, List<ExecutionObserver> executionObservers) {
        this.executor = executor;
        this.execution = execution;
        this.executionObservers = executionObservers.toArray(new ExecutionObserver[executionObservers.size()]);
    }

    public void executeCommand() {
       Runnable executeRunnable = createExecutionRunnable(execution, executionObservers);
       executor.execute(executeRunnable);
    }

    private Runnable createExecutionRunnable(final Execution execution, final ExecutionObserver... executionObservers) {
        return new Runnable() {
            public void run() {
                execution.addExecutionObserver(executionObservers);
                try {
                    execution.setState(ExecutionState.PENDING);
                    ExecutionObserverSupport.firePending(execution.getExecutionObservers(), execution);
                    execution.setState(ExecutionState.STARTED);
                    ExecutionObserverSupport.fireStarted(execution.getExecutionObservers(), execution);
                    execution.doInEventThread();
                    execution.setState(ExecutionState.SUCCESS);
                    ExecutionObserverSupport.fireSuccess(execution.getExecutionObservers(), execution);
                } catch ( Throwable t) {
                    execution.setExecutionException(t);
                    execution.setState(ExecutionState.ERROR);
                    ExecutionObserverSupport.fireError(execution.getExecutionObservers(), execution, t);
                    ExecutionObserverSupport.fireDone(execution.getExecutionObservers(), execution);
                } finally {
                    execution.removeExecutionObserver(executionObservers);
                }
            }
        };
    }

}
