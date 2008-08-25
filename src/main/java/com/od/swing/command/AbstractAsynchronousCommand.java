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
 * A default abstract class implementating AsynchronousCommand
 * Subclasses implement the AsynchronousCommand lifecycle methods
 *
 * This implementation supports CommandLifecycleMonitor instances
 * Events fired to LifeCycleMonitor instances are always fired on the swing event thread.
 * So a LifeCycleMonitor instance can be safely be used to update the UI to represent the progress of the command
 *
 * This implementation also supports an optional CommandController
 * The CommandController allows the commands to integrate with a command controlling subsystem
 * in the client application. This might, for example, allow a ui application to display a list of ongoing tasks,
 * or place an arbitrary limit on the number of concurrent commands.
 * </PRE>
 */
public abstract class AbstractAsynchronousCommand<E extends CommandExecution> implements Command<E> {

    private final CommandController<? super E> commandController;
    private final LifeCycleMonitoringSupport<E> lifeCycleMonitoringSupport = new LifeCycleMonitoringSupport<E>();
    private final String commandName;
    private volatile boolean isRunSynchronously;
    private final Executor executor;


    //use of Hashtable rather than HashMap to ensure synchronized access, default access to facilitate testing
    Map<E, CommandExecutor<E>> executionToExecutorMap = new Hashtable<E, CommandExecutor<E>>();

    /**
     * @param name, a descriptive name for this command
     */
    public AbstractAsynchronousCommand(String name) {
        this(name, Executors.newSingleThreadExecutor(), new DefaultCommandController<E>(), false);
    }

    /**
     * @param name, a descriptive name for this command
     * @param commandController, a CommandController instance which will provide task IDs and control starting and stopping for this command
     */
    public AbstractAsynchronousCommand(String name, CommandController<? super E> commandController) {
        this(name, Executors.newSingleThreadExecutor(), commandController, false);
    }

    /**
     * @param name, a descriptive name for this command
     * @param executor Executor to run this command
     */
    public AbstractAsynchronousCommand(String name, Executor executor) {
        this(name, executor, new DefaultCommandController<E>(), false);
    }

    /**
     * @param name, a descriptive name for this command
     * @param executor Executor to run this command
     * @param commandController, a CommandController instance which will provide task IDs and control starting and stopping for this command
     */
    public AbstractAsynchronousCommand(String name, Executor executor, CommandController<? super E> commandController) {
        this(name, executor, commandController, false);
    }

    /**
     * @param name, a descriptive name for this command
     * @param isRunSynchronously, whether the command should run ansynchronously (the default), or synchronously. The synchronous execution option can be useful for testing
     * @param executor Executor to run this command
     * @param commandController, a CommandController instance which will provide task IDs and control starting and stopping for this command
     */
    public AbstractAsynchronousCommand(String name, Executor executor, CommandController<? super E> commandController, boolean isRunSynchronously) {
        this.commandName = name;
        this.executor = executor;
        this.isRunSynchronously = isRunSynchronously;
        this.commandController = commandController;
    }

    public final E execute(LifeCycleMonitor<? super E>... instanceLifeCycleMonitors) {
        E execution = createExecutionInEventThread();
        if ( execution != null ) {
            executeCommand(execution, instanceLifeCycleMonitors);
        } else {
            System.err.println("Cannot run command " + commandName + ". createExecution returned null");
        }
        return execution;
     }

    private void executeCommand(final E execution, LifeCycleMonitor<? super E>... instanceLifeCycleMonitors) {

        //get a snapshot list of the life cycle monitors which will receive the events for this execution
        final List<LifeCycleMonitor<? super E>> monitorsForExecution = lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot();
        monitorsForExecution.addAll(Arrays.asList(instanceLifeCycleMonitors));

        //create a new executor unique to this command execution
        new DefaultCommandExecutor<E>(
            executor,
            executionToExecutorMap,
            execution,
            commandController,
            monitorsForExecution,
            commandName,
            isRunSynchronously
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
        Throwable t = LifeCycleMonitoringSupport.executeSynchronouslyOnEventThread(r, false);
        if ( t != null ) {
            System.err.println("Error while creating execution for command " + commandName);
            t.printStackTrace();
        }
        return (E)r.getExecution();
    }

    /**
     * @return an Execution for this asyncronous command
     */
    public abstract E createExecution();
    
    public void addLifeCycleMonitor(LifeCycleMonitor<? super E>... lifeCycleMonitor) {
        lifeCycleMonitoringSupport.addLifeCycleMonitor(lifeCycleMonitor);
    }

    public void removeLifeCycleMonitor(LifeCycleMonitor<? super E>... lifeCycleMonitor) {
        lifeCycleMonitoringSupport.removeLifeCycleMonitor(lifeCycleMonitor);
    }

    /**
     * Fire command error to LifeCycleMonitor instances
     * Event will be fired on the Swing event thread
     *
     * This has default visiblity so that CompositeAsyncCommand can use it
     * Subclasses should usually throw an exception during processing - which will trigger an error to be fired and processing to be aborted
     *
     * @param commandName, name describing this command
     * @param commandExecution execution for executing command
     * @param t the error which occurred
     */
    void fireError(String commandName, E commandExecution, Throwable t) {
        CommandExecutor<E> c = executionToExecutorMap.get(commandExecution);
        if ( c != null ) {
            List<LifeCycleMonitor<? super E>> lifeCycleMonitors = c.getLifeCycleMonitors();
            LifeCycleMonitoringSupport.fireError(lifeCycleMonitors, commandName, commandExecution, t);
        } else {
            System.err.println(getClass().getName() + " tried to fire error for unknown execution");
        }
    }

    /**
     * Fire step reached to LifeCycleMonitor instances
     * Event will be fired on the Swing event thread
     *
     * @param commandName, name describing this command
     * @param commandExecution, execution for executing command
     */
    protected void fireStepReached(String commandName, E commandExecution) {
        CommandExecutor<E> c = executionToExecutorMap.get(commandExecution);
        if ( c != null ) {
            List<LifeCycleMonitor<? super E>> lifeCycleMonitors = c.getLifeCycleMonitors();
            LifeCycleMonitoringSupport.fireStepReached(lifeCycleMonitors, commandName, commandExecution);
        } else {
            System.err.println(getClass().getName() + " tried to fire step reached for unknown execution");
        }
    }

    /**
     * @return taskController for this asynchronous command
     */
    public CommandController getCommandController() {
        return commandController;
    }

    /**
     * Prevent the asynchronous command from running in a new subthread
     * This is useful for some unit testing
     * 
     * @param runSynchronously
     */
    public void setRunSynchronously(boolean runSynchronously) {
        isRunSynchronously = runSynchronously;
    }

    public String toString() {
        return commandName;
    }
}
