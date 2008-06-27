package com.od.swing.command;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Hashtable;

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

    private volatile CommandController<? super E> commandController = new DefaultCommandController<E>();
    private final LifeCycleMonitoringSupport<E> lifeCycleMonitoringSupport = new LifeCycleMonitoringSupport<E>();
    private final boolean isRunSynchronously;
    private final String commandName;
    private String startMessage, stopMessage, errorMessage;

    //use of Hashtable rather than HashMap to ensure synchronized access
    private Map<E, CommandExecutor<E>> executionToExecutorMap = new Hashtable<E, CommandExecutor<E>>();

    /**
     * @param name, a descriptive name for this command
     */
    public AbstractAsynchronousCommand(String name) {
        this.commandName = name;
        this.isRunSynchronously = false;
        setDefaultMessagesFromCommandName();
    }

    /**
     * @param name, a descriptive name for this command
     * @param commandController, a CommandController instance which will provide task IDs and control starting and stopping for this command
     */
    public AbstractAsynchronousCommand(String name, CommandController<? super E> commandController) {
        this.commandName = name;
        this.commandController = commandController;
        this.isRunSynchronously = false;
        setDefaultMessagesFromCommandName();
    }

    /**
     * @param name, a descriptive name for this command
     * @param isRunSynchronously, whether the command should run ansynchronously (the default), or synchronously. The synchronous execution option can be useful for testing
     */
    public AbstractAsynchronousCommand(String name, boolean isRunSynchronously) {
        this.commandName = name;
        this.isRunSynchronously = isRunSynchronously;
        setDefaultMessagesFromCommandName();
    }

    /**
     * @param name, a descriptive name for this command
     * @param isRunSynchronously, whether the command should run ansynchronously (the default), or synchronously. The synchronous execution option can be useful for testing
     * @param commandController, a CommandController instance which will provide task IDs and control starting and stopping for this command
     */
    public AbstractAsynchronousCommand(String name, boolean isRunSynchronously, CommandController<? super E> commandController) {
        this.commandName = name;
        this.isRunSynchronously = isRunSynchronously;
        this.commandController = commandController;
        setDefaultMessagesFromCommandName();
    }

    private void setDefaultMessagesFromCommandName() {
        this.startMessage = "Started command " + commandName;
        this.stopMessage = "Stopped command " + commandName;
        this.errorMessage = "Error executing command " + commandName;
    }

    public void setMessages(String startMessage, String stopMessage, String errorMessage) {
        this.startMessage = startMessage;
        this.stopMessage = stopMessage;
        this.errorMessage = errorMessage;
    }

    public void setStopMessage(String stopMessage) {
        this.stopMessage = stopMessage;
    }

    public void setStartMessage(String startMessage) {
        this.startMessage = startMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public final void execute(CommandLifeCycleMonitor<? super E>... instanceLifeCycleMonitors) {
        E execution = createExecutionInEventThread();
        if ( execution != null ) {
            executeCommand(execution, instanceLifeCycleMonitors);
        } else {
            System.err.println("Cannot run command " + commandName + ". createExecution returned null");
        }
     }

    private void executeCommand(final E execution, CommandLifeCycleMonitor<? super E>... instanceLifeCycleMonitors) {

        //get a snapshot list of the life cycle monitors which will receive the events for this execution
        final List<CommandLifeCycleMonitor<? super E>> monitorsForExecution = lifeCycleMonitoringSupport.getLifeCycleMonitorSnapshot();
        monitorsForExecution.addAll(Arrays.asList(instanceLifeCycleMonitors));

        //create a new executor unique to this command execution
        new DefaultCommandExecutor<E>(
            executionToExecutorMap,
            execution,
            commandController,
            monitorsForExecution,
            startMessage,
            stopMessage,
            errorMessage,
            isRunSynchronously
        ).executeCommand();
    }

    //create an execution on the event thread
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
        return r.getExecution();
    }

    /**
     * @return an Execution for this asyncronous command
     */
    public abstract E createExecution();
    
    public void addLifeCycleMonitor(CommandLifeCycleMonitor<E> lifeCycleMonitor) {
        lifeCycleMonitoringSupport.addLifeCycleMonitor(lifeCycleMonitor);
    }

    public void removeLifeCycleMonitor(CommandLifeCycleMonitor<E> lifeCycleMonitor) {
        lifeCycleMonitoringSupport.removeLifeCycleMonitor(lifeCycleMonitor);
    }

    /**
     * Fire command error to LifeCycleMonitor instances
     * Event will be fired on the Swing event thread
     *
     * @param commandExecution execution for executing command
     * @param errorMessage, a message describing the error
     * @param t the error which occurred
     */
    protected void fireError(E commandExecution, String errorMessage, Throwable t) {
        CommandExecutor<E> c = executionToExecutorMap.get(commandExecution);
        if ( c != null ) {
            List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors = c.getLifeCycleMonitors();
            LifeCycleMonitoringSupport.fireError(lifeCycleMonitors, commandExecution, errorMessage, t);
        } else {
            System.err.println(getClass().getName() + " tried to fire error for unknown execution");
        }
    }

    /**
     * Fire step reached to LifeCycleMonitor instances
     * Event will be fired on the Swing event thread
     *
     * @param commandExecution, execution for executing command
     * @param currentStep, current execution step about to start
     * @param totalStep, total steps in execution
     * @param stepMessage, message describing this step
     */
    protected void fireStepReached(E commandExecution, int currentStep, int totalStep, String stepMessage) {
        CommandExecutor<E> c = executionToExecutorMap.get(commandExecution);
        if ( c != null ) {
            List<CommandLifeCycleMonitor<? super E>> lifeCycleMonitors = c.getLifeCycleMonitors();
            LifeCycleMonitoringSupport.fireStepReached(lifeCycleMonitors, commandExecution, currentStep, totalStep, stepMessage);
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

}
