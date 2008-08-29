package com.od.swing.command;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Hashtable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * <PRE>
 * <p/>
 * An abstract superclass for asynchronous Commands
 *
 * Supports ExecutionObserver instances. Events fired to ExecutionObserver instances are always fired on the swing event thread.
 * So a ExecutionObserver instance can be safely be used to update the UI to represent the progress of the command
 * </PRE>
 */
public abstract class AbstractAsynchronousCommand<E extends CommandExecution> implements AsynchronousCommand<E> {

    private final ExecutionObserverSupport<E> executionObservingSupport = new ExecutionObserverSupport<E>();
    private final Executor executor;

    //use of Hashtable rather than HashMap to ensure synchronized access, default access to facilitate testing
    Map<E, CommandExecutor<E>> executionToExecutorMap = new Hashtable<E, CommandExecutor<E>>();


    public AbstractAsynchronousCommand() {
        this(Executors.newSingleThreadExecutor());
    }

    /**
     * @param executor Executor to run this command
     */
    public AbstractAsynchronousCommand(Executor executor) {
        this.executor = executor;
    }

    public final E execute(ExecutionObserver<? super E>... instanceExecutionObservers) {
        E execution = createExecutionInEventThread();
        if ( execution != null ) {
            executeCommand(execution, instanceExecutionObservers);
        } else {
            System.err.println("Cannot run command " + this + ". createExecution returned null");
        }
        return execution;
     }

    private void executeCommand(final E execution, ExecutionObserver<? super E>... instanceExecutionObservers) {

        //get a snapshot list of the execution observers which will receive the events for this execution
        final List<ExecutionObserver<? super E>> observersForExecution = executionObservingSupport.getExecutionObserverSnapshot();
        observersForExecution.addAll(Arrays.asList(instanceExecutionObservers));

        //create a new executor unique to this command execution
        new DefaultCommandExecutor<E>(
            executor,
            executionToExecutorMap,
            execution,
            observersForExecution
        ).executeCommand();
    }

    /**
     * Create an execution.
     * It is important this is done on the event thread because, while creating
     * the execution, state from the ui models or components likely has to be copied/cloned to use as
     * parameters for the async processing. For safety only the event thread should interact with ui
     * components/models. Cloning state from the ui models ensures the background thread has its own
     * copy during execution, and there are no potential race conditions
     */
    private E createExecutionInEventThread() {

        class CreateExecutionRunnable implements Runnable {

            volatile E execution;

            public E getExecution() {
                return execution;
            }

            public void run() {
                execution = createExecution();
            }
        }

        CreateExecutionRunnable r = new CreateExecutionRunnable();
        Throwable t = ExecutionObserverSupport.executeSynchronouslyOnEventThread(r, false);
        if ( t != null ) {
            System.err.println("Error while creating execution for command " + this);
            t.printStackTrace();
        }
        return (E)r.getExecution();
    }

    /**
     * @return an Execution for this asyncronous command
     */
    public abstract E createExecution();


    public void addExecutionObserver(ExecutionObserver<? super E> executionObserver) {
        addExecutionObservers(executionObserver);
    }

    public void addExecutionObservers(ExecutionObserver<? super E>... executionObservers) {
        executionObservingSupport.addExecutionObservers(executionObservers);
    }

    public void removeExecutionObserver(ExecutionObserver<? super E> executionObserver) {
        removeExecutionObservers(executionObserver);
    }

    public void removeExecutionObservers(ExecutionObserver<? super E>... executionObservers) {
        executionObservingSupport.removeExecutionObservers(executionObservers);
    }

    /**
     * Fire command error to ExecutionObserver instances
     * Event will be fired on the Swing event thread
     *
     * This has default visiblity so that CompositeAsyncCommand can use it but subclasses should raise an error by throwing it in doInBackground() or done()
     * Subclasses should usually throw an exception during processing - which will trigger an error to be fired and processing to be aborted
     *
     * @param commandExecution execution for executing command
     * @param t the error which occurred
     */
    void fireError(E commandExecution, Throwable t) {
        CommandExecutor<E> c = executionToExecutorMap.get(commandExecution);
        if ( c != null ) {
            List<ExecutionObserver<? super E>> executionObservers = c.getExecutionObservers();
            ExecutionObserverSupport.fireError(executionObservers, commandExecution, t);
        } else {
            System.err.println(getClass().getName() + " tried to fire error for unknown execution");
        }
    }

    /**
     * Fire step reached to ExecutionObserver instances
     * Event will be fired on the Swing event thread
     *
     * @param commandExecution, execution for executing command
     */
    protected void fireStepReached(E commandExecution) {
        CommandExecutor<E> c = executionToExecutorMap.get(commandExecution);
        if ( c != null ) {
            List<ExecutionObserver<? super E>> executionObservers = c.getExecutionObservers();
            ExecutionObserverSupport.fireStepReached(executionObservers, commandExecution);
        } else {
            System.err.println(getClass().getName() + " tried to fire step reached for unknown execution");
        }
    }
}
