/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package swingcommand;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Apr-2009
 * Time: 19:50:53
 * 
 */
public abstract class SwingCommand<P,E> {

    private static ExecutorService DEFAULT_BACKGROUND_EXECUTOR = Executors.newCachedThreadPool();
    private static Executor DEFAULT_SIMPLE_EXECUTOR = new IfSubThreadInvokeLaterExecutor();
    private ExecutorFactory DEFAULT_EXECUTOR_FACTORY = new DefaultExecutorFactory();

    private final List<TaskListener<? super E>> taskListeners = new ArrayList<TaskListener<? super E>>();

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

    public Task<P,E> execute() {
        return execute(null, executor, null);
    }

    public Task<P,E> execute(Executor executor) {
        return execute(null, executor, null);
    }

    public Task<P,E> execute(ExecutorFactory executorFactory) {
        return execute(null, executorFactory, null);
    }

    public Task<P,E> execute(TaskListener<? super E> taskListener) {
        return execute(executor, taskListener);
    }

    public Task<P,E> execute(Executor executor, TaskListener<? super E> taskListener) {
        return execute(null, executor, taskListener);
    }

    public Task<P,E> execute(ExecutorFactory executorFactory, TaskListener<? super E> taskListener) {
        return execute(null, executorFactory, taskListener);
    }

    public Task<P,E> execute(P parameters) {
        return execute(parameters, executor, null);
    }

    public Task<P,E> execute(P parameters, Executor executor) {
        return execute(parameters, executor, null);
    }

    public Task<P,E> execute(P parameters, ExecutorFactory executorFactory) {
        return execute(parameters, executorFactory, null);
    }

    public Task<P,E> execute(P parameters, TaskListener<? super E> taskListener) {
        return execute(parameters, executor, taskListener);
    }

    public Task<P,E> execute(P parameters, Executor executor, TaskListener<? super E> taskListener) {
        Task<P, E> task = createTaskAndSetParams(parameters);

        //use the supplied Executor or the default, if null
        if (executor == null) {
            executor = getExecutor(task);
        }

        executeCommand(executor, task, taskListener);
        return task;
    }

    public Task<P,E> execute(P parameters, ExecutorFactory executorFactory, TaskListener<? super E> taskListener) {
        Task<P, E> task = createTaskAndSetParams(parameters);
        
        //use the ExecutorFactory to create an Executor based on the Task
        Executor executor = executorFactory.getExecutor(task);

        executeCommand(executor, task, taskListener);
        return task;
    }

    private Task<P, E> createTaskAndSetParams(P parameters) {
        Task<P,E> task = doCreateTask();
        if ( parameters != null) {
            task.setParameters(parameters);
        }
        return task;
    }

    private List<TaskListener<? super E>> getList(TaskListener<? super E> taskListener) {
        List<TaskListener<? super E>> l = new ArrayList<TaskListener<? super E>>();
        l.add(taskListener);
        return l;
    }

    private Task<P,E> doCreateTask() {
        try {
            return createTask();
        } catch ( Throwable t) {
            throw new SwingCommandRuntimeException("Failed to run SwingCommand, createTask() threw an exeception", t);
        }
    }

    private Executor getExecutor(Task task) {
        return DEFAULT_EXECUTOR_FACTORY.getExecutor(task);
    }

    public final void addTaskListener(TaskListener<? super E> taskListener) {
        synchronized (this.taskListeners) {
            this.taskListeners.add(taskListener);
        }
    }

    public final void removeTaskListener(TaskListener<? super E> taskListener) {
        synchronized (this.taskListeners) {
            this.taskListeners.remove(taskListener);
        }
    }

    private List<TaskListener<? super E>> getListenerSnapshot()  {
        synchronized (taskListeners) {
            return new ArrayList<TaskListener<? super E>>(taskListeners);
        }
    }

    /**
     * @return an Execution for this asynchronous command
     */
    protected abstract Task<P,E> createTask();


    private void executeCommand(Executor executor, Task<P,E> execution, TaskListener<? super E> taskListener) {

        //get a snapshot list of the execution observers which will receive the events for this execution
        final List<TaskListener<? super E>> allListeners = getListenerSnapshot();

        //add invocation listener if supplied
        if ( taskListener != null) {
            allListeners.add(taskListener);
        }

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

    /**
     * Subclasses may override this method to return a different default executor for tasks which run
     * in the event thread. This may, for example, change the behaviour so that the execute method become asynchronous
     * if called from the event thread (with the call to doInEventThread runnable occuring later on the event queue)
     */
    protected Executor getDefaultTaskExecutor() {
        return DEFAULT_SIMPLE_EXECUTOR;
    }

    /**
     * Subclasses may override this method to return a different default executor for background tasks
     */
    protected Executor getDefaultBackgroundTaskExecutor() {
        return DEFAULT_BACKGROUND_EXECUTOR;
    }

    class DefaultExecutorFactory implements ExecutorFactory {
        public Executor getExecutor(Task e) {
            return (e instanceof BackgroundTask) ? getDefaultBackgroundTaskExecutor() : getDefaultTaskExecutor();
        }
    }

    public static interface ExecutorFactory {
        Executor getExecutor(Task e);
    }


    class ExecutionManager {

        private final Executor executor;
        private final Task<P,E> task;
        private final List<TaskListener<? super E>> taskListeners;

        public ExecutionManager(Executor executor, Task<P,E> task, List<TaskListener<? super E>> taskListeners) {
            this.executor = executor;
            this.task = task;
            this.taskListeners = taskListeners;
        }

        /**
         * This object is used to synchronize memory for each stage of the command processing,
         * This ensures that any state updated during each stage is flushed to shared heap memory before the next stage executes
         * (Since the next stage will executed in a different thread such state changes would not otherwise be guaranteed to be visible)
         */
        private final Object memorySync = new Object();

        public void executeCommand() {

            task.addTaskListeners(taskListeners);

            //Call fire pending before spawning a new thread. Provided execute was called on the
            //event thread, no more ui work can possibly get finished before fireStarting is called
            //If fireStarting is used, for example, to disable a button, this guarantees that the button will be
            //disabled before the action listener triggering the swingcommand returns.
            //otherwise the user might be able to click the button again before the fireStarting callback
            task.setExecutionState(Task.ExecutionState.PENDING);
            TaskListenerSupport.firePending(task.getTaskListeners(), task);

             executor.execute(new Runnable() {
                public void run() {
                    doExecuteTask();
                }
            });
        }
    
        private void doExecuteTask() {
            //this try block makes sure we always call end up calling fireFinished
            try {
                Thread.interrupted(); // clear any interrupted state before starting

                setExecutionState(Task.ExecutionState.STARTED);
                TaskListenerSupport.fireStarted(task.getTaskListeners(), task);

                if ( task instanceof BackgroundTask) {
                    synchronized (memorySync) {
                        //STAGE1  - in the current swingcommand processing thread
                        ((BackgroundTask) task).doBackgroundProcessing();
                    }
                }

                //STAGE2 - this needs to be finished on the event thread
                runDoInEventThread(task);

                setExecutionState(Task.ExecutionState.SUCCESS);
                TaskListenerSupport.fireSuccess(task.getTaskListeners(), task);
            } catch (Throwable t ) {
                setTaskException(t);
                setExecutionState(Task.ExecutionState.ERROR);
                TaskListenerSupport.fireError(task.getTaskListeners(), task, t);
            } finally {
                TaskListenerSupport.fireFinished(task.getTaskListeners(), task);
            }
        }

        private void setTaskException(Throwable t) {
            if ( t instanceof SwingCommandException) {
                task.setExecutionException(t.getCause());
            } else {
                task.setExecutionException(t);
            }
        }

        private void setExecutionState(final Task.ExecutionState newState) {
            TaskListenerSupport.executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    task.setExecutionState(newState);
                }
            });
        }

        private void runDoInEventThread(final Task task) throws Exception {
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
                throw new SwingCommandException("Failed while invoking doInEventThread() on " + getClass().getName(), doAfterExecuteRunnable.throwable);
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
