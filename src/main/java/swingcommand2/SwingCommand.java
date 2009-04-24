package swingcommand2;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

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

    private final List<TaskListener> taskListeners = new ArrayList<TaskListener>();

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

    public SwingTask execute(TaskListener... taskListeners) {
        return execute(executor, taskListeners);
    }

    public SwingTask execute(Executor executor, TaskListener... taskListeners) {
        SwingTask execution = doCreateTask();
        if (executor == null) {
            executor = createExecutor(execution);
        }
        executeCommand(executor, execution, taskListeners);
        return execution;
    }

    public SwingTask execute(ExecutorFactory executorFactory, TaskListener... taskListeners) {
        SwingTask task = doCreateTask();
        Executor executor = executorFactory.getExecutor(task);
        executeCommand(executor, task, taskListeners);
        return task;
    }

    private SwingTask doCreateTask() {
        try {
            return createTask();
        } catch ( Throwable t) {
            throw new SwingCommandRuntimeException("Failed to run SwingCommand, createTask() threw an exeception", t);
        }
    }

    protected Executor createExecutor(SwingTask task) {
        return DEFAULT_EXECUTOR_FACTORY.getExecutor(task);
    }

    public final void addExecutionObserver(TaskListener... taskListeners) {
        synchronized (this.taskListeners) {
            this.taskListeners.addAll(Arrays.asList(taskListeners));
        }
    }

    public final void removeExecutionObserver(TaskListener... taskListeners) {
        synchronized (this.taskListeners) {
            this.taskListeners.removeAll(Arrays.asList(taskListeners));
        }
    }

    private List<TaskListener> getListenerSnapshot()  {
        synchronized (taskListeners) {
            return new ArrayList<TaskListener>(taskListeners);
        }
    }

    /**
     * @return an Execution for this asynchronous command
     */
    protected abstract SwingTask createTask();


    private void executeCommand(Executor executor, SwingTask execution, TaskListener... taskListeners) {

        //get a snapshot list of the execution observers which will receive the events for this execution
        final List<TaskListener> allListeners = getListenerSnapshot();
        allListeners.addAll(Arrays.asList(taskListeners));

        //create a new execution controller for this execution
        ExecutionManager executionManager = new ExecutionManager(executor, execution, allListeners);
        executionManager.executeCommand();
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

    static class DefaultExecutorFactory implements ExecutorFactory {
        public Executor getExecutor(SwingTask e) {
            return (e instanceof BackgroundTask) ? DEFAULT_ASYNC_EXECUTOR : DEFAULT_SIMPLE_EXECUTOR;
        }
    }

    public static interface ExecutorFactory {
        Executor getExecutor(SwingTask e);
    }


    static class ExecutionManager {

        private final Executor executor;
        private final SwingTask task;
        private final TaskListener[] taskListeners;

        public ExecutionManager(Executor executor, SwingTask task, List<TaskListener> taskListeners) {
            this.executor = executor;
            this.task = task;
            this.taskListeners = taskListeners.toArray(new TaskListener[taskListeners.size()]);
        }

        /**
         * This object is used to synchronize memory for each stage of the command processing,
         * This ensures that any state updated during each stage is flushed to shared heap memory before the next stage executes
         * (Since the next stage will executed in a different thread such state changes would not otherwise be guaranteed to be visible)
         */
        private final Object memorySync = new Object();

        public void executeCommand() {

            task.addTaskListener(taskListeners);

            //Call fire pending before spawning a new thread. Provided execute was called on the
            //event thread, no more ui work can possibly get done before fireStarting is called
            //If fireStarting is used, for example, to disable a button, this guarantees that the button will be
            //disabled before the action listener triggering the swingcommand returns.
            //otherwise the user might be able to click the button again before the fireStarting callback
            task.setState(ExecutionState.PENDING);
            TaskListenerSupport.firePending(task.getTaskListeners(), task);

             executor.execute(new Runnable() {
                public void run() {
                    doExecuteTask();
                }
            });
        }
    
        private void doExecuteTask() {
            //this try block makes sure we always call end up calling fireDone
            try {
                setExecutionState(ExecutionState.STARTED);
                TaskListenerSupport.fireStarted(task.getTaskListeners(), task);

                if ( task instanceof BackgroundTask) {
                    synchronized (memorySync) {
                        //STAGE1  - in the current swingcommand processing thread
                        ((BackgroundTask) task).doInBackground();
                    }
                }

                //STAGE2 - this needs to be done on the event thread
                runDoInEventThread(task);

                setExecutionState(ExecutionState.SUCCESS);
                TaskListenerSupport.fireSuccess(task.getTaskListeners(), task);
            } catch (Throwable t ) {
                setTaskException(t);
                setExecutionState(ExecutionState.ERROR);
                TaskListenerSupport.fireError(task.getTaskListeners(), task, t);
            } finally {
                TaskListenerSupport.fireDone(task.getTaskListeners(), task);
                task.removeTaskListener(taskListeners);
            }
        }

        private void setTaskException(Throwable t) {
            if ( t instanceof SwingCommandException) {
                task.setExecutionException(t.getCause());
            } else {
                task.setExecutionException(t);
            }
        }

        private void setExecutionState(final ExecutionState newState) {
            TaskListenerSupport.executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    task.setState(newState);
                }
            });
        }

        private void runDoInEventThread(final SwingTask task) throws Exception {
            class DoAfterExecuteRunnable implements Runnable {
                volatile Throwable throwable;

                public void run() {
                    synchronized (memorySync) {  //make sure the event thread sees the latest state
                        try {
                            task.doInEventThread();
                        } catch (Throwable e) {
                            throwable = e;
                        }
                    }
                }
            }
            DoAfterExecuteRunnable doAfterExecuteRunnable = new DoAfterExecuteRunnable();
            TaskListenerSupport.executeSynchronouslyOnEventThread(doAfterExecuteRunnable);
            if (doAfterExecuteRunnable.throwable != null) {
                throw new SwingCommandException("Failed while invoking runDone() on " + getClass().getName(), doAfterExecuteRunnable.throwable);
            }
        }

    }

    static class SwingCommandRuntimeException extends RuntimeException {

        public SwingCommandRuntimeException(String message) {
            super(message);
        }

        public SwingCommandRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class SwingCommandException extends Exception {

        public SwingCommandException(String message) {
            super(message);
        }

        public SwingCommandException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
