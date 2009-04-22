package swingcommand2;

import java.util.concurrent.Executor;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
* User: nick
* Date: 22-Apr-2009
* Time: 20:13:26
* To change this template use File | Settings | File Templates.
*/
class AsyncExecutionManager implements ExecutionManager {

    private final Executor executor;
    private final AsyncExecution commandExecution;
    private final ExecutionObserver[] executionObservers;

    public AsyncExecutionManager(Executor executor, AsyncExecution commandExecution, List<ExecutionObserver> executionObservers) {
        this.executor = executor;
        this.commandExecution = commandExecution;
        this.executionObservers = executionObservers.toArray(new ExecutionObserver[executionObservers.size()]);
    }

    /**
     * This object is used to synchronize memory for each stage of the command processing,
     * This ensures that any state updated during each stage is flushed to shared heap memory before the next stage executes
     * (Since the next stage will executed in a different thread such state changes would not otherwise be guaranteed to be visible)
     */
    private final Object memorySync = new Object();

    public void executeCommand() {

        commandExecution.addExecutionObserver(executionObservers);

        //Call fire pending before spawning a new thread. Provided execute was called on the
        //event thread, no more ui work can possibly get done before fireStarting is called
        //If fireStarting is used, for example, to disable a button, this guarantees that the button will be
        //disabled before the action listener triggering the swingcommand returns.
        //otherwise the user might be able to click the button again before the fireStarting callback
        ExecutionObserverSupport.firePending(commandExecution.getExecutionObservers(), commandExecution);

        //a runnable to do the async portion of the swingcommand
        Runnable executionRunnable = new Runnable() {
            public void run() {
                doExecuteAsync();
            }
        };
         executor.execute(executionRunnable);
    }

    private void doExecuteAsync() {
        //this try block makes sure we always call end up calling fireDone
        try {
            setExecutionState(ExecutionState.STARTED);
            ExecutionObserverSupport.fireStarted(commandExecution.getExecutionObservers(), commandExecution);

            synchronized (memorySync) {
                //STAGE1  - in the current swingcommand processing thread
                commandExecution.doInBackground();
            }

            //STAGE2 - this needs to be done on the event thread
            runDone(commandExecution);

            setExecutionState(ExecutionState.SUCCESS);
            ExecutionObserverSupport.fireSuccess(commandExecution.getExecutionObservers(), commandExecution);
        } catch (Throwable t ) {
            commandExecution.setExecutionException(t);
            setExecutionState(ExecutionState.ERROR);
            ExecutionObserverSupport.fireError(commandExecution.getExecutionObservers(), commandExecution, t);
        } finally {
            ExecutionObserverSupport.fireDone(commandExecution.getExecutionObservers(), commandExecution);
            commandExecution.removeExecutionObserver(executionObservers);
        }
    }

    private void setExecutionState(final ExecutionState newState) {
        ExecutionObserverSupport.executeSynchronouslyOnEventThread(new Runnable(){
            public void run() {
                commandExecution.setState(newState);
            }
        }, true);
    }

    private void runDone(final AsyncExecution commandExecution) throws Exception {
        class DoneRunnable implements Runnable {
            volatile Throwable t;
            protected Throwable getError() {
                return t;
            }

            public void run() {
                synchronized (memorySync) {  //make sure the event thread sees the latest state
                    try {
                        commandExecution.doInEventThread();
                    }
                    catch (Throwable e) {
                        t = e;
                    }
                }
            }
        }
        DoneRunnable doAfterExecuteRunnable = new DoneRunnable();
        ExecutionObserverSupport.executeSynchronouslyOnEventThread(doAfterExecuteRunnable, true);
        Throwable t = doAfterExecuteRunnable.getError();
        if (t != null) {
            throw new Exception("Failed while invoking runDone() on " + getClass().getName(), t);
        }
    }

}
