package swingcommand;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * This runnable contains the logic which executes the command in stages
 * Usually it is run from a new thread/sub thread which has been spawned specifically to manage
 * the lifecycle of this async command.
 */
class DefaultCommandExecutor<E extends AsynchronousExecution> implements CommandExecutor<E> {

    private final Executor executor;
    private final Map<E, CommandExecutor<E>> executionToExecutorMap;
    private final E commandExecution;
    private final List<ExecutionObserver<? super E>> executionObservers;

    public DefaultCommandExecutor(Executor executor, Map<E, CommandExecutor<E>> executionToExecutorMap, E commandExecution, List<ExecutionObserver<? super E>> executionObservers) {
        this.executor = executor;
        this.executionToExecutorMap = executionToExecutorMap;
        this.commandExecution = commandExecution;
        this.executionObservers = executionObservers;
    }

    /**
     * This object is used to synchronize memory for each stage of the command processing,
     * This ensures that any state updated during each stage is flushed to shared heap memory before the next stage executes
     * (Since the next stage will executed in a different thread such state changes would not otherwise be guaranteed to be visible)
     */
    private final Object memorySync = new Object();

    public List<ExecutionObserver<? super E>> getExecutionObservers() {
        return executionObservers;
    }

    public void executeCommand() {

        //register the executor against the execution in the map
        executionToExecutorMap.put(commandExecution, DefaultCommandExecutor.this);

        //Call fire starting before spawning a new thread. Provided execute was called on the
        //event thread, no more ui work can possibly get done before fireStarting is called
        //If fireStarting is used, for example, to disable a button, this guarantees that the button will be
        //disabled before the action listener triggering the swingcommand returns.
        //otherwise the user might be able to click the button again before the fireStarting callback
        ExecutionObserverSupport.fireStarting(executionObservers, commandExecution);

        //a runnable to do the async portion of the swingcommand
        Runnable executionRunnable = new Runnable() {
            public void run() {
                try {
                    doExecuteAsync();
                } finally {
                    executionToExecutorMap.remove(commandExecution);
                }
            }
        };
         executor.execute(executionRunnable);
    }

    private void doExecuteAsync() {
        //this try block makes sure we always call end up calling fireEnded
        try {
            ExecutionObserverSupport.fireStarted(executionObservers, commandExecution);

            synchronized (memorySync) {
                //STAGE1  - in the current swingcommand processing thread
                commandExecution.doInBackground();
            }

            //STAGE2 - this needs to be done on the event thread
            runDone(commandExecution);

            ExecutionObserverSupport.fireSuccess(executionObservers, commandExecution);
        } catch (Throwable t ) {
            ExecutionObserverSupport.fireError(executionObservers, commandExecution, t);
            t.printStackTrace();
        } finally {
            ExecutionObserverSupport.fireEnded(executionObservers, commandExecution);
        }
    }


    private void runDone(final E commandExecution) throws Exception {
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