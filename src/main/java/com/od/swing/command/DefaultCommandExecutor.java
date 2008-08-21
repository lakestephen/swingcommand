package com.od.swing.command;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * This runnable contains the logic which executes the command in stages
 * Usually it is run from a new thread/sub thread which has been spawned specifically to manage
 * the lifecycle of this async command.
 *
 * It may be run synchronously from the event thread if synchronous mode is enabled for testing
 */
class DefaultCommandExecutor<E extends CommandExecution> implements CommandExecutor<E> {

    private final Map<E, CommandExecutor<E>> executionToExecutorMap;
    private final E commandExecution;
    private final List<LifeCycleMonitor<? super E>> lifeCycleMonitors;
    private final CommandController<? super E> commandController;
    private final String commandName;
    private final boolean runSynchronously;

    public DefaultCommandExecutor(Map<E, CommandExecutor<E>> executionToExecutorMap, E commandExecution, CommandController<? super E> commandController, List<LifeCycleMonitor<? super E>> lifeCycleMonitors, String commandName, boolean isRunSynchronously) {
        this.executionToExecutorMap = executionToExecutorMap;
        this.commandExecution = commandExecution;
        this.lifeCycleMonitors = lifeCycleMonitors;
        this.commandController = commandController;
        this.commandName = commandName;
        runSynchronously = isRunSynchronously;
    }

    /**
     * This object is used to synchronize memory for each stage of the command processing,
     * This ensures that any state updated during each stage is flushed to shared heap memory before the next stage executes
     * (Since the next stage will executed in a different thread such state changes would not otherwise be guaranteed to be visible)
     */
    private final Object memorySync = new Object();

    public List<LifeCycleMonitor<? super E>> getLifeCycleMonitors() {
        return lifeCycleMonitors;
    }

    public Thread executeCommand() {

        //register the executor against the execution in the map
        executionToExecutorMap.put(commandExecution, DefaultCommandExecutor.this);

        //Call fire started before spawning a new thread. Provided execute was called on the
        //event thread, no more ui work can possibly get done before fireStarted is called
        //If fireStarted is used, for example, to disable a button, this guarantees that the button will be
        //disabled before the action listener triggering the command returns.
        //otherwise the user might be able to click the button again before fireStarted
        LifeCycleMonitoringSupport.fireStarted(lifeCycleMonitors, commandName, commandExecution);

        //a runnable to do the async portion of the command
        Runnable execute = new Runnable() {
            public void run() {
                try {
                    doExecuteAsync();
                } finally {
                    executionToExecutorMap.remove(commandExecution);
                }
            }
        };

        Thread thread;
        //kick off the executor on a subthread, unless we are in run synchronous mode or are on a subthread already
        if ( SwingUtilities.isEventDispatchThread() && ! runSynchronously ) {
            thread = new Thread(execute);
            thread.start();
        } else {
            thread = Thread.currentThread();
            execute.run();
        }
        return thread;
    }

    private void doExecuteAsync() {
        //this try block makes sure we always call end up calling fireEnded
        //this try block makes sure we always end up calling commandStopped, one commandStarting returns true
        try {
            synchronized (memorySync) {
                commandController.commandStarting(commandName, commandExecution);

                //STAGE2  - in the current command processing thread which will not be the event thread unless in synchronous mode
                commandExecution.doExecuteAsync();
            }

            //STAGE3 - this needs to be done on the event thread
            runDoAfterExecute(commandExecution);

        } catch (Throwable t ) {
            safeHandleError(commandExecution, t);
            LifeCycleMonitoringSupport.fireError(lifeCycleMonitors, commandName, commandExecution, t);
            t.printStackTrace();
        } finally {
            safeStopCommand(commandExecution);
            LifeCycleMonitoringSupport.fireEnded(lifeCycleMonitors, commandName, commandExecution);
        }
    }

    //run command stopped safely
    private void safeStopCommand(E commandExecution)
    {
        try {
            commandController.commandStopped(commandName, commandExecution);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    //run handle error safely
    private void safeHandleError(E commandExecution, Throwable t)
    {
        try {
            commandController.commandError(commandName, commandExecution, t);
        } catch ( Throwable th) {
            th.printStackTrace();
        }
    }


    private void runDoAfterExecute(final E commandExecution) throws AsyncCommandException {
        class DoAfterExecuteRunnable implements Runnable {
            volatile Throwable t;
            protected Throwable getError() {
                return t;
            }

            public void run() {
                synchronized (memorySync) {  //make sure the event thread sees the latest state
                    try {
                        commandExecution.doAfterExecute();
                    }
                    catch (Throwable e) {
                        t = e;
                    }
                }
            }
        }
        DoAfterExecuteRunnable doAfterExecuteRunnable = new DoAfterExecuteRunnable();
        if ( ! runSynchronously ) {
            LifeCycleMonitoringSupport.executeSynchronouslyOnEventThread(doAfterExecuteRunnable, true);           
        } else {
            doAfterExecuteRunnable.run();
        }
        checkAndRethrowError(doAfterExecuteRunnable.getError());
    }

    /**
     * Check to see whether an asynchronous task raised an error
     * If so, re-raise the error on the current thread
     *
     * @param t Throwable to check
     * @throws AsyncCommandException
     */
    private void checkAndRethrowError(Throwable t) throws AsyncCommandException {
        if (t != null) {
            throw new AsyncCommandException("Failed while running asynchronous command class " + getClass().getName(), t);
        }
    }

}
